package it.hardoptimization.nativebridge;

import it.hardoptimization.HardOptimizationMod;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Zero-copy Java 25 FFM (Foreign Function & Memory) bridge to the Rust `hardopt_native` cdylib.
 * Automatically falls back to pure Java if the native library is unavailable for the current OS/arch.
 */
public final class NativeBridge {
	private static volatile boolean initialized = false;
	private static volatile boolean available = false;
	private static volatile int abiVersion = 0;
	private static volatile String platformTag = "unknown";
	private static volatile String statusMessage = "Not initialized";

	private static MethodHandle fnVersion;
	private static MethodHandle fnHash64;
	private static MethodHandle fnBatchAabbOverlaps;

	private NativeBridge() {
	}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		platformTag = detectPlatformTag();
		String libFileName = mapLibraryFileName("hardopt_native");
		String resourcePath = "/natives/" + platformTag + "/" + libFileName;

		try (InputStream in = NativeBridge.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				statusMessage = "Pure-Java fallback (no native binary for " + platformTag + ")";
				HardOptimizationMod.LOGGER.info("[HardOptimization] Native Rust core: {}", statusMessage);
				return;
			}

			Path tempDir = Files.createTempDirectory("hardopt-native-");
			tempDir.toFile().deleteOnExit();
			Path tempLib = tempDir.resolve(libFileName);
			Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);
			tempLib.toFile().deleteOnExit();

			System.load(tempLib.toAbsolutePath().toString());
			SymbolLookup lookup = SymbolLookup.loaderLookup();
			Linker linker = Linker.nativeLinker();

			fnVersion = linker.downcallHandle(
				lookup.find("hardopt_version").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT)
			);
			fnHash64 = linker.downcallHandle(
				lookup.find("hardopt_hash64").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
			);
			fnBatchAabbOverlaps = linker.downcallHandle(
				lookup.find("hardopt_batch_aabb_overlaps").orElseThrow(),
				FunctionDescriptor.of(
					ValueLayout.JAVA_LONG,
					ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
					ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
					ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
					ValueLayout.ADDRESS, ValueLayout.JAVA_LONG
				)
			);

			abiVersion = (int) fnVersion.invokeExact();
			available = true;
			statusMessage = "ACTIVE (Rust FFM ABI v" + abiVersion + ", " + platformTag + ")";
			HardOptimizationMod.LOGGER.info("[HardOptimization] Native Rust core loaded: {}", statusMessage);
		} catch (Throwable t) {
			available = false;
			statusMessage = "Pure-Java fallback (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")";
			HardOptimizationMod.LOGGER.warn("[HardOptimization] Failed to load Rust native core, using Java fallback: {}", t.toString());
		}
	}

	public static boolean isAvailable() {
		return available;
	}

	public static int getAbiVersion() {
		return abiVersion;
	}

	public static String getStatusMessage() {
		return statusMessage;
	}

	public static long hash64(MemorySegment segment, long byteLength) {
		if (!available) {
			long h = 0xcbf29ce484222325L;
			for (long i = 0; i < byteLength; i++) {
				h ^= (segment.get(ValueLayout.JAVA_BYTE, i) & 0xFFL);
				h *= 0x100000001b3L;
			}
			return h;
		}
		try {
			return (long) fnHash64.invokeExact(segment, byteLength);
		} catch (Throwable t) {
			throw new RuntimeException("Native hash64 call failed", t);
		}
	}

	public static long batchAabbOverlaps(
		MemorySegment aabbs, long count,
		double qMinX, double qMinY, double qMinZ,
		double qMaxX, double qMaxY, double qMaxZ,
		MemorySegment outIndices, long outCapacity
	) {
		if (!available) {
			long written = 0;
			for (long i = 0; i < count; i++) {
				long base = i * 6L;
				double minX = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base);
				double minY = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base + 1);
				double minZ = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base + 2);
				double maxX = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base + 3);
				double maxY = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base + 4);
				double maxZ = aabbs.getAtIndex(ValueLayout.JAVA_DOUBLE, base + 5);
				if (maxX > qMinX && minX < qMaxX && maxY > qMinY && minY < qMaxY && maxZ > qMinZ && minZ < qMaxZ) {
					outIndices.setAtIndex(ValueLayout.JAVA_INT, written++, (int) i);
					if (written == outCapacity) break;
				}
			}
			return written;
		}
		try {
			return (long) fnBatchAabbOverlaps.invokeExact(
				aabbs, count,
				qMinX, qMinY, qMinZ, qMaxX, qMaxY, qMaxZ,
				outIndices, outCapacity
			);
		} catch (Throwable t) {
			throw new RuntimeException("Native batchAabbOverlaps call failed", t);
		}
	}

	private static String detectPlatformTag() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		String osTag = os.contains("win") ? "windows" : (os.contains("mac") || os.contains("darwin") ? "macos" : "linux");
		String archTag = (arch.equals("amd64") || arch.equals("x86_64")) ? "x86_64" : arch;
		return osTag + "-" + archTag;
	}

	private static String mapLibraryFileName(String baseName) {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return baseName + ".dll";
		}
		if (os.contains("mac") || os.contains("darwin")) {
			return "lib" + baseName + ".dylib";
		}
		return "lib" + baseName + ".so";
	}
}

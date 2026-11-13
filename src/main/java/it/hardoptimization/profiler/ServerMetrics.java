package it.hardoptimization.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ServerMetrics {
	private static final int SAMPLE_SIZE = 200;
	private static final long[] TICK_TIMES_NS = new long[SAMPLE_SIZE];
	private static int writeIndex = 0;
	private static int count = 0;
	private static volatile long currentTickStartNs = System.nanoTime();
	private static final AtomicLong totalTicks = new AtomicLong(0);

	private ServerMetrics() {
	}

	public static void onTickStart() {
		currentTickStartNs = System.nanoTime();
	}

	public static void onTickEnd() {
		long elapsed = System.nanoTime() - currentTickStartNs;
		synchronized (TICK_TIMES_NS) {
			TICK_TIMES_NS[writeIndex] = elapsed;
			writeIndex = (writeIndex + 1) % SAMPLE_SIZE;
			if (count < SAMPLE_SIZE) {
				count++;
			}
		}
		totalTicks.incrementAndGet();
	}

	public static double getCurrentTickElapsedMs() {
		return (System.nanoTime() - currentTickStartNs) / 1_000_000.0;
	}

	public static long getTotalTicks() {
		return totalTicks.get();
	}

	public record Snapshot(
		double tps,
		double avgMspt,
		double minMspt,
		double p95Mspt,
		double p99Mspt,
		double maxMspt,
		long heapUsedMb,
		long heapCommittedMb,
		long heapMaxMb,
		long gcCount,
		long gcTimeMs,
		int sampleCount
	) {
	}

	public static Snapshot capture() {
		long[] copy;
		int n;
		synchronized (TICK_TIMES_NS) {
			n = count;
			copy = new long[n];
			System.arraycopy(TICK_TIMES_NS, 0, copy, 0, n);
		}

		double avgMs = 0.0;
		double minMs = 0.0;
		double p95Ms = 0.0;
		double p99Ms = 0.0;
		double maxMs = 0.0;

		if (n > 0) {
			Arrays.sort(copy);
			long sum = 0;
			for (long v : copy) {
				sum += v;
			}
			avgMs = (sum / (double) n) / 1_000_000.0;
			minMs = copy[0] / 1_000_000.0;
			maxMs = copy[n - 1] / 1_000_000.0;
			p95Ms = copy[Math.min(n - 1, (int) Math.floor(n * 0.95))] / 1_000_000.0;
			p99Ms = copy[Math.min(n - 1, (int) Math.floor(n * 0.99))] / 1_000_000.0;
		}

		double tps = avgMs <= 50.0 ? 20.0 : Math.min(20.0, 1000.0 / avgMs);

		Runtime rt = Runtime.getRuntime();
		long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		long committedMb = rt.totalMemory() / (1024L * 1024L);
		long maxMb = rt.maxMemory() / (1024L * 1024L);

		long gcCount = 0;
		long gcTimeMs = 0;
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean bean : gcBeans) {
			long c = bean.getCollectionCount();
			long t = bean.getCollectionTime();
			if (c > 0) gcCount += c;
			if (t > 0) gcTimeMs += t;
		}

		return new Snapshot(tps, avgMs, minMs, p95Ms, p99Ms, maxMs, usedMb, committedMb, maxMb, gcCount, gcTimeMs, n);
	}
}

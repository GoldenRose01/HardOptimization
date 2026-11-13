package it.hardoptimization.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.hardoptimization.HardOptimizationMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HardOptimizationConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hardoptimization.json");

	private static HardOptimizationConfig instance;

	public boolean enableTickBudgetedScheduler = true;
	public double targetMsptBudget = 28.0;
	public int maxWorkerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

	public boolean enableChunkLookupCache = true;
	public boolean enableShapesFastPath = true;
	public boolean enableEntityCollisionFastPath = true;
	public boolean enableChunkSaveThrottling = true;
	public boolean enableNetworkBufferOptimization = true;
	public boolean yieldToConflictingMods = true;

	public static synchronized HardOptimizationConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static synchronized HardOptimizationConfig reload() {
		instance = load();
		return instance;
	}

	private static HardOptimizationConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				HardOptimizationConfig loaded = GSON.fromJson(reader, HardOptimizationConfig.class);
				if (loaded != null) {
					loaded.sanitize();
					loaded.save();
					return loaded;
				}
			} catch (Exception e) {
				HardOptimizationMod.LOGGER.warn("[HardOptimization] Failed to read config, using defaults: {}", e.getMessage());
			}
		}
		HardOptimizationConfig defaults = new HardOptimizationConfig();
		defaults.sanitize();
		defaults.save();
		return defaults;
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			HardOptimizationMod.LOGGER.warn("[HardOptimization] Failed to save config: {}", e.getMessage());
		}
	}

	private void sanitize() {
		if (targetMsptBudget < 5.0 || targetMsptBudget > 49.0) {
			targetMsptBudget = 28.0;
		}
		int maxCores = Math.max(2, Runtime.getRuntime().availableProcessors());
		if (maxWorkerThreads < 1 || maxWorkerThreads > maxCores) {
			maxWorkerThreads = Math.max(2, maxCores / 2);
		}
	}
}

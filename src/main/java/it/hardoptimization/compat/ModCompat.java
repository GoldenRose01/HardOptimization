package it.hardoptimization.compat;

import it.hardoptimization.config.HardOptimizationConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModCompat {
	public enum ModuleStatus {
		ACTIVE,
		DISABLED_BY_CONFIG,
		YIELDED_TO_MOD
	}

	public record ModuleState(String name, ModuleStatus status, String reason) {
	}

	private static final Map<String, ModuleState> STATES = new LinkedHashMap<>();

	private ModCompat() {
	}

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	public static boolean shouldApplyModule(String moduleName, boolean configEnabled, String... conflictingModIds) {
		HardOptimizationConfig config = HardOptimizationConfig.get();
		if (!configEnabled) {
			STATES.put(moduleName, new ModuleState(moduleName, ModuleStatus.DISABLED_BY_CONFIG, "Disabled in hardoptimization.json"));
			return false;
		}
		if (config.yieldToConflictingMods) {
			for (String modId : conflictingModIds) {
				if (isModLoaded(modId)) {
					STATES.put(moduleName, new ModuleState(moduleName, ModuleStatus.YIELDED_TO_MOD, "Yielded to installed mod '" + modId + "'"));
					return false;
				}
			}
		}
		STATES.put(moduleName, new ModuleState(moduleName, ModuleStatus.ACTIVE, "Running"));
		return true;
	}

	public static Map<String, ModuleState> getStates() {
		return Collections.unmodifiableMap(STATES);
	}
}

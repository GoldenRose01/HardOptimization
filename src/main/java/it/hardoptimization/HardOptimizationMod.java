package it.hardoptimization;

import it.hardoptimization.command.HardOptCommand;
import it.hardoptimization.config.HardOptimizationConfig;
import it.hardoptimization.nativebridge.NativeBridge;
import it.hardoptimization.scheduler.BackgroundScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardOptimizationMod implements ModInitializer {
	public static final String MOD_ID = "hardoptimization";
	public static final Logger LOGGER = LoggerFactory.getLogger("HardOptimization");

	@Override
	public void onInitialize() {
		HardOptimizationConfig cfg = HardOptimizationConfig.get();
		NativeBridge.init();
		LOGGER.info("[HardOptimization] Initialized for Minecraft 26.3 (MSPT budget: {} ms, workers: {})",
			cfg.targetMsptBudget, cfg.maxWorkerThreads);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> BackgroundScheduler.init());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BackgroundScheduler.shutdown());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			HardOptCommand.register(dispatcher)
		);
	}
}

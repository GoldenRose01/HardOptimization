package it.hardoptimization.mixin.world;

import it.hardoptimization.config.HardOptimizationConfig;
import it.hardoptimization.profiler.ServerMetrics;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	@ModifyVariable(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"), argsOnly = true)
	private BooleanSupplier hardopt$budgetChunkMapTick(BooleanSupplier originalHaveTime) {
		HardOptimizationConfig cfg = HardOptimizationConfig.get();
		if (!cfg.enableChunkSaveThrottling) {
			return originalHaveTime;
		}
		final double budgetMs = cfg.targetMsptBudget;
		return () -> originalHaveTime.getAsBoolean() && ServerMetrics.getCurrentTickElapsedMs() < budgetMs;
	}
}

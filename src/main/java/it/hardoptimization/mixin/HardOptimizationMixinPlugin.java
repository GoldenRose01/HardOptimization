package it.hardoptimization.mixin;

import it.hardoptimization.compat.ModCompat;
import it.hardoptimization.config.HardOptimizationConfig;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class HardOptimizationMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
		HardOptimizationConfig.get();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		HardOptimizationConfig cfg = HardOptimizationConfig.get();

		if (mixinClassName.endsWith("MinecraftServerMixin")) {
			return ModCompat.shouldApplyModule("TickProfilerAndScheduler", true);
		}
		if (mixinClassName.endsWith("LevelMixin")) {
			return ModCompat.shouldApplyModule("ChunkLookupCache", cfg.enableChunkLookupCache, "lithium");
		}
		if (mixinClassName.endsWith("ShapesMixin")) {
			return ModCompat.shouldApplyModule("ShapesFastPath", cfg.enableShapesFastPath, "lithium");
		}
		if (mixinClassName.endsWith("EntityGetterMixin")) {
			return ModCompat.shouldApplyModule("EntityCollisionFastPath", cfg.enableEntityCollisionFastPath, "lithium");
		}
		if (mixinClassName.endsWith("ChunkMapMixin")) {
			return ModCompat.shouldApplyModule("ChunkSaveThrottling", cfg.enableChunkSaveThrottling, "c2me");
		}
		if (mixinClassName.endsWith("CompressionEncoderMixin")) {
			return ModCompat.shouldApplyModule("NetworkCompressionOptimization", cfg.enableNetworkBufferOptimization, "krypton");
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}

package it.hardoptimization.mixin.world;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunk.class)
public interface LevelChunkAccessor {
	@Accessor("loaded")
	boolean hardopt$isLoaded();
}

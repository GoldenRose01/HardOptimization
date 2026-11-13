package it.hardoptimization.mixin.world;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Shadow
	@Final
	private Thread thread;

	@Shadow
	@Final
	private boolean isClientSide;

	@Unique
	private int hardopt$cacheX0 = Integer.MIN_VALUE;
	@Unique
	private int hardopt$cacheZ0 = Integer.MIN_VALUE;
	@Unique
	private LevelChunk hardopt$cacheChunk0;

	@Unique
	private int hardopt$cacheX1 = Integer.MIN_VALUE;
	@Unique
	private int hardopt$cacheZ1 = Integer.MIN_VALUE;
	@Unique
	private LevelChunk hardopt$cacheChunk1;

	@Inject(method = "getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("HEAD"), cancellable = true)
	private void hardopt$fastGetChunk(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
		if (this.isClientSide || Thread.currentThread() != this.thread) {
			return;
		}
		LevelChunk c0 = this.hardopt$cacheChunk0;
		if (c0 != null && this.hardopt$cacheX0 == chunkX && this.hardopt$cacheZ0 == chunkZ
			&& ((LevelChunkAccessor) (Object) c0).hardopt$isLoaded()) {
			cir.setReturnValue(c0);
			return;
		}
		LevelChunk c1 = this.hardopt$cacheChunk1;
		if (c1 != null && this.hardopt$cacheX1 == chunkX && this.hardopt$cacheZ1 == chunkZ
			&& ((LevelChunkAccessor) (Object) c1).hardopt$isLoaded()) {
			// Promote slot 1 -> slot 0
			this.hardopt$cacheX1 = this.hardopt$cacheX0;
			this.hardopt$cacheZ1 = this.hardopt$cacheZ0;
			this.hardopt$cacheChunk1 = c0;
			this.hardopt$cacheX0 = chunkX;
			this.hardopt$cacheZ0 = chunkZ;
			this.hardopt$cacheChunk0 = c1;
			cir.setReturnValue(c1);
		}
	}

	@Inject(method = "getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("RETURN"))
	private void hardopt$cacheGetChunk(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
		if (this.isClientSide || Thread.currentThread() != this.thread) {
			return;
		}
		LevelChunk result = cir.getReturnValue();
		if (result != null && result != this.hardopt$cacheChunk0) {
			this.hardopt$cacheX1 = this.hardopt$cacheX0;
			this.hardopt$cacheZ1 = this.hardopt$cacheZ0;
			this.hardopt$cacheChunk1 = this.hardopt$cacheChunk0;
			this.hardopt$cacheX0 = chunkX;
			this.hardopt$cacheZ0 = chunkZ;
			this.hardopt$cacheChunk0 = result;
		}
	}
}

package it.hardoptimization.mixin.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	public abstract boolean isDeadOrDying();

	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void hardopt$skipPushWhenDying(CallbackInfo ci) {
		if (this.isDeadOrDying()) {
			ci.cancel();
		}
	}
}

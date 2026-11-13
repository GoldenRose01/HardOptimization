package it.hardoptimization.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
	@Inject(method = "getEntityCollisions", at = @At("HEAD"), cancellable = true)
	private void hardopt$fastEmptyEntityCollisions(Entity entity, AABB box, CallbackInfoReturnable<List<VoxelShape>> cir) {
		if (box.getSize() < 1.0E-7) {
			cir.setReturnValue(List.of());
		}
	}
}

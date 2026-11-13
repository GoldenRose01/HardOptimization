package it.hardoptimization.mixin.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	@Inject(method = "isFullContainer", at = @At("HEAD"), cancellable = true)
	private static void hardopt$fastIsFullContainer(Container container, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (container instanceof WorldlyContainer) {
			return;
		}
		if (container.isEmpty()) {
			cir.setReturnValue(false);
			return;
		}
		int size = container.getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = container.getItem(i);
			if (stack.getCount() < stack.getMaxStackSize()) {
				cir.setReturnValue(false);
				return;
			}
		}
		cir.setReturnValue(true);
	}
}

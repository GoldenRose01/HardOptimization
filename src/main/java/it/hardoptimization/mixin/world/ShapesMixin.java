package it.hardoptimization.mixin.world;

import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Shapes.class)
public abstract class ShapesMixin {
	@Shadow
	@Final
	private static VoxelShape EMPTY;

	@Shadow
	@Final
	private static VoxelShape BLOCK;

	@Inject(method = "joinIsNotEmpty", at = @At("HEAD"), cancellable = true)
	private static void hardopt$fastJoinIsNotEmpty(VoxelShape shape1, VoxelShape shape2, BooleanOp op, CallbackInfoReturnable<Boolean> cir) {
		if (shape1 == shape2) {
			if (op == BooleanOp.NOT_SAME) {
				cir.setReturnValue(false);
				return;
			}
			if (op == BooleanOp.AND || op == BooleanOp.OR) {
				cir.setReturnValue(!shape1.isEmpty());
				return;
			}
		}
		if (shape1 == EMPTY && shape2 == EMPTY) {
			cir.setReturnValue(op.apply(false, false));
			return;
		}
		if (shape1 == BLOCK && shape2 == BLOCK) {
			cir.setReturnValue(op.apply(true, true));
		}
	}
}

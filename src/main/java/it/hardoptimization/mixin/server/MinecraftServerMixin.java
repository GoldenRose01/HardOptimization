package it.hardoptimization.mixin.server;

import it.hardoptimization.profiler.ServerMetrics;
import it.hardoptimization.scheduler.BackgroundScheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "tickServer", at = @At("HEAD"))
	private void hardopt$onTickStart(BooleanSupplier haveTime, CallbackInfo ci) {
		ServerMetrics.onTickStart();
	}

	@Inject(method = "tickServer", at = @At("RETURN"))
	private void hardopt$onTickEnd(BooleanSupplier haveTime, CallbackInfo ci) {
		BackgroundScheduler.drainWithinTickBudget();
		ServerMetrics.onTickEnd();
	}
}

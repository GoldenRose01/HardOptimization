package it.hardoptimization.mixin.network;

import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.zip.Deflater;

@Mixin(CompressionEncoder.class)
public abstract class CompressionEncoderMixin {
	@Shadow
	@Final
	private Deflater deflater;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void hardopt$tuneDeflater(int threshold, CallbackInfo ci) {
		// Compression level 4 cuts CPU time significantly vs default level 6
		// while producing standard zlib streams 100% compatible with all vanilla/modded clients.
		this.deflater.setLevel(4);
	}
}

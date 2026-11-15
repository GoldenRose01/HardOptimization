package it.hardoptimization.command;

import com.mojang.brigadier.CommandDispatcher;
import it.hardoptimization.compat.ModCompat;
import it.hardoptimization.config.HardOptimizationConfig;
import it.hardoptimization.nativebridge.NativeBridge;
import it.hardoptimization.profiler.ServerMetrics;
import it.hardoptimization.scheduler.BackgroundScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Locale;

public final class HardOptCommand {
	private HardOptCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hardopt")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(ctx -> showStatus(ctx.getSource()))
			.then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource())))
			.then(Commands.literal("modules").executes(ctx -> showModules(ctx.getSource())))
			.then(Commands.literal("memory")
				.executes(ctx -> showMemory(ctx.getSource()))
				.then(Commands.literal("gc").executes(ctx -> runGc(ctx.getSource()))))
			.then(Commands.literal("reload").executes(ctx -> reloadConfig(ctx.getSource())))
		);
	}

	private static int showStatus(CommandSourceStack source) {
		ServerMetrics.Snapshot s = ServerMetrics.capture();
		ChatFormatting tpsColor = s.tps() >= 19.5 ? ChatFormatting.GREEN : (s.tps() >= 17.0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
		ChatFormatting msptColor = s.avgMspt() <= 30.0 ? ChatFormatting.GREEN : (s.avgMspt() <= 45.0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
		ChatFormatting ramColor = s.heapUsedMb() < 16384 ? ChatFormatting.GREEN : ChatFormatting.RED;

		source.sendSuccess(() -> Component.literal("=== HardOptimization Server Status ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
			"TPS: %.2f | MSPT (avg/min/p95/p99/max): %.2f / %.2f / %.2f / %.2f / %.2f ms",
			s.tps(), s.avgMspt(), s.minMspt(), s.p95Mspt(), s.p99Mspt(), s.maxMspt()))
			.withStyle(msptColor), false);
		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
			"Heap RAM: %d MB used / %d MB committed / %d MB max",
			s.heapUsedMb(), s.heapCommittedMb(), s.heapMaxMb()))
			.withStyle(ramColor), false);
		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
			"GC: %d collections (%d ms total) | Scheduler queue: %d | Ticks: %d",
			s.gcCount(), s.gcTimeMs(), BackgroundScheduler.getPendingMainThreadTasks(), ServerMetrics.getTotalTicks()))
			.withStyle(tpsColor), false);
		return 1;
	}

	private static int showModules(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("=== HardOptimization Modules ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (ModCompat.ModuleState state : ModCompat.getStates().values()) {
			ChatFormatting color = switch (state.status()) {
				case ACTIVE -> ChatFormatting.GREEN;
				case YIELDED_TO_MOD -> ChatFormatting.YELLOW;
				case DISABLED_BY_CONFIG -> ChatFormatting.GRAY;
			};
			source.sendSuccess(() -> Component.literal(
				String.format(" - %s: %s (%s)", state.name(), state.status(), state.reason())
			).withStyle(color), false);
		}
		ChatFormatting nativeColor = NativeBridge.isAvailable() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
		source.sendSuccess(() -> Component.literal(" - RustNativeCore: " + NativeBridge.getStatusMessage()).withStyle(nativeColor), false);
		return 1;
	}

	private static int showMemory(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("=== JVM Memory Pools ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			MemoryUsage usage = pool.getUsage();
			if (usage == null) continue;
			long usedMb = usage.getUsed() / (1024L * 1024L);
			long maxMb = usage.getMax() > 0 ? usage.getMax() / (1024L * 1024L) : -1;
			source.sendSuccess(() -> Component.literal(String.format(
				" - %s (%s): %d MB used%s",
				pool.getName(), pool.getType(), usedMb, maxMb > 0 ? " / " + maxMb + " MB max" : ""
			)).withStyle(ChatFormatting.AQUA), false);
		}
		return 1;
	}

	private static int runGc(CommandSourceStack source) {
		Runtime rt = Runtime.getRuntime();
		long before = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		System.gc();
		long after = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		source.sendSuccess(() -> Component.literal(
			String.format("Advisory GC complete: %d MB -> %d MB (freed %d MB)", before, after, Math.max(0, before - after))
		).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int reloadConfig(CommandSourceStack source) {
		HardOptimizationConfig cfg = HardOptimizationConfig.reload();
		source.sendSuccess(() -> Component.literal(
			String.format(Locale.ROOT, "Reloaded hardoptimization.json (MSPT budget: %.1f ms, workers: %d). Restart required for Mixin toggles.",
				cfg.targetMsptBudget, cfg.maxWorkerThreads)
		).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}
}

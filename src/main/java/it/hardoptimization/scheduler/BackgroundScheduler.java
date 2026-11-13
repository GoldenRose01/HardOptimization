package it.hardoptimization.scheduler;

import it.hardoptimization.HardOptimizationMod;
import it.hardoptimization.config.HardOptimizationConfig;
import it.hardoptimization.profiler.ServerMetrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class BackgroundScheduler {
	private static final ConcurrentLinkedQueue<Runnable> MAIN_THREAD_QUEUE = new ConcurrentLinkedQueue<>();
	private static final AtomicInteger QUEUE_SIZE = new AtomicInteger(0);

	private static volatile ExecutorService cpuPool;
	private static volatile ExecutorService ioVirtualPool;

	private BackgroundScheduler() {
	}

	public static synchronized void init() {
		if (cpuPool != null && !cpuPool.isShutdown()) {
			return;
		}
		int threads = HardOptimizationConfig.get().maxWorkerThreads;
		AtomicInteger id = new AtomicInteger(1);
		cpuPool = Executors.newFixedThreadPool(threads, r -> {
			Thread t = new Thread(r, "HardOpt-Worker-" + id.getAndIncrement());
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			return t;
		});
		ioVirtualPool = Executors.newVirtualThreadPerTaskExecutor();
		HardOptimizationMod.LOGGER.info("[HardOptimization] Initialized scheduler ({} CPU workers + Java 25 Virtual Threads for I/O)", threads);
	}

	public static synchronized void shutdown() {
		if (cpuPool != null) {
			cpuPool.shutdownNow();
			cpuPool = null;
		}
		if (ioVirtualPool != null) {
			ioVirtualPool.shutdownNow();
			ioVirtualPool = null;
		}
		MAIN_THREAD_QUEUE.clear();
		QUEUE_SIZE.set(0);
	}

	public static <T> CompletableFuture<T> supplyCpuAsync(Supplier<T> task) {
		if (cpuPool == null) init();
		return CompletableFuture.supplyAsync(task, cpuPool);
	}

	public static CompletableFuture<Void> runIoAsync(Runnable task) {
		if (ioVirtualPool == null) init();
		return CompletableFuture.runAsync(task, ioVirtualPool);
	}

	public static void enqueueMainThread(Runnable task) {
		MAIN_THREAD_QUEUE.offer(task);
		QUEUE_SIZE.incrementAndGet();
	}

	public static int getPendingMainThreadTasks() {
		return QUEUE_SIZE.get();
	}

	public static int drainWithinTickBudget() {
		HardOptimizationConfig cfg = HardOptimizationConfig.get();
		if (!cfg.enableTickBudgetedScheduler) {
			int ran = 0;
			Runnable r;
			while ((r = MAIN_THREAD_QUEUE.poll()) != null) {
				QUEUE_SIZE.decrementAndGet();
				safeRun(r);
				ran++;
			}
			return ran;
		}

		int ran = 0;
		double budgetMs = cfg.targetMsptBudget;
		while (true) {
			if (ran > 0 && ServerMetrics.getCurrentTickElapsedMs() >= budgetMs) {
				break;
			}
			Runnable r = MAIN_THREAD_QUEUE.poll();
			if (r == null) {
				break;
			}
			QUEUE_SIZE.decrementAndGet();
			safeRun(r);
			ran++;
		}
		return ran;
	}

	private static void safeRun(Runnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			HardOptimizationMod.LOGGER.error("[HardOptimization] Error in scheduled main-thread task", t);
		}
	}
}

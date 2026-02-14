package org.collection.queue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class AbstractPerfTest {

	protected static final int OPERATIONS = 1000000;
	protected static final int CAPACITY = 1 << 20;
	protected static final int BATCH = 32;
	protected static final int ITERATIONS = 10;

	protected abstract int producers();

	protected abstract int consumers();

	protected void warmup(Supplier<VarQueue<Integer>> provider) {
		VarQueue<Integer> q = provider.get();
		for (int i = 0; i < 500000; i++) {
			q.offer(i);
			q.poll();
		}
	}

	protected long runOfferBenchmark(Supplier<VarQueue<Integer>> provider) throws InterruptedException {
		warmup(provider);
		VarQueue<Integer> q = provider.get();

		int producers = producers();
		CountDownLatch latch = new CountDownLatch(producers);
		long start = System.nanoTime();

		for (int p = 0; p < producers; p++) {
			new Thread(() -> {
				for (int i = 0; i < OPERATIONS; i++) {
					q.offer(i);
				}
				latch.countDown();
			}).start();
		}

		latch.await();
		return System.nanoTime() - start;
	}

	protected long runMixedBenchmark(Supplier<VarQueue<Integer>> provider) throws InterruptedException {
		warmup(provider);
		VarQueue<Integer> q = provider.get();

		int producers = producers();
		int consumers = consumers();

		CountDownLatch latch = new CountDownLatch(producers + consumers);
		AtomicInteger consumed = new AtomicInteger(0);
		int total = producers * OPERATIONS;

		// Consumers
		for (int c = 0; c < consumers; c++) {
			new Thread(() -> {
				int local = 0;
				while (true) {
					for (int i = 0; i < BATCH; i++) {
						Integer v = q.poll();
						if (v != null) {
							local++;
						}
					}
					if (local > 0) {
						consumed.addAndGet(local);
						local = 0;
					}
					if (consumed.get() >= total) break;
					Thread.onSpinWait();
				}
				latch.countDown();
			}).start();
		}

		// Producers
		for (int p = 0; p < producers; p++) {
			new Thread(() -> {
				for (int i = 0; i < OPERATIONS; i++) {
					while (!q.offer(i)) Thread.onSpinWait();
				}
				latch.countDown();
			}).start();
		}

		long start = System.nanoTime();
		latch.await();
		return System.nanoTime() - start;
	}

	protected long runPollBenchmark(Supplier<VarQueue<Integer>> provider) {
		warmup(provider);
		VarQueue<Integer> q = provider.get();
		for (int i = 0; i < OPERATIONS; i++) q.offer(i);
		long start = System.nanoTime();
		for (int i = 0; i < OPERATIONS; i++) q.poll();
		return System.nanoTime() - start;
	}

	protected double toMops(long ops, long nanos) {
		return (ops / (nanos / 1_000_000_000.0)) / 1_000_000.0;
	}

	protected void runAll(String name, Supplier<VarQueue<Integer>> provider) throws Exception {
		System.out.println("\n=== " + name + " (" + producers() + "P:" + consumers() + "C) ===");
		long offerNs = 0L;
		for (int i = 0; i < ITERATIONS; i++) {
			offerNs += runOfferBenchmark(provider);
		}
		offerNs /= ITERATIONS;
		System.out.printf("Offer throughput: %.2f M ops/s%n", toMops((long) producers() * OPERATIONS, offerNs));
		long mixedNs = 0L;
		for (int i = 0; i < ITERATIONS; i++) {
			mixedNs += runMixedBenchmark(provider);
		}
		mixedNs /= ITERATIONS;
		System.out.printf("Mixed throughput: %.2f M ops/s%n", toMops((long) producers() * OPERATIONS, mixedNs));
		long pollNs = 0L;
		for (int i = 0; i < ITERATIONS; i++) {
			pollNs = runPollBenchmark(provider);
		}
		pollNs /= ITERATIONS;
		System.out.printf("Poll throughput:  %.2f M ops/s%n", toMops(OPERATIONS, pollNs));
	}
}

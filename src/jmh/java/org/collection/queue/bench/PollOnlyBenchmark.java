package org.collection.queue.bench;

import java.util.concurrent.TimeUnit;

import org.collection.queue.bench.adapter.QueueAdapter;
import org.collection.queue.bench.adapter.QueueFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Single-thread sanity baseline.
 *
 * <p>Pre-fills a queue with {@code batch} elements before each invocation
 * and measures how fast the current thread can drain it. This is the
 * closest equivalent to the existing custom "Poll throughput" numbers in
 * {@code Readme.md} — included so contributors can cross-check JMH
 * output against the hand-rolled numbers during migration.
 *
 * <p>This is <em>not</em> a concurrency benchmark. Concurrent
 * producer/consumer behaviour is measured in
 * {@link SpscThroughput}, {@link MpmcThroughput} and friends.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class PollOnlyBenchmark {

    @Param({"varqueue", "jctools-vh", "jctools-unsafe", "abq"})
    public String impl;

    @Param({"spsc", "mpmc"})
    public String pattern;

    @Param({"65536"})
    public int capacity;

    @Param({"1024"})
    public int batch;

    private QueueAdapter<Integer> queue;
    private final Integer payload = 42;

    @Setup(Level.Iteration)
    public void setUp() {
        queue = QueueFactory.create(pattern, impl, capacity);
    }

    /**
     * Re-fill the queue before every measured invocation so {@link #poll}
     * always has {@code batch} elements to drain. Uses
     * {@link Level#Invocation} because the benchmark method consumes
     * exactly {@code batch} elements per call.
     */
    @Setup(Level.Invocation)
    public void refill() {
        for (int i = 0; i < batch; i++) {
            queue.offer(payload);
        }
    }

    @Benchmark
    public void poll(Blackhole bh) {
        for (int i = 0; i < batch; i++) {
            bh.consume(queue.poll());
        }
    }
}

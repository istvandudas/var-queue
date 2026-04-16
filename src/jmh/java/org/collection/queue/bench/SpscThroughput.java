package org.collection.queue.bench;

import java.util.concurrent.TimeUnit;

import org.collection.queue.bench.state.SpscState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Throughput benchmark for the SPSC concurrency pattern.
 *
 * <p>One producer thread and one consumer thread run concurrently in the
 * same {@code @Group}. Producers spin on a full queue; consumers spin on
 * an empty queue. This measures steady-state throughput under genuine
 * producer/consumer coupling — not the uncontended fast path.
 *
 * <p>The polled value is always passed through {@link Blackhole} so that
 * JIT dead-code elimination cannot remove the read.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SpscThroughput {

    @Benchmark
    @Group("spsc")
    @GroupThreads(1)
    public void offer(SpscState s) {
        while (!s.queue.offer(s.payload)) {
            Thread.onSpinWait();
        }
    }

    @Benchmark
    @Group("spsc")
    @GroupThreads(1)
    public void poll(SpscState s, Blackhole bh) {
        Integer v;
        while ((v = s.queue.poll()) == null) {
            Thread.onSpinWait();
        }
        bh.consume(v);
    }
}

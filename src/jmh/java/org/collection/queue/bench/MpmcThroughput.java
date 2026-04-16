package org.collection.queue.bench;

import java.util.concurrent.TimeUnit;

import org.collection.queue.bench.state.MpmcState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Throughput benchmark for the MPMC concurrency pattern.
 *
 * <p>Two producer threads and two consumer threads run concurrently in
 * the same {@code @Group}. This is the most contention-heavy shape in
 * the MVP matrix and is the benchmark where the split between queues
 * tends to be largest.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class MpmcThroughput {

    @Benchmark
    @Group("mpmc")
    @GroupThreads(2)
    public void offer(MpmcState s) {
        while (!s.queue.offer(s.payload)) {
            Thread.onSpinWait();
        }
    }

    @Benchmark
    @Group("mpmc")
    @GroupThreads(2)
    public void poll(MpmcState s, Blackhole bh) {
        Integer v;
        while ((v = s.queue.poll()) == null) {
            Thread.onSpinWait();
        }
        bh.consume(v);
    }
}

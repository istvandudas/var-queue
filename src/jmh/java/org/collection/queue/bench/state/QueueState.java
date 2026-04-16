package org.collection.queue.bench.state;

import org.collection.queue.bench.adapter.QueueAdapter;
import org.collection.queue.bench.adapter.QueueFactory;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Shared JMH state for concurrent producer/consumer benchmarks.
 *
 * <p>Uses {@link Scope#Group} so that all producer and consumer threads
 * inside a {@code @Group("...")} share exactly one queue instance —
 * essential for measuring real producer/consumer contention rather than
 * the uncontended fast path.
 *
 * <p>The queue is rebuilt at {@link Level#Iteration} rather than
 * {@link Level#Trial} so that one measurement iteration's residual
 * state cannot bias the next: each iteration starts with a fresh queue
 * and ends with a drain.
 *
 * <p>Subclasses fix the {@code pattern} field per concurrency shape so
 * JMH only has to vary {@code impl} and {@code capacity} in its matrix.
 * The {@code payload} is cached as a boxed {@link Integer} field rather
 * than allocated per-call: this keeps the benchmark focused on queue
 * cost rather than autoboxing cost.
 */
@State(Scope.Group)
public abstract class QueueState {

    @Param({"65536"})
    public int capacity;

    @Param({"varqueue", "jctools-vh", "jctools-unsafe", "abq"})
    public String impl;

    /** Cached payload — one allocation, many offers. */
    public final Integer payload = 42;

    public QueueAdapter<Integer> queue;

    /**
     * Concurrency shape the subclass is measuring. Must be one of
     * {@code spsc}, {@code spmc}, {@code mpsc}, {@code mpmc}.
     */
    protected abstract String pattern();

    @Setup(Level.Iteration)
    public void setUp() {
        queue = QueueFactory.create(pattern(), impl, capacity);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        // Drain whatever the producers left behind so the next iteration
        // starts with a clean, empty queue.
        while (queue.poll() != null) {
            // spin drain
        }
    }
}

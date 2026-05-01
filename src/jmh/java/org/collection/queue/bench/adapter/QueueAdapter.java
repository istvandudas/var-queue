package org.collection.queue.bench.adapter;

/**
 * Narrow interface used exclusively inside JMH benchmarks to drive
 * queue implementations from different vendors through a uniform surface.
 *
 * <p>Kept intentionally smaller than {@link org.collection.queue.VarQueue}:
 * only the three hot-path operations actually measured in benchmarks are
 * here. Size/capacity accessors are omitted so that adapters do not need
 * to fake them (as {@code ClqAdapter} does today for
 * {@link java.util.concurrent.ConcurrentLinkedQueue}).
 *
 * @param <E> element type
 */
public interface QueueAdapter<E> {

    /**
     * Attempts to insert the element without blocking.
     *
     * @return {@code true} if inserted, {@code false} if the queue was full
     */
    boolean offer(E e);

    /**
     * Removes and returns the head element, or {@code null} if empty.
     */
    E poll();

    /**
     * Returns the head element without removing it, or {@code null} if empty.
     */
    E peek();
}

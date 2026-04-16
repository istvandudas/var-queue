package org.collection.queue.bench.adapter;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adapter wrapping JDK {@link java.util.concurrent} queues so they can
 * serve as benchmark baselines.
 *
 * <p>Two flavours are exposed:
 * <ul>
 *   <li>{@link #clq()} — unbounded {@link ConcurrentLinkedQueue}; the
 *       classic multi-producer/multi-consumer baseline. Ignores the
 *       requested capacity.</li>
 *   <li>{@link #arrayBlocking(int)} — bounded {@link ArrayBlockingQueue};
 *       a fairer comparison point for the ring-buffer queues since it is
 *       also array-backed and bounded.</li>
 * </ul>
 *
 * @param <E> element type
 */
public final class JucAdapter<E> implements QueueAdapter<E> {

    private final Queue<E> delegate;

    private JucAdapter(Queue<E> delegate) {
        this.delegate = delegate;
    }

    public static <E> JucAdapter<E> clq() {
        return new JucAdapter<>(new ConcurrentLinkedQueue<>());
    }

    public static <E> JucAdapter<E> arrayBlocking(int capacity) {
        return new JucAdapter<>(new ArrayBlockingQueue<>(capacity));
    }

    @Override
    public boolean offer(E e) {
        return delegate.offer(e);
    }

    @Override
    public E poll() {
        return delegate.poll();
    }

    @Override
    public E peek() {
        return delegate.peek();
    }
}

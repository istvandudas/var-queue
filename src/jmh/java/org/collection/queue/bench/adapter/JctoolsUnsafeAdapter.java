package org.collection.queue.bench.adapter;

import java.util.Queue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpmcArrayQueue;
import org.jctools.queues.SpscArrayQueue;

/**
 * Adapter wrapping the default (Unsafe-backed) JCTools bounded array queues
 * from the {@code org.jctools:jctools-core} artifact.
 *
 * <p>This is the {@code sun.misc.Unsafe}-based variant — the historical
 * state of the art for JVM queue throughput. Included as a performance
 * ceiling reference. Note that on Java 26 (March 2026) this path triggers
 * startup failures by default due to
 * <a href="https://openjdk.org/jeps/471">JEP 471</a>.
 *
 * @param <E> element type
 */
public final class JctoolsUnsafeAdapter<E> implements QueueAdapter<E> {

    private final Queue<E> delegate;

    private JctoolsUnsafeAdapter(Queue<E> delegate) {
        this.delegate = delegate;
    }

    public static <E> JctoolsUnsafeAdapter<E> spsc(int capacity) {
        return new JctoolsUnsafeAdapter<>(new SpscArrayQueue<>(capacity));
    }

    public static <E> JctoolsUnsafeAdapter<E> spmc(int capacity) {
        return new JctoolsUnsafeAdapter<>(new SpmcArrayQueue<>(capacity));
    }

    public static <E> JctoolsUnsafeAdapter<E> mpsc(int capacity) {
        return new JctoolsUnsafeAdapter<>(new MpscArrayQueue<>(capacity));
    }

    public static <E> JctoolsUnsafeAdapter<E> mpmc(int capacity) {
        return new JctoolsUnsafeAdapter<>(new MpmcArrayQueue<>(capacity));
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

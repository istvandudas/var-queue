package org.collection.queue.bench.adapter;

import java.util.Queue;

import org.jctools.queues.varhandle.MpmcVarHandleArrayQueue;
import org.jctools.queues.varhandle.MpscVarHandleArrayQueue;
import org.jctools.queues.varhandle.SpmcVarHandleArrayQueue;
import org.jctools.queues.varhandle.SpscVarHandleArrayQueue;

/**
 * Adapter wrapping the VarHandle-backed JCTools bounded array queues
 * from the {@code org.jctools:jctools-core-jdk11} artifact.
 *
 * <p>This is the apples-to-apples comparison with var-queue itself: both
 * use {@link java.lang.invoke.VarHandle} under the hood, both target
 * post-Unsafe JVMs, and neither will be affected by the JEP 471 startup
 * failure behaviour in Java 26.
 *
 * @param <E> element type
 */
public final class JctoolsVhAdapter<E> implements QueueAdapter<E> {

    private final Queue<E> delegate;

    private JctoolsVhAdapter(Queue<E> delegate) {
        this.delegate = delegate;
    }

    public static <E> JctoolsVhAdapter<E> spsc(int capacity) {
        return new JctoolsVhAdapter<>(new SpscVarHandleArrayQueue<>(capacity));
    }

    public static <E> JctoolsVhAdapter<E> spmc(int capacity) {
        return new JctoolsVhAdapter<>(new SpmcVarHandleArrayQueue<>(capacity));
    }

    public static <E> JctoolsVhAdapter<E> mpsc(int capacity) {
        return new JctoolsVhAdapter<>(new MpscVarHandleArrayQueue<>(capacity));
    }

    public static <E> JctoolsVhAdapter<E> mpmc(int capacity) {
        return new JctoolsVhAdapter<>(new MpmcVarHandleArrayQueue<>(capacity));
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

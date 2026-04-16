package org.collection.queue.bench.adapter;

import org.collection.queue.MPMCVarQueue;
import org.collection.queue.MPSCVarQueue;
import org.collection.queue.SPMCVarQueue;
import org.collection.queue.SPSCVarQueue;
import org.collection.queue.VarQueue;

/**
 * Adapter wrapping the project's own {@link VarQueue} implementations.
 *
 * <p>Access is routed through the {@link VarQueue} interface so that the
 * adapter does not have to branch on the concrete class in the hot path —
 * the JIT will devirtualize the call at each benchmark call site because
 * the concrete type is stable within a forked JVM.
 *
 * @param <E> element type
 */
public final class VarQueueAdapter<E> implements QueueAdapter<E> {

    private final VarQueue<E> delegate;

    private VarQueueAdapter(VarQueue<E> delegate) {
        this.delegate = delegate;
    }

    public static <E> VarQueueAdapter<E> spsc(int capacity) {
        return new VarQueueAdapter<>(new SPSCVarQueue<>(capacity));
    }

    public static <E> VarQueueAdapter<E> spmc(int capacity) {
        return new VarQueueAdapter<>(new SPMCVarQueue<>(capacity));
    }

    public static <E> VarQueueAdapter<E> mpsc(int capacity) {
        return new VarQueueAdapter<>(new MPSCVarQueue<>(capacity));
    }

    public static <E> VarQueueAdapter<E> mpmc(int capacity) {
        return new VarQueueAdapter<>(new MPMCVarQueue<>(capacity));
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

package org.collection.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adapter that wraps a standard ConcurrentLinkedQueue and exposes it
 * through the MPSCQueue interface so it can be benchmarked alongside
 * MPSCVarQueue.
 */
public final class ClqAdapter<E> implements VarQueue<E> {

    private final Queue<E> q = new ConcurrentLinkedQueue<>();

    @Override
    public boolean offer(E e) {
        return q.offer(e);
    }

    @Override
    public E poll() {
        return q.poll();
    }

    @Override
    public E peek() {
        return q.peek();
    }

    @Override
    public boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public int size() {
        return q.size();
    }
}

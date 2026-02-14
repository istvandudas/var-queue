package org.collection.queue;

public interface VarQueue<E> {
	boolean offer(E e);
	E poll();
	E peek();
	boolean isEmpty();
	int size();
}


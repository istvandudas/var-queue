package org.collection.queue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

public final class MPMCVarQueue<E> implements VarQueue<E> {

	private static final class Cell<E> {
		volatile long seq;
		E value;

		Cell(long seq) {
			this.seq = seq;
		}
	}

	private static final VarHandle CELL_SEQ;
	private static final VarHandle CELL_VALUE;
	private static final VarHandle HEAD;
	private static final VarHandle TAIL;

	static {
		try {
			MethodHandles.Lookup l = MethodHandles.lookup();
			CELL_SEQ = l.findVarHandle(Cell.class, "seq", long.class);
			CELL_VALUE = l.findVarHandle(Cell.class, "value", Object.class);
			HEAD = l.findVarHandle(MPMCVarQueue.class, "head", long.class);
			TAIL = l.findVarHandle(MPMCVarQueue.class, "tail", long.class);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Cell<E>[] buffer;
	private final int mask;
	private final int capacity;

	private volatile long head = 0L;
	private volatile long tail = 0L;

	public MPMCVarQueue(int requestedCapacity) {
		int c = roundToPowerOfTwo(requestedCapacity);
		this.capacity = c;
		this.mask = c - 1;
		this.buffer = (Cell<E>[]) new Cell[c];

		for (int i = 0; i < c; i++) {
			buffer[i] = new Cell<>(i);
		}
	}

	private static int roundToPowerOfTwo(int value) {
		int highest = Integer.highestOneBit(value);
		return (value == highest) ? value : highest << 1;
	}

	@Override
	public boolean offer(E e) {
		Objects.requireNonNull(e);

		while (true) {
			long t = (long) TAIL.getVolatile(this);
			Cell<E> cell = buffer[(int) (t & mask)];
			long seq = (long) CELL_SEQ.getVolatile(cell);

			long diff = seq - t;
			if (diff == 0) {
				if (TAIL.compareAndSet(this, t, t + 1)) {
					CELL_VALUE.setOpaque(cell, e);
					CELL_SEQ.setRelease(cell, t + 1);
					return true;
				}
			} else if (diff < 0) {
				return false; // full
			} else {
				Thread.onSpinWait();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public E poll() {
		while (true) {
			long h = (long) HEAD.getVolatile(this);
			Cell<E> cell = buffer[(int) (h & mask)];
			long seq = (long) CELL_SEQ.getVolatile(cell);

			long expected = h + 1;
			if (seq == expected) {
				if (HEAD.compareAndSet(this, h, h + 1)) {
					Object v = CELL_VALUE.getOpaque(cell);
					CELL_VALUE.setOpaque(cell, null);
					CELL_SEQ.setRelease(cell, h + capacity);
					return (E) v;
				}
			} else if (seq < expected) {
				return null; // empty
			} else {
				Thread.onSpinWait();
			}
		}
	}

	@Override
	public E peek() {
		long h = head;
		Cell<E> cell = buffer[(int) (h & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		return (seq == h + 1) ? (E) CELL_VALUE.getOpaque(cell) : null;
	}

	@Override
	public boolean isEmpty() {
		long h = head;
		Cell<E> cell = buffer[(int) (h & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		return seq != h + 1;
	}

	@Override
	public int size() {
		long h = head;
		long t = tail;
		long diff = t - h;
		return diff <= 0 ? 0 : diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
	}
}

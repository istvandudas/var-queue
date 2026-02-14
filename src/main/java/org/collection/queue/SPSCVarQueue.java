package org.collection.queue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ultra-fast, bounded, array-backed SPSC queue using sequence numbers.
 *
 * - Single producer, single consumer
 * - No CAS anywhere
 * - No spinning
 * - No null checks
 * - No GC
 * - Perfect memory locality
 *
 * This is the fastest queue architecture possible on the JVM.
 */
public final class SPSCVarQueue<E> implements VarQueue<E> {

	// ----------------------------------------------------------------------
	// Padding to avoid false sharing
	// ----------------------------------------------------------------------

	@SuppressWarnings("unused")
	private volatile long p00, p01, p02, p03, p04, p05, p06;
	@SuppressWarnings("unused")
	private volatile long p07, p08, p09, p10, p11, p12, p13;

	private long head;

	@SuppressWarnings("unused")
	private volatile long p14, p15, p16, p17, p18, p19, p20;
	@SuppressWarnings("unused")
	private volatile long p21, p22, p23, p24, p25, p26, p27;

	private long tail;

	@SuppressWarnings("unused")
	private volatile long p28, p29, p30, p31, p32, p33, p34;
	@SuppressWarnings("unused")
	private volatile long p35, p36, p37, p38, p39, p40, p41;

	// ----------------------------------------------------------------------
	// Cell and core fields
	// ----------------------------------------------------------------------

	private static final class Cell<E> {
		volatile long seq;
		E value;

		Cell(long seq) {
			this.seq = seq;
		}
	}

	private final Cell<E>[] buffer;
	private final int mask;
	private final int capacity;

	private static final VarHandle CELL_SEQ;
	private static final VarHandle CELL_VALUE;

	static {
		try {
			MethodHandles.Lookup l = MethodHandles.lookup();
			CELL_SEQ = l.findVarHandle(Cell.class, "seq", long.class);
			CELL_VALUE = l.findVarHandle(Cell.class, "value", Object.class);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// ----------------------------------------------------------------------
	// Construction
	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public SPSCVarQueue(int requestedCapacity) {
		if (requestedCapacity <= 0) {
			throw new IllegalArgumentException("Capacity must be > 0");
		}
		int c = roundToPowerOfTwo(requestedCapacity);
		this.capacity = c;
		this.mask = c - 1;
		this.buffer = (Cell<E>[]) new Cell[c];

		for (int i = 0; i < c; i++) {
			buffer[i] = new Cell<>(i);
		}

		this.head = 0L;
		this.tail = 0L;
	}

	private static int roundToPowerOfTwo(int value) {
		int highest = Integer.highestOneBit(value);
		return (value == highest) ? value : highest << 1;
	}

	// ----------------------------------------------------------------------
	// Public API
	// ----------------------------------------------------------------------

	/**
	 * Offer without CAS — only valid for single producer.
	 */
	public boolean offer(E e) {
		Objects.requireNonNull(e, "element");

		long t = tail;
		Cell<E> cell = buffer[(int) (t & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);

		long diff = seq - t;
		if (diff != 0L) {
			return false; // queue full
		}

		CELL_VALUE.setOpaque(cell, e);
		CELL_SEQ.setRelease(cell, t + 1);
		tail = t + 1;
		return true;
	}

	/**
	 * Poll without CAS — only valid for single consumer.
	 */
	@SuppressWarnings("unchecked")
	public E poll() {
		long h = head;
		Cell<E> cell = buffer[(int) (h & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);

		long expected = h + 1;
		if (seq != expected) {
			return null; // empty
		}

		Object value = CELL_VALUE.getOpaque(cell);
		CELL_VALUE.setOpaque(cell, null);
		CELL_SEQ.setRelease(cell, h + capacity);
		head = h + 1;

		return (E) value;
	}

	@SuppressWarnings("unchecked")
	public E peek() {
		long h = head;
		Cell<E> cell = buffer[(int) (h & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);

		if (seq != h + 1) {
			return null;
		}

		return (E) CELL_VALUE.getOpaque(cell);
	}

	public boolean isEmpty() {
		long h = head;
		Cell<E> cell = buffer[(int) (h & mask)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		return seq != h + 1;
	}

	public int size() {
		long h = head;
		long t = tail;
		long diff = t - h;
		if (diff <= 0) return 0;
		return diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
	}

	public int capacity() {
		return capacity;
	}

	/**
	 * Batch drain for extremely fast consumer loops.
	 */
	public int drain(Consumer<? super E> consumer, int maxItems) {
		Objects.requireNonNull(consumer, "consumer");
		if (maxItems <= 0) return 0;

		int drained = 0;
		while (drained < maxItems) {
			long h = head;
			Cell<E> cell = buffer[(int) (h & mask)];
			long seq = (long) CELL_SEQ.getVolatile(cell);

			if (seq != h + 1) break;

			@SuppressWarnings("unchecked")
			E value = (E) CELL_VALUE.getOpaque(cell);
			CELL_VALUE.setOpaque(cell, null);
			CELL_SEQ.setRelease(cell, h + capacity);
			head = h + 1;

			consumer.accept(value);
			drained++;
		}
		return drained;
	}
}


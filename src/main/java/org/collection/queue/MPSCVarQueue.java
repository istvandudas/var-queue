package org.collection.queue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bounded, array-backed, multi-producer single-consumer (MPSC) queue using
 * sequence numbers per slot (cell) for correct, wait-free publication without
 * spin-waiting on nulls.
 *
 * - Multiple producers: CAS on tail
 * - Single consumer: plain increment on head
 * - Bounded: capacity is fixed, power-of-two
 * - Sequence-based: no publication race, no null spinning
 * - VarHandle-only: no Unsafe
 */
public final class MPSCVarQueue<E> implements VarQueue<E> {

	// ----------------------------------------------------------------------
	// Padding to avoid false sharing around head/tail
	// ----------------------------------------------------------------------

	@SuppressWarnings("unused")
	private volatile long p00, p01, p02, p03, p04, p05, p06;
	@SuppressWarnings("unused")
	private volatile long p07, p08, p09, p10, p11, p12, p13;

	// Consumer index (head)
	private volatile long head;

	@SuppressWarnings("unused")
	private volatile long p14, p15, p16, p17, p18, p19, p20;
	@SuppressWarnings("unused")
	private volatile long p21, p22, p23, p24, p25, p26, p27;

	// Producer index (tail)
	private volatile long tail;

	@SuppressWarnings("unused")
	private volatile long p28, p29, p30, p31, p32, p33, p34;
	@SuppressWarnings("unused")
	private volatile long p35, p36, p37, p38, p39, p40, p41;

	// ----------------------------------------------------------------------
	// Cell and core fields
	// ----------------------------------------------------------------------

	/**
	 * Each cell holds:
	 * - a sequence number (seq)
	 * - the value
	 *
	 * seq encodes the state:
	 * - For producer: cell is free when seq == index
	 * - For consumer: cell is ready when seq == index + 1
	 * After consume, seq is advanced by capacity to mark it free again.
	 */
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

	private static final VarHandle HEAD;
	private static final VarHandle TAIL;
	private static final VarHandle CELL_SEQ;
	private static final VarHandle CELL_VALUE;

	static {
		try {
			MethodHandles.Lookup l = MethodHandles.lookup();
			HEAD = l.findVarHandle(MPSCVarQueue.class, "head", long.class);
			TAIL = l.findVarHandle(MPSCVarQueue.class, "tail", long.class);
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
	public MPSCVarQueue(int requestedCapacity) {
		if (requestedCapacity <= 0) {
			throw new IllegalArgumentException("Capacity must be > 0");
		}
		int c = roundToPowerOfTwo(requestedCapacity);
		this.capacity = c;
		this.mask = c - 1;
		this.buffer = (Cell<E>[]) new Cell[c];

		for (int i = 0; i < c; i++) {
			// Initial seq = index, meaning "free for producer at index"
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

	@Override
	public boolean offer(E e) {
		Objects.requireNonNull(e, "element");

		for (;;) {
			long currentTail = (long) TAIL.getOpaque(this);
			Cell<E> cell = buffer[calcOffset(currentTail)];
			long seq = (long) CELL_SEQ.getVolatile(cell);
			long diff = seq - currentTail;

			if (diff == 0L) {
				// Cell is free for this index, try to claim the slot
				if (TAIL.compareAndSet(this, currentTail, currentTail + 1)) {
					// We own this cell now
					CELL_VALUE.setOpaque(cell, e);
					// Publish: seq = index + 1 (release)
					CELL_SEQ.setRelease(cell, currentTail + 1);
					return true;
				}
				// CAS failed, another producer won, retry
			} else if (diff < 0L) {
				// seq < currentTail => cell not yet recycled => queue is full
				return false;
			} else {
				// seq > currentTail => another producer is ahead, retry
				// (backoff could be added here if needed)
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public E poll() {
		long currentHead = (long) HEAD.getOpaque(this);
		Cell<E> cell = buffer[calcOffset(currentHead)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		long expected = currentHead + 1;

		if (seq != expected) {
			// Not yet published or queue empty
			return null;
		}

		// Read value (opaque is enough once seq matched)
		Object value = CELL_VALUE.getOpaque(cell);
		// Clear value
		CELL_VALUE.setOpaque(cell, null);
		// Mark cell as free for next cycle: seq = head + capacity (release)
		CELL_SEQ.setRelease(cell, currentHead + capacity);
		// Advance head (opaque is enough)
		HEAD.setOpaque(this, currentHead + 1);

		return (E) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E peek() {
		long currentHead = (long) HEAD.getOpaque(this);
		Cell<E> cell = buffer[calcOffset(currentHead)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		long expected = currentHead + 1;

		if (seq != expected) {
			return null;
		}

		return (E) CELL_VALUE.getOpaque(cell);
	}

	@Override
	public boolean isEmpty() {
		long currentHead = (long) HEAD.getOpaque(this);
		Cell<E> cell = buffer[calcOffset(currentHead)];
		long seq = (long) CELL_SEQ.getVolatile(cell);
		long expected = currentHead + 1;
		return seq != expected;
	}

	@Override
	public int size() {
		// Approximate, but good enough for monitoring
		long currentHead = (long) HEAD.getVolatile(this);
		long currentTail = (long) TAIL.getVolatile(this);
		long diff = currentTail - currentHead;
		if (diff <= 0) {
			return 0;
		}
		return diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
	}

	public int capacity() {
		return capacity;
	}

	/**
	 * Batch-drain up to maxItems into the given consumer.
	 * Returns the number of drained elements.
	 */
	public int drain(Consumer<? super E> consumer, int maxItems) {
		Objects.requireNonNull(consumer, "consumer");
		if (maxItems <= 0) return 0;

		int drained = 0;
		while (drained < maxItems) {
			long currentHead = (long) HEAD.getOpaque(this);
			Cell<E> cell = buffer[calcOffset(currentHead)];
			long seq = (long) CELL_SEQ.getVolatile(cell);
			long expected = currentHead + 1;

			if (seq != expected) {
				break; // no more ready elements
			}

			@SuppressWarnings("unchecked")
			E value = (E) CELL_VALUE.getOpaque(cell);
			CELL_VALUE.setOpaque(cell, null);
			CELL_SEQ.setRelease(cell, currentHead + capacity);
			HEAD.setOpaque(this, currentHead + 1);

			consumer.accept(value);
			drained++;
		}
		return drained;
	}

	// ----------------------------------------------------------------------
	// Internal helpers
	// ----------------------------------------------------------------------

	private int calcOffset(long index) {
		return (int) (index & mask);
	}
}

package org.collection.queue.bench.adapter;

/**
 * Constructs {@link QueueAdapter} instances for the queue pattern and
 * implementation chosen by a benchmark's {@code @Param} values.
 *
 * <p>Pattern strings (case-insensitive): {@code spsc}, {@code spmc},
 * {@code mpsc}, {@code mpmc}.
 *
 * <p>Implementation strings:
 * <ul>
 *   <li>{@code varqueue} — this project's VarHandle-based queues.</li>
 *   <li>{@code jctools-vh} — JCTools VarHandle queues ({@code jctools-core-jdk11}).</li>
 *   <li>{@code jctools-unsafe} — JCTools Unsafe queues ({@code jctools-core}).</li>
 *   <li>{@code clq} — unbounded {@link java.util.concurrent.ConcurrentLinkedQueue}
 *       baseline. Only valid for MPMC-shape benchmarks; ignores capacity.</li>
 *   <li>{@code abq} — bounded {@link java.util.concurrent.ArrayBlockingQueue}
 *       baseline.</li>
 * </ul>
 */
public final class QueueFactory {

    private QueueFactory() {
    }

    /**
     * Builds an adapter for the given pattern and implementation.
     *
     * @throws IllegalArgumentException if the combination is unrecognised
     */
    public static <E> QueueAdapter<E> create(String pattern, String impl, int capacity) {
        String p = pattern.toLowerCase();
        String i = impl.toLowerCase();

        switch (i) {
            case "varqueue":
                return switch (p) {
                    case "spsc" -> VarQueueAdapter.spsc(capacity);
                    case "spmc" -> VarQueueAdapter.spmc(capacity);
                    case "mpsc" -> VarQueueAdapter.mpsc(capacity);
                    case "mpmc" -> VarQueueAdapter.mpmc(capacity);
                    default -> throw unknownPattern(p);
                };
            case "jctools-vh":
                return switch (p) {
                    case "spsc" -> JctoolsVhAdapter.spsc(capacity);
                    case "spmc" -> JctoolsVhAdapter.spmc(capacity);
                    case "mpsc" -> JctoolsVhAdapter.mpsc(capacity);
                    case "mpmc" -> JctoolsVhAdapter.mpmc(capacity);
                    default -> throw unknownPattern(p);
                };
            case "jctools-unsafe":
                return switch (p) {
                    case "spsc" -> JctoolsUnsafeAdapter.spsc(capacity);
                    case "spmc" -> JctoolsUnsafeAdapter.spmc(capacity);
                    case "mpsc" -> JctoolsUnsafeAdapter.mpsc(capacity);
                    case "mpmc" -> JctoolsUnsafeAdapter.mpmc(capacity);
                    default -> throw unknownPattern(p);
                };
            case "clq":
                return JucAdapter.clq();
            case "abq":
                return JucAdapter.arrayBlocking(capacity);
            default:
                throw new IllegalArgumentException("Unknown impl: " + impl);
        }
    }

    private static IllegalArgumentException unknownPattern(String p) {
        return new IllegalArgumentException("Unknown pattern: " + p);
    }
}

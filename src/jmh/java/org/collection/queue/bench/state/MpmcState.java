package org.collection.queue.bench.state;

/**
 * MPMC-flavoured queue state. Inherits the default {@code @Param} matrix
 * from {@link QueueState}.
 */
public class MpmcState extends QueueState {

    @Override
    protected String pattern() {
        return "mpmc";
    }
}

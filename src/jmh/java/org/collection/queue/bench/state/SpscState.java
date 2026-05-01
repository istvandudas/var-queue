package org.collection.queue.bench.state;

/**
 * SPSC-flavoured queue state. Inherits the default {@code @Param} matrix
 * from {@link QueueState}. Users can narrow or widen the matrix at run
 * time with {@code -p impl=varqueue,jctools-vh} etc.
 */
public class SpscState extends QueueState {

    @Override
    protected String pattern() {
        return "spsc";
    }
}

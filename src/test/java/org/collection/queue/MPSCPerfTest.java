package org.collection.queue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("perf")
public class MPSCPerfTest extends AbstractPerfTest {

	@Override
	protected int producers() {
		return 4;
	}

	@Override
	protected int consumers() {
		return 1;
	}

	@Test
	void run() throws Exception {
		runAll("MPSC", () -> new MPSCVarQueue<>(CAPACITY));
		runAll("ConcurrentLinkedQueue", ClqAdapter::new);
	}
}

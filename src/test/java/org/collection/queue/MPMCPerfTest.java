package org.collection.queue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("perf")
public class MPMCPerfTest extends AbstractPerfTest {

	@Override
	protected int producers() {
		return 4;
	}

	@Override
	protected int consumers() {
		return 4;
	}

	@Test
	void run() throws Exception {
		runAll("MPMC", () -> new MPMCVarQueue<>(CAPACITY));
		runAll("ConcurrentLinkedQueue", ClqAdapter::new);
	}
}

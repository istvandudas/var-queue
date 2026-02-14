package org.collection.queue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("perf")
public class SPMCPerfTest extends AbstractPerfTest {

	@Override
	protected int producers() {
		return 1;
	}

	@Override
	protected int consumers() {
		return 4;
	}

	@Test
	void run() throws Exception {
		runAll("SPMC", () -> new SPMCVarQueue<>(CAPACITY));
		runAll("ConcurrentLinkedQueue", ClqAdapter::new);
	}
}

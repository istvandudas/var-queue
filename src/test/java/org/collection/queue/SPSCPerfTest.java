package org.collection.queue;

import org.junit.jupiter.api.Test;

public class SPSCPerfTest extends AbstractPerfTest {

	@Override
	protected int producers() {
		return 1;
	}

	@Override
	protected int consumers() {
		return 1;
	}

	@Test
	void run() throws Exception {
		runAll("SPSCVarQueue", () -> new SPSCVarQueue<>(CAPACITY));
		runAll("ConcurrentLinkedQueue", ClqAdapter::new);
	}
}


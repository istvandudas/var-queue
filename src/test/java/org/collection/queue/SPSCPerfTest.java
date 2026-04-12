package org.collection.queue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("perf")
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


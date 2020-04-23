package Usage;

import Scheduler.Action;

import java.util.Random;

public class Operation extends Action<Long, Long> {

	private Random r;
	private boolean log;

	public Operation(String name, long progress, long random, boolean log) {
		super(name, progress);
		this.r = new Random(random);
		this.log = log;
	}

	@Override
	protected Long infoProcessing(Object info) {
		return (Long) ((info == null) ? 0L : info);
	}

	@Override
	protected Long exe(Long info) {
		if (log) {
			appendLog(String.valueOf(info));
		}

		return info + r.nextInt(3) - 1;
	}
}

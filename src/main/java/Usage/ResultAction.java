package Usage;

import Scheduler.Action;

import java.util.concurrent.atomic.AtomicLong;

public class ResultAction extends Action<Long, Long> {

	public static AtomicLong result = new AtomicLong(0);

	protected ResultAction(String name, long progress, long result) {
		super(name, progress);
		ResultAction.result.set(result);
	}

	@Override
	protected Long infoProcessing(Object info) {
		return (long) ((info == null) ? 0L : info);
	}

	@Override
	protected Long exe(Long info) {
		ResultAction.result.set(info);
		return info;
	}
}

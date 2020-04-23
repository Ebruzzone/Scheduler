package Usage;

import Scheduler.Action;
import Scheduler.Joiner;

import java.util.List;
import java.util.Map;

public class JoinerOperations extends Joiner<Long, Long, Object> {

	private String nameSched;

	protected JoinerOperations(String name, long progress, Map<String, List<Action<?, ?>>> actions, String sched) {
		super(name, progress, actions);
		nameSched = sched;
	}

	@Override
	protected Long join(Map<String, Long> elements) {
		long sum = 0;

		for (String name : elements.keySet()) {
			sum += elements.get(name);
		}

		return sum;
	}

	@Override
	protected Object input(String name, Long element) {
		return 0L;
	}

	@Override
	protected Long infoProcessing(Object info) {
		manager.clearLogs(nameSched);


		setParallelPreviousInput(ResultAction.result.get() % 2 == 0);
		setPreviousInput(ResultAction.result.get() % 2 == 0);

		return (Long) ((info == null) ? ResultAction.result.get() : info);
	}
}

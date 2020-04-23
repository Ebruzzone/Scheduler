package Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public abstract class Joiner<T, E, IN> extends Action<T, E> {

	protected Map<String, List<Action<?, ?>>> actions;
	private AtomicBoolean previousInput;
	private Map<String, Scheduler.ParallelExecutor> parallelExecutors;
	private final Object o;

	protected Joiner(String name, long progress, Map<String, List<Action<?, ?>>> actions) {
		super(name, progress);
		this.actions = actions;
		o = new Object();
		previousInput = new AtomicBoolean(false);
	}

	protected Joiner(long progress, Map<String, List<Action<?, ?>>> actions) {
		super(progress);
		this.actions = actions;
		o = new Object();
		previousInput = new AtomicBoolean(false);
	}

	protected abstract T join(Map<String, E> elements);

	protected abstract IN input(String name, E element);

	@Override
	protected final T exe(E info) {

		parallelExecutors = new ConcurrentHashMap<>();
		Map<String, E> elements = new HashMap<>();

		for (String name : actions.keySet()) {
			parallelExecutors.put(name,
					scheduler.newParallelActions(actions.get(name), input(name, info), o, previousInput.get()));
		}

		int flag;

		synchronized (o) {
			while (true) {

				flag = parallelExecutors.size();

				for (String name : parallelExecutors.keySet()) {
					if (parallelExecutors.get(name).notBusy()) {
						flag--;
						elements.put(name, infoProcessing(parallelExecutors.get(name).getObj()));
					}
				}

				for (String name : elements.keySet()) {
					if (parallelExecutors.containsKey(name)) {
						parallelExecutors.get(name).idle.set(true);
						parallelExecutors.remove(name);
					}
				}

				if (flag < 1) {
					break;
				}

				try {
					o.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		elements.put(name, info);

		return join(elements);
	}

	protected final void setParallelPreviousInput(boolean previousInput) {
		this.previousInput.set(previousInput);
	}

	protected final double getProgress(String name) {
		return parallelExecutors.get(name).getProgress();
	}

	protected final String getLogs(String name) {
		return parallelExecutors.get(name).getLogs();
	}
}

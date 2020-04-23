package Scheduler;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class Scheduler implements RunnableAlive {

	private boolean setOrList;
	private boolean killed;
	private Long init;
	private AtomicBoolean alive;
	private AtomicBoolean previousInput;
	private AtomicLong last;
	private ConcurrentLinkedQueue<Action<?, ?>> actions;
	private List<Long> waitMilli;
	private Long followingMilli;
	private Integer startIndex;
	private ConcurrentLinkedQueue<String> logs;
	private AtomicLong times;
	private AtomicLong progress;
	private AtomicReference<Object> obj;
	private ConcurrentLinkedQueue<ParallelExecutor> parallelExecutors;
	private final int idleParallelExecutor;
	private final Object o;

	Scheduler(List<Action<?, ?>> actions, ArrayList<Long> waitMilli, Long init,
			  Integer startIndex, int idleParallelExecutor) {
		this.init = init;
		this.actions = new ConcurrentLinkedQueue<>(actions);
		this.waitMilli = waitMilli;
		this.startIndex = startIndex;
		this.idleParallelExecutor = idleParallelExecutor;
		killed = false;
		o = new Object();
		logs = new ConcurrentLinkedQueue<>();
		times = new AtomicLong(0);
		last = new AtomicLong(0);
		progress = new AtomicLong(0);
		alive = new AtomicBoolean(true);
		previousInput = new AtomicBoolean(false);
		obj = new AtomicReference<>(null);

		if (this.startIndex == null) {
			this.startIndex = 0;
		}

		parallelExecutors = new ConcurrentLinkedQueue<>();

		for (int i = 0; i < idleParallelExecutor; i++) {
			ParallelExecutor parallelExecutor = new ParallelExecutor();
			parallelExecutor.start();
			parallelExecutors.add(parallelExecutor);
		}
	}

	void notifySchedule() {
		synchronized (o) {
			o.notify();
		}
	}

	void notifySchedule(List<Action<?, ?>> actions) {
		synchronized (o) {
			this.actions = new ConcurrentLinkedQueue<>(actions);
			o.notify();
		}
	}

	void setFollowingWaitTime(long milli) {
		synchronized (o) {
			followingMilli = milli;
			setOrList = true;
		}
	}

	private void waitSchedule() {

		try {
			o.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void waitSchedule(int index) {

		index = (index + startIndex) % waitMilli.size();
		long time = waitMilli.get(index) - System.currentTimeMillis() + last.get();

		if (time > 0) {
			try {
				o.wait(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void waitSchedule(long milli) {

		if (milli > 0) {
			try {
				setOrList = false;
				o.wait(milli);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	long getProgress() {
		return progress.get();
	}

	long getIndex() {
		return times.get();
	}

	Object getFinalObject() {
		return obj.get();
	}

	String getLogs() {
		return Arrays.toString(logs.toArray());
	}

	void clearLogs() {
		logs.clear();
	}

	long getLast() {
		return last.get();
	}

	void setPreviousInput(boolean previousInput) {
		this.previousInput.set(previousInput);
	}

	synchronized public void kill() {
		alive.set(false);

		notifySchedule();

		for (ParallelExecutor parallelExecutor : parallelExecutors) {
			parallelExecutor.setAndStart(new LinkedList<>());
		}
	}

	synchronized public void killAndWait() {
		alive.set(false);

		notifySchedule();

		for (ParallelExecutor parallelExecutor : parallelExecutors) {
			parallelExecutor.setAndStart(new LinkedList<>());
		}

		while (parallelExecutors.size() > 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		while (!killed) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {

		last.set(System.currentTimeMillis());

		synchronized (o) {
			if (init != null) {
				waitSchedule(init - last.get());
			} else {
				setOrList = false;
			}
		}

		while (alive.get()) {

			last.set(System.currentTimeMillis());
			progress.set(0);
			times.incrementAndGet();

			if (!previousInput.get()) {
				obj.set(null);
			}

			for (Action<?, ?> action : actions) {
				obj.set(action.execute(obj.get()));
				progress.addAndGet(action.getProgress());
				logs.add(action.getLogs());
			}

			if (parallelExecutors.size() != idleParallelExecutor) {
				if (parallelExecutors.size() > idleParallelExecutor) {
					for (int i = parallelExecutors.size() - idleParallelExecutor; i > 0; i--) {
						Objects.requireNonNull(parallelExecutors.peek()).kill();
					}
				} else {
					for (int i = parallelExecutors.size(); i < idleParallelExecutor; i++) {
						ParallelExecutor p = new ParallelExecutor();
						parallelExecutors.add(p);
						p.start();
					}
				}
			}

			synchronized (o) {

				if (!alive.get()) {
					break;
				}

				if (setOrList) {
					waitSchedule(followingMilli - System.currentTimeMillis() + last.get());
				} else if (waitMilli != null) {
					waitSchedule((int) times.get());
				} else {
					waitSchedule();
				}
			}
		}

		synchronized (this) {
			killed = true;
			this.notify();
		}
	}

	synchronized ParallelExecutor newParallelActions(List<Action<?, ?>> actions, Object inputObject,
													 Object synchronizedObject, boolean previousInput) {
		ParallelExecutor parallelExecutor = null;

		for (ParallelExecutor parallelExecutor1 : parallelExecutors) {
			if (parallelExecutor1.isIdle()) {
				parallelExecutor = parallelExecutor1;
				break;
			}
		}

		if (parallelExecutor == null) {
			parallelExecutor = new ParallelExecutor();
			parallelExecutors.add(parallelExecutor);
			parallelExecutor.start();
		}

		parallelExecutor.setAndStart(actions, inputObject, synchronizedObject, previousInput);

		return parallelExecutor;
	}

	class ParallelExecutor extends Thread {

		private List<Action<?, ?>> actions;
		private ConcurrentLinkedQueue<String> logs;
		private AtomicLong progress;
		private AtomicReference<Object> obj;
		private AtomicReference<Object> o;
		private AtomicBoolean busy;
		AtomicBoolean idle;

		ParallelExecutor() {
			progress = new AtomicLong(0);
			logs = new ConcurrentLinkedQueue<>();
			busy = new AtomicBoolean(false);
			obj = new AtomicReference<>(null);
			o = new AtomicReference<>(new Object());
			idle = new AtomicBoolean(true);
		}

		synchronized private void setAndStart(List<Action<?, ?>> actions,
											  Object input, Object o, boolean previousInput) {
			this.actions = actions;
			this.o.set(o);
			idle.set(false);
			busy.set(true);

			if (!previousInput) {
				obj.set(input);
			}

			this.notify();
		}

		synchronized private void setAndStart(List<Action<?, ?>> actions) {
			this.actions = actions;
			idle.set(false);
			busy.set(true);
			this.notify();
		}

		synchronized private void kill() {
			this.actions = new LinkedList<>();
			idle.set(true);
			busy.set(true);
			this.notify();
		}

		double getProgress() {
			return progress.get();
		}

		String getLogs() {
			return Arrays.toString(logs.toArray());
		}

		boolean notBusy() {
			return !busy.get();
		}

		boolean isIdle() {
			return idle.get();
		}

		Object getObj() {
			return obj.get();
		}

		@Override
		public void run() {

			while (Scheduler.this.alive.get()) {

				synchronized (this) {
					if (!busy.get()) {
						try {
							this.wait();

							if (idle.get()) {
								parallelExecutors.remove(this);
								return;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
							parallelExecutors.remove(this);
							return;
						}
					}

					logs.clear();
					progress.set(0L);

					busy.set(true);

					for (Action<?, ?> action : actions) {
						obj.set(action.execute(obj.get()));
						progress.addAndGet(action.getProgress());
						logs.add(action.getLogs());
					}

					busy.set(false);

					synchronized (o.get()) {
						o.get().notify();
					}
				}
			}

			synchronized (Scheduler.this) {
				parallelExecutors.remove(this);
				Scheduler.this.notify();
			}
		}
	}
}

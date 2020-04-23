package Scheduler;

import java.util.ArrayList;

@SuppressWarnings("unused")
public abstract class Action<T, E> {
	protected String name;
	protected final long progress;
	private StringBuilder logs;
	protected ScheduleManager manager;
	Scheduler scheduler;

	protected Action(String name, long progress) {
		this.name = name;
		this.progress = progress;
		logs = new StringBuilder();
	}

	protected Action(long progress) {
		this.name = "";
		this.progress = progress;
		logs = new StringBuilder();
	}

	void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	void setScheduleManager(ScheduleManager manager) {
		this.manager = manager;
	}

	protected abstract E infoProcessing(Object info);

	protected abstract T exe(E info);

	public synchronized final T execute(Object info) {
		return exe(infoProcessing(info));
	}

	public synchronized final long getProgress() {
		return progress;
	}

	public synchronized final String getLogs() {
		return logs.toString();
	}

	public synchronized final void appendLog(String s) {
		logs.append(s);
	}

	public synchronized final void clearLogs() {
		logs = new StringBuilder();
	}

	protected final void setPreviousInput(boolean previousInput) {
		scheduler.setPreviousInput(previousInput);
	}

	protected final void setFollowingWaitTime(long milli) {
		scheduler.setFollowingWaitTime(milli);
	}

	protected final void notifyAnotherScheduler(String name) {
		manager.notifySchedule(name);
	}

	protected final void notifyAnotherScheduler(String name, ArrayList<Action<?, ?>> actions) {
		manager.notifySchedule(name, actions);
	}

	protected final long lastStartSchedule() {
		return scheduler.getLast();
	}

	protected final Object getSchedulerObject() {
		return scheduler.getFinalObject();
	}
}

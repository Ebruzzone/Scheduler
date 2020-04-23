package Scheduler;

public interface RunnableAlive extends Runnable {
	void kill();
	void killAndWait();
}

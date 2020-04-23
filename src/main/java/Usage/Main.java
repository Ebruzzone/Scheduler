package Usage;

import Scheduler.Action;
import Scheduler.ScheduleManager;
import org.json.JSONObject;

import java.time.Instant;
import java.util.*;

public class Main {

	public static void main(String[] args) {

		Random r = new Random();
		ScheduleManager manager = new ScheduleManager();

		Recurrence appointment = new Recurrence("appointment", 100,
				new String[1], "Appointment", "...");
		List<Action<?, ?>> recurrences = new LinkedList<>();
		recurrences.add(appointment);
		JSONObject json = new JSONObject()
				.put("initDay", "sun")
				.put("hs", new Integer[]{14})
				.put("week", new Integer[]{0})
				.put("weeks", new Integer[]{0});

		manager.addScheduler("RecurrentAppointment", recurrences, json,
				Instant.parse("2020-05-03T14:00:00.00Z").getEpochSecond() * 1000);

		Map<String, List<Action<?, ?>>> map = new HashMap<>();
		List<Action<?, ?>> operations = new LinkedList<>();
		LinkedList<Long> waitMilli = new LinkedList<>();

		for (int i = 0; i < 100; i++) {
			operations.add(new Operation("100", 0,
					r.nextInt(1000000) + System.currentTimeMillis(), false));
		}

		map.put("1", operations);

		operations = new LinkedList<>();
		for (int i = 0; i < 150; i++) {
			operations.add(new Operation("150", 0,
					r.nextInt(1000000) + System.currentTimeMillis(), false));
		}

		map.put("2", operations);

		operations = new LinkedList<>();
		for (int i = 0; i < 50; i++) {
			operations.add(new Operation("50", 0,
					r.nextInt(1000000) + System.currentTimeMillis(), false));
		}

		map.put("3", operations);

		JoinerOperations joinerOperations = new JoinerOperations("Joiner", 90, map, "Scheduler01");
		ResultAction resultAction = new ResultAction("Result", 10, 0L);

		operations = new LinkedList<>();

		operations.add(0, joinerOperations);
		operations.add(1, resultAction);

		waitMilli.addLast(0L);

		manager.addScheduler("Scheduler01", operations, waitMilli, null, 0, 3);

		System.out.println(ResultAction.result.get());
		System.out.println(manager.getTimes("Scheduler01"));

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(ResultAction.result.get());
		long times = manager.getTimes("Scheduler01");
		System.out.println(times);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(ResultAction.result.get());
		System.out.println(manager.getTimes("Scheduler01"));
		ScheduleManager.SchedulerParameters[] schedulerParameters =
				manager.killAndGetParameters(new String[]{"Scheduler01", "RecurrentAppointment"});
		System.out.println(schedulerParameters[0].times + "\n" + (schedulerParameters[0].times - times));
		//manager.kill();
	}
}

package Scheduler;

import org.json.JSONObject;

import java.util.*;

@SuppressWarnings({"unused", "ConstantConditions"})
public class ScheduleManager {

	private Map<String, Scheduler> schedulers;

	public ScheduleManager() {
		schedulers = new HashMap<>();
	}

	public void kill() {
		for (String name : schedulers.keySet()) {
			schedulers.get(name).kill();
		}
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, List<Long> waitMilli,
							 Long init, Integer startIndex) {

		addSched(name, actions, waitMilli, init, startIndex, 0);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, String[] times,
							 Long init, Integer startIndex) {

		LinkedList<Long> waitMilli = new LinkedList<>();

		for (String s : times) {
			addNewWaitTime(waitMilli, s);
		}

		addSched(name, actions, waitMilli, init, startIndex, 0);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, JSONObject options, Long init) {

		Map<Integer, LinkedList<Long>> map = newRoutine(options, init);
		int index = map.keySet().toArray(new Integer[0])[0];

		addSched(name, actions, map.get(index), init, index, 0);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, List<Long> waitMilli,
							 Long init, Integer startIndex, int idleParallelSchedule) {

		addSched(name, actions, waitMilli, init, startIndex, idleParallelSchedule);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions,
							 Long init, Integer startIndex, int idleParallelSchedule) {

		addSched(name, actions, null, init, startIndex, idleParallelSchedule);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, Long init) {

		addSched(name, actions, null, init, null, 0);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, Long init, int idleParallelSchedule) {

		addSched(name, actions, null, init, null, idleParallelSchedule);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, String[] times,
							 Long init, Integer startIndex, int idleParallelSchedule) {

		LinkedList<Long> waitMilli = new LinkedList<>();

		for (String s : times) {
			addNewWaitTime(waitMilli, s);
		}

		addSched(name, actions, waitMilli, init, startIndex, idleParallelSchedule);
	}

	public void addScheduler(String name, List<Action<?, ?>> actions, JSONObject options,
							 Long init, int idleParallelSchedule) {

		Map<Integer, LinkedList<Long>> map = newRoutine(options, init);
		int index = map.keySet().toArray(new Integer[0])[0];

		addSched(name, actions, map.get(index), init, index, idleParallelSchedule);
	}

	private void addSched(String name, List<Action<?, ?>> actions, List<Long> waitMilli,
						  Long init, Integer startIndex, int idleParallelSchedule) {

		Scheduler scheduler = new Scheduler(actions,
				waitMilli == null ? null : new ArrayList<>(waitMilli), init, startIndex, idleParallelSchedule);

		for (Action<?, ?> action : actions) {
			action.setScheduler(scheduler);
			action.setScheduleManager(this);
		}

		schedulers.put(name, scheduler);

		new Thread(scheduler).start();
	}

	public void setPreviousInput(String name, boolean previousInput) {
		if (schedulers.containsKey(name)) {
			schedulers.get(name).setPreviousInput(previousInput);
		}
	}

	public void notifySchedule(String name) {
		if (schedulers.containsKey(name)) {
			schedulers.get(name).notifySchedule();
		}
	}

	public void notifySchedule(String name, ArrayList<Action<?, ?>> actions) {
		if (schedulers.containsKey(name)) {
			schedulers.get(name).notifySchedule(actions);
		}
	}

	public void killSchedule(String name) {
		if (schedulers.containsKey(name)) {
			schedulers.remove(name).kill();
		}
	}

	public double getProgress(String name) {
		if (schedulers.containsKey(name)) {
			return schedulers.get(name).getProgress();
		}

		return -1;
	}

	public String getLogs(String name) {
		if (schedulers.containsKey(name)) {
			return schedulers.get(name).getLogs();
		}

		return null;
	}

	public void clearLogs(String name) {
		if (schedulers.containsKey(name)) {
			schedulers.get(name).clearLogs();
		}
	}

	public long getTimes(String name) {
		if (schedulers.containsKey(name)) {
			return schedulers.get(name).getIndex();
		}

		return 0;
	}

	public Object getSchedulerObject(String name) {
		if (schedulers.containsKey(name)) {
			return schedulers.get(name).getFinalObject();
		}

		return null;
	}

	public SchedulerParameters killAndGetParameters(String name) {
		if (schedulers.containsKey(name)) {

			schedulers.get(name).killAndWait();

			SchedulerParameters schedulerParameters = new SchedulerParameters();
			schedulerParameters.logs = schedulers.get(name).getLogs();
			schedulerParameters.progress = schedulers.get(name).getProgress();
			schedulerParameters.times = schedulers.get(name).getIndex();
			schedulerParameters.lastTimestamp = schedulers.get(name).getLast();
			schedulerParameters.object = schedulers.remove(name).getFinalObject();
			return schedulerParameters;
		}

		return null;
	}

	public SchedulerParameters[] killAndGetParameters(String[] names) {

		List<Killer> killers = new LinkedList<>();
		SchedulerParameters[] schedulerParameters = new SchedulerParameters[names.length];
		int i = 0;

		for (String name : names) {
			killers.add(new Killer(name));
		}

		while (i < names.length) {
			schedulerParameters[i] = killers.get(i).getSchedulerParameters();
			i++;
		}

		return schedulerParameters;
	}

	private class Killer extends Thread {
		private String name;
		private SchedulerParameters schedulerParameters;
		private boolean end;

		private Killer(String name) {
			this.name = name;
			schedulerParameters = null;
			end = false;
			this.start();
		}

		private synchronized SchedulerParameters getSchedulerParameters() {
			if (!end) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return schedulerParameters;
		}

		@Override
		public void run() {
			schedulerParameters = ScheduleManager.this.killAndGetParameters(name);

			synchronized (this) {
				end = true;
				this.notify();
			}
		}
	}

	public static class SchedulerParameters {
		public String logs;
		public long progress;
		public long times;
		public long lastTimestamp;
		public Object object;
	}

	/************************************************************************************
	 * The string time must have the following features:								*
	 *  - sequence of number and letter with spaces										*
	 *  - F is 4 years = 365,25 * 4 days = 126230400000‬ milliseconds					*
	 *  - Y is a year = 31556952000 milliseconds										*
	 *  - y is a year of 365,25 days													*
	 *  - M is a month of 30 days														*
	 *  - W is a week of 7 days															*
	 *  - w is a quarter week															*
	 *  - D is a day																	*
	 *  - d is a quarter day															*
	 *  - H is an hour																	*
	 *  - h is a quarter hour															*
	 *  - m is a minute																	*
	 *  - S is a second																	*
	 *  - s is a millisecond															*
	 *																					*
	 *  Examples:																		*
	 *  1Y 1M 3w -1d 22222s = one year and one month and 3/4 week						*
	 *  less 1/4 day and 22 seconds and 222 milliseconds = 34580974222 milliseconds		*
	 *																					*
	 *  Attention: each number must be an integer negative or positive					*
	 *																					*
	 * @param waits previous list														*
	 * @param time  string with some features to extract the following wait time		*
	 ************************************************************************************/
	public static void addNewWaitTime(LinkedList<Long> waits, String time) {

		int posY = time.indexOf("Y"), posy = time.indexOf("y"), posM = time.indexOf("M"),
				posW = time.indexOf("W"), posw = time.indexOf("w"), posD = time.indexOf("D"),
				posd = time.indexOf("d"), posH = time.indexOf("H"), posh = time.indexOf("h"),
				posm = time.indexOf("m"), posS = time.indexOf("S"), poss = time.indexOf("s"),
				space, posF = time.indexOf("F");

		long milli = 0;

		if (posF != -1) {
			space = time.substring(0, posF).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posF)) * 126230400000L;
		}

		if (posY != -1) {
			space = time.substring(0, posY).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posY)) * 31556952000L;
		}

		if (posy != -1) {
			space = time.substring(0, posy).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posy)) * 31557600000L;
		}

		if (posM != -1) {
			space = time.substring(0, posM).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posM)) * 2592000000L;
		}

		if (posW != -1) {
			space = time.substring(0, posW).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posW)) * 604800000L;
		}

		if (posw != -1) {
			space = time.substring(0, posw).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posw)) * 151200000L;
		}

		if (posD != -1) {
			space = time.substring(0, posD).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posD)) * 86400000L;
		}

		if (posd != -1) {
			space = time.substring(0, posd).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posd)) * 21600000L;
		}

		if (posH != -1) {
			space = time.substring(0, posH).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posH)) * 3600000L;
		}

		if (posh != -1) {
			space = time.substring(0, posh).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posh)) * 900000L;
		}

		if (posm != -1) {
			space = time.substring(0, posm).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posm)) * 60000L;
		}

		if (posS != -1) {
			space = time.substring(0, posS).lastIndexOf(" ") + 1;
			milli += Integer.parseInt(time.substring(space, posS)) * 1000L;
		}

		if (poss != -1) {
			space = time.substring(0, poss).lastIndexOf(" ") + 1;
			milli += Long.parseLong(time.substring(space, poss));
		}

		waits.addLast(milli);
	}

	/************************************************************************************
	 * The json options must have the following features:								*
	 *  - "initDay" initial day of the week, default is "mon"							*
	 *  - array "months" with which months of the year									*
	 *  - json "month" with which months and which days in an array						*
	 *  - array "weeks" with which weeks of the month, default all						*
	 *  - array "week" with which days of the week 										*
	 *  - json "days" with which days in an array and with the int "period" of days		*
	 *  - array "hs" with which hours of the days										*
	 *  - array "mins" with which hours of the days										*
	 *  - array "secs" with which hours of the days										*
	 *  - array "millis" with which milliseconds of the second with format "sss"		*
	 *  																				*
	 * Each row is a bond, without all of they the response will be 1 so a wait time	*
	 * of a millisecond forever.														*
	 *																					*
	 * Example (No Run):																*
	 *  {																				*
	 *  	"initDay": "sun",															*
	 *      "months": [1, 10, 12],			-- range 1, 12 --							*
	 *      "month": {																	*
	 *          "1": [1, 6, 7, 8, 31],		-- range 1, 12 --							*
	 *          "4": [10, 20, 30]			-- range 1, 31 --							*
	 *      },																			*
	 *      "weeks": [0, 1, 3],				-- range 0, 3 --							*
	 *      "week": [0, 2, 4, 6],			-- range 0, 6 --							*
	 *      "days": {																	*
	 *          "d": [0, 2, 4, 6],			-- range 0, period - 1 --					*
	 *          "period": 7																*
	 *      },																			*
	 *      "hs": [0, 15, 23],				-- range 0, 23 --							*
	 *      "mins": [1, 59],				-- range 0, 59 --							*
	 *      "secs": [1, 21, 41],			-- range 0, 59 --							*
	 *      "millis": [0, 125, 250, 750]	-- range 0, 999 --							*
	 *  }																				*
	 *																					*
	 *  It is preferable not to use millis												*
	 *																					*
	 * @param options       json with some features to build a routine					*
	 * @param initTimestamp Long with the timestamp of when the schedule will start;   	*
	 *                      if it is < of now or = null, will set to now timestamp		*
	 * @return list of wait times for scheduling with sundry patterns					*
	 ************************************************************************************/
	public static Map<Integer, LinkedList<Long>> newRoutine(JSONObject options, Long initTimestamp) {

		if (initTimestamp == null || initTimestamp < System.currentTimeMillis()) {
			throw new IllegalArgumentException("initTimestamp must be in the future, " +
					"it is the first time when the scheduler will start.");
		}

		int verify = 0, index = 0;
		verify = options.has("month") ? verify + 1 : verify;
		verify = options.has("months") ? verify + 1 : verify;
		verify = options.has("days") ? verify + 1 : verify;

		if (verify > 1) {
			throw new IllegalArgumentException("options must have only one of days, month and months.");
		}

		verify = 0;
		verify = options.has("month") ? verify + 1 : verify;
		verify = options.has("days") ? verify + 1 : verify;
		verify = options.has("week") ? verify + 1 : verify;

		if (verify > 1) {
			throw new IllegalArgumentException("options must have only one of month, days and week.");
		}

		if (options.has("weeks") && !options.has("week")) {
			throw new IllegalArgumentException("If there is weeks in options, there must be week too.");
		}

		LinkedList<Long> routine = new LinkedList<>();
		Map<Integer, LinkedList<Long>> res = new HashMap<>();

		Integer[] months = null, weeks = null, week = null,
				hs = null, mins = null, secs = null, millis = null;
		JSONObject month = null, days = null;
		int initDay = 7;

		if (options.has("initDay")) {
			switch (options.getString("initDay")) {
				case "tue":
					initDay = 1;
					break;
				case "wed":
					initDay = 2;
					break;
				case "thu":
					initDay = 3;
					break;
				case "fri":
					initDay = 4;
					break;
				case "sat":
					initDay = 5;
					break;
				case "sun":
					initDay = 6;
					break;
				default:
			}
		}

		if (options.has("months")) {
			months = (Integer[]) options.get("months");

			Arrays.sort(months);

			if (months[0] < 1 || months[months.length - 1] > 12) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("month")) {
			month = options.getJSONObject("month");
		}

		if (options.has("weeks")) {
			weeks = (Integer[]) options.get("weeks");

			Arrays.sort(weeks);

			if (weeks[0] < 0 || weeks[weeks.length - 1] > 3) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("week")) {
			week = (Integer[]) options.get("week");

			Arrays.sort(week);

			if (week[0] < 0 || week[week.length - 1] > 6) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("days")) {
			days = options.getJSONObject("days");
		}

		if (options.has("hs")) {
			hs = (Integer[]) options.get("hs");

			Arrays.sort(hs);

			if (hs[0] < 0 || hs[hs.length - 1] > 23) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("mins")) {
			mins = (Integer[]) options.get("mins");

			Arrays.sort(mins);

			if (mins[0] < 0 || mins[mins.length - 1] > 59) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("secs")) {
			secs = (Integer[]) options.get("secs");

			Arrays.sort(secs);

			if (secs[0] < 0 || secs[secs.length - 1] > 59) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		if (options.has("millis")) {
			millis = (Integer[]) options.get("millis");

			Arrays.sort(millis);

			if (millis[0] < 0 || millis[millis.length - 1] > 999) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}
		}

		res.put(0, routine);

		if (millis != null) {

			res = processArray(millis, res, initTimestamp, 1L, 1000, 1L);
			index = (int) res.keySet().toArray()[0];
			routine = res.get(index);
		}

		if (secs != null) {

			Long last = 1000L;
			if (routine.size() > 0) {
				last = routine.removeLast();
			}

			res = processArray(secs, res, initTimestamp, 1000L, 60, last);
			index = (int) res.keySet().toArray()[0];
			routine = res.get(index);
		}

		if (mins != null) {

			Long last = 60000L;
			if (routine.size() > 0) {
				last = routine.removeLast();

				if (last < 1000) {
					last += 59000;
				}
			}

			res = processArray(mins, res, initTimestamp, 60000L, 60, last);
			index = (int) res.keySet().toArray()[0];
			routine = res.get(index);
		}

		if (hs != null) {

			Long last = 3600000L;
			if (routine.size() > 0) {
				last = routine.removeLast();

				if (last < 1000) {
					last += 3599000L;
				} else if (last < 60000) {
					last += 3540000L;
				}
			}

			res = processArray(hs, res, initTimestamp, 3600000L, 24, last);
			index = (int) res.keySet().toArray()[0];
			routine = res.get(index);
		}

		if (days != null) {
			Integer[] array = (Integer[]) days.get("d");
			int period = days.getInt("period");

			Arrays.sort(array);

			if (array[0] < 0 || array[array.length - 1] >= period) {
				throw new IllegalArgumentException("The arrays must have the specified range.");
			}

			Long last = 86400000L;
			if (routine.size() > 0) {
				last = routine.removeLast();

				if (last < 1000) {
					last += 86399000L;
				} else if (last < 60000) {
					last += 86340000L;
				} else if (last < 3600000) {
					last += 82800000L;
				}
			}

			res = processArray(array, res, initTimestamp, 86400000L, period, last);
			index = (int) res.keySet().toArray()[0];
			routine = res.get(index);
		}

		if (week != null) {

			int init = ((int) ((initTimestamp % 604800000L) / 86400000L) + 3) % 7;

			if (week.length > 1) {

				boolean flag = false;

				for (int i = 0; i < week.length; i++) {
					if (init == ((week[i] + initDay) % 7)) {
						index += (i * Math.max(routine.size(), 1));
						flag = false;
						break;
					} else {
						flag = true;
					}
				}

				if (flag) {
					throw new IllegalArgumentException("initTimestamp must be the first timestamp of the scheduling.");
				}

				Long last = 86400000L;
				LinkedList<Long> r1;
				if (routine.size() > 0) {
					last = routine.removeLast();
					r1 = new LinkedList<>(routine);

					if (last < 1000) {
						last += 86399000L;
					} else if (last < 60000) {
						last += 86340000L;
					} else if (last < 3600000) {
						last += 82800000L;
					}
				} else {
					r1 = new LinkedList<>();
				}

				for (int i = 1; i < week.length; i++) {
					routine.addLast((week[i] - week[i - 1]) * 86400000 - 86400000 + last);
					routine.addAll(r1);
				}

				routine.addLast((7 + week[0] - week[week.length - 1]) * 86400000 - 86400000 + last);

			} else {

				if (init != ((week[0] + initDay) % 7)) {
					throw new IllegalArgumentException("initTimestamp must be the first timestamp of the scheduling.");
				}

				routine.addLast(604800000L);
			}

			res.clear();
			res.put(index, routine);
		}

		LinkedList<Integer> ds;

		int period = 365 * 4 + 1;

		if (months != null || weeks != null) {

			int bis = 2, per = 4, per2 = 400, bis2 = 370, last1 = 5, lw = 7, lm = 12, nbis = 100, diff = 0;
			int[][][] s = new int[per2][lm][];
			int[] m = new int[]{31, 0, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
			period = (365 * 400 + 97) / 7;

			last1 = (last1 + initDay) % lw;
			last1 = last1 == 0 ? lw : last1;

			for (int i = 0; i < per2; i++) {

				if ((i + bis2) % per2 == 0 || (i % per == bis && (i + bis2) % nbis != 0)) {
					m[1] = 29;
				} else {
					m[1] = 28;
				}

				for (int j = 0; j < lm; j++) {

					s[i][j] = new int[(m[j] - last1) / lw + 1];

					for (int k = 0; ; k++) {

						s[i][j][k] = diff;

						last1 += lw;
						diff += lw;

						if (last1 > m[j]) {
							last1 = (last1 % m[j]);
							break;
						}
					}
				}
			}

			if (week != null) {
				for (int i = 0; i < per2; i++) {
					for (int j = 0; j < s[i].length; j++) {
						int[] n = new int[s[i][j].length * week.length];

						for (int k = 0; k < n.length; k++) {
							n[k] = s[i][j][k % s[i][j].length] + week[k % week.length];
						}

						s[i][j] = n;
					}
				}
			}

			ds = new LinkedList<>();

			if (weeks == null) {
				weeks = new Integer[]{0};
			}

			for (int i = 0; i < per2; i++) {
				for (int j = 0; j < s[i].length; j++) {
					if (months == null || Arrays.binarySearch(months, j + 1) > -1) {
						for (Integer integer : weeks) {
							ds.addLast(s[i][j][integer] / 7);
						}
					}
				}
			}

			Long last = 86400000L;
			if (routine.size() > 0) {
				last = routine.removeLast();

				if (last < 1000) {
					last += 86399000L;
				} else if (last < 60000) {
					last += 86340000L;
				} else if (last < 3600000) {
					last += 82800000L;
				}
			}

			res = processArray(ds.toArray(new Integer[0]), res,
					initTimestamp - 86400000L * ((4 + initDay) % 7), 604800000L, period, last);
		} else if (month != null) {

			ds = new LinkedList<>();

			int[] n = new int[]{0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
					31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
					31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
					31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
			Integer[] d;

			for (int i = 1; i < n.length; i++) {
				n[i] = n[i] + n[i - 1];
			}

			for (int y = 0; y < 4; y++) {
				for (int i = 1; i < 13; i++) {
					if (month.has(String.valueOf(i))) {
						d = (Integer[]) month.get(String.valueOf(i));

						for (int value : d) {
							ds.addLast(n[i + y * 12 - 1] + value - 1);
						}
					}
				}
			}

			Long last = 86400000L;
			if (routine.size() > 0) {
				last = routine.removeLast();

				if (last < 1000) {
					last += 86399000L;
				} else if (last < 60000) {
					last += 86340000L;
				} else if (last < 3600000) {
					last += 82800000L;
				}
			}

			res = processArray(ds.toArray(new Integer[0]), res, initTimestamp, 86400000L, period, last);
		}

		return res;
	}

	private static Map<Integer, LinkedList<Long>> processArray(Integer[] array, Map<Integer, LinkedList<Long>> routine,
															   long initTimestamp, long milli, int period, Long last) {

		int index = (int) routine.keySet().toArray()[0];
		LinkedList<Long> routine1 = routine.get(index), r1 = new LinkedList<>(routine1);

		if (array.length > 1) {
			long init = (initTimestamp % (milli * period)) / milli;
			boolean flag = false;

			for (int i = 0; i < array.length; i++) {
				if (init == array[i]) {
					index += (i * (routine1.size() + 1));
					flag = false;
					break;
				} else {
					flag = true;
				}
			}

			if (flag) {
				throw new IllegalArgumentException("initTimestamp must be the first timestamp of the scheduling.");
			}

			for (int i = 1; i < array.length; i++) {
				routine1.addLast((array[i] - array[i - 1]) * milli - milli + last);
				routine1.addAll(r1);
			}

			routine1.addLast((period + array[0] - array[array.length - 1]) * milli - milli + last);

		} else {
			routine1.addLast(milli * period);
		}

		Map<Integer, LinkedList<Long>> res = new HashMap<>();
		res.put(index, routine1);
		return res;
	}
}

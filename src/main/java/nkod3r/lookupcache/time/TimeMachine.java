package nkod3r.lookupcache.time;


import nkod3r.lookupcache.logger.KLogger;
import nkod3r.lookupcache.lookup.LocalLookup;
import nkod3r.lookupcache.lookup.LookupConsumer;
import nkod3r.lookupcache.time.events.TimeLoopEvent;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static nkod3r.lookupcache.time.TimeLane.TimeLanePeriod.*;


/**
 * create timestamp on each call.
 * increment defines each steps width in milliseconds
 */
public class TimeMachine implements LookupConsumer {
    public enum TimeMode{
        SIMULATED, REAL
    }

    public static final long DEFAULT_SIM_START_MILLIS = 1453640400000L;

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = MINUTE * 60;
    public static final long DAY = HOUR * 24;
    public static final long YEAR = DAY * 365;
    public static final long MONTH = DAY * 30;
    public static final long WEEK = DAY * 7;
    private static final long DEFAULT_INCREMENT = 0;

    private static KLogger log = new KLogger(TimeMachine.class);

    private long start;
    private long end;
    private long increment;
    private long currentTimeMillis;
    private ConcurrentHashMap<String, TimeMachineTask> tasks;
    private ScheduledExecutorService timer;
    private boolean freezeTime;
    private TimeLane hyperLane;
    private TimeLane fastLane;
    private TimeLane standardLane;
    private TimeLane slowLane;
    private List<TimeLane> lanes;

    private List<Runnable> listeners;
    private TimeMode timeMode;
    public LocalLookup lookup;

    public TimeMachine(LocalLookup lookup) {
        this.lookup=lookup;
        freezeTime = false;
        start = System.currentTimeMillis();
        end = System.currentTimeMillis();
        currentTimeMillis = start;
        increment = DEFAULT_INCREMENT;
        timeMode= TimeMode.REAL;
        timer = Executors.newSingleThreadScheduledExecutor();
        tasks = new ConcurrentHashMap<>();
        listeners = new ArrayList<>();
        lanes=new ArrayList<>();
    }

    public void initTimeLanes() {
        hyperLane = new TimeLane(this,"hyperLane", HyperLane);
        fastLane = new TimeLane(this,"fastLane", FastLane);
        standardLane = new TimeLane(this,"standardLane", StandardLane);
        slowLane = new TimeLane(this,"slowLane", SlowLane);
        scheduleTimeLane(hyperLane);
        scheduleTimeLane(fastLane);
        scheduleTimeLane(standardLane);
        scheduleTimeLane(slowLane);
    }


    public long currentTimeMillis() {
        return currentTimeMillis(increment);
    }

    public long peekCurrentTimeMillis() {
        return currentTimeMillis;
    }

    public long currentTimeMillis(long increment) {
        if (freezeTime) {
            return currentTimeMillis;
        }
        if (timeMode.equals(TimeMode.SIMULATED)) {
            if (currentTimeMillis >= end) {
                notifyOnTimeLoop(new TimeLoopEvent());
                return currentTimeMillis;
            }
            currentTimeMillis += increment;
            return currentTimeMillis;
        }
        currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis;
    }

    private Future scheduleTimeLane(TimeLane lane) {
        ScheduledFuture<?> future = timer.scheduleAtFixedRate(
                lane,
                lane.delay,
                lane.period.value,
                TimeUnit.MILLISECONDS);
        lane.setFuture(future);
        lanes.add(lane);
        return future;
    }

    public void schedule(TimeMachineTask task) {
        if (tasks.containsValue(task)) {
//            log.debug("Task '" + task.ident + "' already scheduled.");
            task.activate();
            return;
        }
        tasks.put(task.ident, task);
        log.debug("scheduled task:", task);
    }

    public void stop(String name) {
        if (!tasks.containsKey(name)) {
            return;
        }
        tasks.get(name).cancel();
    }

    public void stop(TimeMachineTask task) {
        if (!tasks.containsKey(task.ident)) {
            return;
        }
        tasks.get(task.ident).cancel();
    }

    public void stopAll() {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            item.getValue().cancel();
        }
    }

    public void stopAllBut(TimeMachineTask task) {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            if (item.getValue().ident.equals(task.ident)) {
                continue;
            }
            item.getValue().cancel();
        }
    }

    public void stopAllPrefixed(String prefix) {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            if (item.getKey().startsWith(prefix)) {
                item.getValue().cancel();
            }
        }
    }

    public boolean restartTask(String prefix, long delay, long period) {
        boolean scheduledSomething = false;
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            if (item.getKey().startsWith(prefix)) {
                schedule(item.getValue());
                scheduledSomething = true;
            }
        }
        return scheduledSomething;
    }

    public void startAllBut(TimeMachineTask... exception) {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            TimeMachineTask task = item.getValue();
            for (int i = 0; i < exception.length; i++) {
                if (task.ident.equals(exception[i].ident)) {
                    continue;
                }
                task.activate();
            }
        }
    }

    public void runAllBut(TimeMachineTask... exception) {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            TimeMachineTask task = item.getValue();
            for (int i = 0; i < exception.length; i++) {
                if (task.ident.equals(exception[i].ident)) {
                    continue;
                }
                task.run();
            }
        }
    }

    public void runAll() {
        for (Map.Entry<String, TimeMachineTask> item : tasks.entrySet()) {
            TimeMachineTask task = item.getValue();
            task.run();
        }
    }

    public String getRFCTimeString() {
        return getRFCTimeString(currentTimeMillis);
    }

    public String getRFCTimeString(long timeMillis) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = new Date(timeMillis);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    public String getTimeString() {
        String pattern = "HH:mm:ss yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date;
        if (freezeTime) {
            date = new Date(currentTimeMillis);
        } else {
            date = new Date(currentTimeMillis());
        }
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    public String getMilliTimeString() {
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = new Date(currentTimeMillis());
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }
    public String getTimeStringWithSlashes(long timestamp) {
        String pattern = "MM/dd/yyyy HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = new Date(timestamp);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    public String getHMSTimeString() {
        return getHMSTimeString(currentTimeMillis());
    }

    public String getHMSTimeString(long stamp) {
        String pattern = "HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = new Date(stamp);
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    public String doubleTimeToRFC(double time) {
        final Date timestamp = new Date((long) (time * 1000L));
        return getRFCTimeString(timestamp.getTime())+"+02:00";
    }

    public long parseTime(String timeString){
        //2018-10-17T20:59:25+02:00
        Instant instant = Instant.parse(timeString);
        long millis = instant.toEpochMilli();
        return millis;
    }

    public void freeze() {
        freezeTime = true;
    }

    public void unfreeze() {
        freezeTime = false;
    }

    public ConcurrentHashMap<String, TimeMachineTask> getTasks() {
        return tasks;
    }

    public long getStart() {
        return start;
    }

    public long getIncrement() {
        return increment;
    }

    public void setStart(long start) {
        this.start = start;
        this.currentTimeMillis = start;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getEnd() {
        return end;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public void notifyOnTimeLoop(TimeLoopEvent timeLoopEvent) {
        listeners.forEach(l -> l.run());
    }

    public void setOnTimeLoopListener(Runnable listener) {
        listeners.add(listener);
    }

    public List<TimeLane> getLanes() {
        return lanes;
    }

    public TimeMode getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(TimeMode timeMode) {
        this.timeMode = timeMode;
    }

    @Override
    public <T> T lookup(Class<T> clazz) {
        return lookup.get(clazz);
    }
}

package nkod3r.lookupcache.time;


import nkod3r.lookupcache.logger.KLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

public class TimeLane extends TimeMachineTask {
    private final TimeMachine timeMachine;
    private KLogger log = new KLogger(TimeLane.class);

    public enum TimeLanePeriod {
        HyperLane(100), FastLane(1000), StandardLane(60000), SlowLane(100000);
        long value;

        TimeLanePeriod(long value) {
            this.value = value;
        }
    }

    private Future future;

    public TimeLane(TimeMachine timeMachine, String ident, TimeLanePeriod period) {
        super(ident, period);
        this.timeMachine = timeMachine;
        delay = 3000;
    }

    @Override
    public void run() {
        TimeMachineTask task;
        ConcurrentHashMap<String, TimeMachineTask> tasks = timeMachine.getTasks();
        for (Map.Entry<String, TimeMachineTask> entry : tasks.entrySet()) {
            task = entry.getValue();
            if (task.period != period || task.isCancelled() || task.isRunning()) {
//                log.debug("skipping task due to other running task. Dont want to spam.");
                continue;
            }
            if (task.period.value != TimeLanePeriod.HyperLane.value && task.period.value != TimeLanePeriod.FastLane.value) {
                log.debug("running task: " + task.ident);
            }
            task.setRunning(true);
            try {
                task.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
            task.setRunning(false);
        }
    }

    public void setFuture(ScheduledFuture future) {
        this.future = future;
    }
}

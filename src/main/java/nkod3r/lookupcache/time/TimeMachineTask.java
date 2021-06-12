package nkod3r.lookupcache.time;

public abstract class TimeMachineTask implements Runnable{
    public String ident;
    public long delay;
    public TimeLane.TimeLanePeriod period;
    public boolean cancelled=false;
    private boolean running=false;

    public TimeMachineTask(String ident, TimeLane.TimeLanePeriod period) {
        this.ident = ident;
        this.delay = 1000;
        this.period = period;
    }

    public void cancel(){
        cancelled=true;
    }

    public void activate(){
        cancelled=false;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public String toString() {
        return "TimeMachineTask{" +
                "ident='" + ident + '\'' +
                ", delay=" + delay +
                ", period=" + period +
                ", cancelled=" + cancelled +
                '}';
    }

    protected boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

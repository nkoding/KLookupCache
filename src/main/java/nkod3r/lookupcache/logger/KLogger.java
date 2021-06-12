package nkod3r.lookupcache.logger;

import nkod3r.lookupcache.lookup.Lookup;
import nkod3r.lookupcache.time.TimeMachine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class KLogger {
    private static List<LogAppender> appenders = new ArrayList<>();
    private static boolean silent;
    private static StringBuilder buf=new StringBuilder();
    private boolean doOutput=true;

    public enum LogLevel{
        FINE, DEBUG, ERROR
    }

    static {
        addAppender(new StdOutLogAppender());
    }

    private static TimeMachine timeMachine;
    private Class context;

    public KLogger(Class context) {
        this.context = context;
    }

    public static void addAppender(LogAppender appender) {
        appenders.add(appender);
    }


    public void debugStackTrace(String msg, int stackTraceDepth, Object... objects) {
        StringBuilder buf=new StringBuilder();
        buf.append(msg).append("\n");
        try{
            throw new Exception("testErr");
        } catch (Exception e){
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0, stackTraceLength = stackTrace.length; i < stackTraceLength && i<stackTraceDepth ; i++) {
                StackTraceElement elem = stackTrace[i];
                buf.append(elem.toString()).append("\n");
            }
        }
        debug(buf.toString(), objects);
    }

    public void debug(String msg, Object... objects) {
        if(!doOutput){
            return;
        }
        if(timeMachine==null){
            timeMachine = new TimeMachine(Lookup.newInstance());
        }
        StringBuilder message = new StringBuilder();
        message.append(timeMachine.getMilliTimeString())
                .append(" DEBUG [").append(context.getSimpleName()).append("]  ")
                .append(msg).append('\t');
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            message.append(obj.toString());
            if (i < objects.length - 1) {
                message.append('\n');
            }
        }
        message.append('\n');
        if(silent){
            buf.append(message);
        } else {
            for (LogAppender appender : appenders) {
                appender.info(message.toString());
            }
        }
    }

    public void error(Exception ex) {
        StringBuilder errMsg=new StringBuilder();
        StringWriter stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));
        if(!doOutput){
            return;
        }
        if(timeMachine==null){
            timeMachine = new TimeMachine(Lookup.newInstance());
        }
        errMsg.append(timeMachine.getMilliTimeString())
                .append(" DEBUG [")
                .append(context.getSimpleName())
                .append("]  ")
                .append(ex.getMessage())
                .append('\n')
                .append(stackTrace.toString());
        errMsg.append('\n');
        if(silent){
            buf.append(errMsg);
        } else {
            for (LogAppender appender : appenders) {
                appender.error(errMsg.toString());
            }
        }
    }

    public void error(String msg) {
        StringBuilder errMsg=new StringBuilder();
        if(!doOutput){
            return;
        }
        if(timeMachine==null){
            timeMachine = new TimeMachine(Lookup.newInstance());
        }
        errMsg.append(timeMachine.getMilliTimeString())
                .append(" DEBUG [")
                .append(context.getSimpleName())
                .append("]  ")
                .append(msg);
        errMsg.append('\n');
        if(silent){
            buf.append(errMsg);
        } else {
            for (LogAppender appender : appenders) {
                appender.error(errMsg.toString());
            }
        }
    }

    public static void stash() {
        silent=true;
        buf=new StringBuilder();
    }

    public static void flush() {
        for (LogAppender appender : appenders) {
            appender.info(buf.toString());
        }
        buf.setLength(0);
    }

    public static void setSilent(boolean silent) {
        KLogger.silent = silent;
    }

    public void setDoOutput(boolean doOutput) {
        this.doOutput = doOutput;
    }

    public void printCurrentStackTrace(){
        try {
            throw new Exception("cache init");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

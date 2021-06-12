package nkod3r.lookupcache.logger;

public class StdOutLogAppender implements LogAppender {

    @Override
    public void info(String message) {
        System.out.print(message);
    }

    @Override
    public void error(String msg) {
        System.err.print(msg);
    }

}

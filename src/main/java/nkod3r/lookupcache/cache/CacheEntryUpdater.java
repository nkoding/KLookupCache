package nkod3r.lookupcache.cache;

import nkod3r.lookupcache.lookup.Lookup;
import nkod3r.lookupcache.time.TimeMachine;

import java.io.IOException;


public abstract class CacheEntryUpdater<T> {
    private final String key;
    private final Class<T> type;
    private long keepAliveTime;
    private long lastUpdateTime;

    public CacheEntryUpdater(Class<T> type, long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        this.lastUpdateTime=-1;
        this.key = type.getName();
        this.type=type;
    }

    public CacheEntryUpdater(String key, Class<T> type, long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        this.lastUpdateTime=-1;
        this.key = key;
        this.type=type;
    }

    /**
     * retrieves a new version of the object
     *
     * @return
     * @param params
     */
    public abstract T update(Object... params) throws IOException;

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    public void setUpdatedNow() {
        this.lastUpdateTime = Lookup.get(TimeMachine.class).currentTimeMillis();
    }

    public String getKey() {
        return key;
    }

    public Class<T> getType() {
        return type;
    }
}

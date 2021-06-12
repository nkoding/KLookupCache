package nkod3r.lookupcache.cache;


import nkod3r.lookupcache.logger.KLogger;
import nkod3r.lookupcache.lookup.Lookup;
import nkod3r.lookupcache.time.TimeMachine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * though it's called "SimpleCache" it is more powerfult than the not-so simple nkod3r.lookupcache.cache because it's faster and more flexible.
 * the cost is less type safety and that you have to do key management for all the cached objects keys
 */
public class Cache {
    private KLogger log = new KLogger(Cache.class);
    public static final long ALWAYS_UPDATE = -10; //dont nkod3r.lookupcache.cache, always update
    public static final long KEEPALIVE_FOREVER = -20; //just update once, then nkod3r.lookupcache.cache forever
    private ConcurrentHashMap<String, Object> contents;
    private ConcurrentHashMap<String, CacheEntryUpdater<?>> updaters;
    private ConcurrentHashMap<String, List<AfterUpdateListener>> afterUpdateListeners;
    private ConcurrentHashMap<String, List<BeforeUpdateListener>> beforeUpdateListeners;

    public Cache() {
//        log.printCurrentStackTrace();
        log.setDoOutput(false);
        contents = new ConcurrentHashMap<>();
        updaters = new ConcurrentHashMap<>();
        afterUpdateListeners = new ConcurrentHashMap<>();
        beforeUpdateListeners = new ConcurrentHashMap<>();
    }

    public <T> void add(CacheEntryUpdater<T> updater) {
        updaters.put(updater.getKey(), updater);
    }

    public <T> T get(Class<T> key, Object... params) {
        return get(key.getName());
    }

    public <T> T get(String key, Object... params) {
        T obj;
        if (isExpired(key)) {
            //update expired objects
            update(key, params);
        }

        if (!contents.containsKey(key)) {
            //update uninitialized FOREVER-updaters once
            log.debug("init: " + key);
            update(key, params);
        }
        log.debug("get cached: " + key);
        obj = (T) contents.get(key);
        return obj;
    }

    /**
     * get from nkod3r.lookupcache.cache, does not update.
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T peek(String key) {
        return (T) contents.get(key);
    }

    public <T> T peek(Class<T> key) {
        return (T) contents.get(keyOf(key));
    }

    public void put(String key, Object newObject) {
        if (updaters.containsKey(key)) {
            updaters.get(key).setUpdatedNow();
        }
        Object oldObject = contents.get(key);
        contents.put(key, newObject);
        notifyAfterUpdateListeners(key, oldObject, newObject);
    }

    public <T> void put(Class<T> key, Object newObject) {
        put(keyOf(key), newObject);
    }

    public <T> void put(Object newObject) {
        put(keyOf(newObject.getClass()), newObject);
    }

    public <T> Object update(Class<T> key, Object... params) {
        return update(keyOf(key), params);
    }

    public Object update(String key, Object... params) {
        if (!canUpdate(key)) {
            return null;
        }
        CacheEntryUpdater<?> updater = updaters.get(key);
        if (updater == null) {
            return null;
        }
        TimeMachine timeMachine = Lookup.get(TimeMachine.class);
        Object oldObject, newObject;
        try {
            log.debug("updating: " + key);
            oldObject = contents.get(key);
            newObject = updater.update(params);
            notifyAfterUpdateListeners(key, oldObject, newObject);
            updater.setLastUpdateTime(timeMachine.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (newObject == null) {
            log.debug("keep cached: " + key);
            return contents.get(key);//dont change the nkod3r.lookupcache.cache if update failed
        }
        this.contents.put(key, newObject);
        return newObject;
    }

    public <T> boolean isExpired(Class<T> key) {
        return isExpired(keyOf(key));
    }

    public <T> boolean isExpired(String key) {
        long now = Lookup.get(TimeMachine.class).currentTimeMillis();
        CacheEntryUpdater<?> updater = updaters.get(key);
        if (updater == null) {
            return false;
        }
        long lastUpdate = updater.getLastUpdateTime();
        long keepAlive = updater.getKeepAliveTime();
        if (keepAlive == KEEPALIVE_FOREVER) {
            //cached object never expires, hence it'll never be updated
            return false;
        }
        if (keepAlive == ALWAYS_UPDATE) {
            //cached object immediately expires, hence it'll always be updated
            return true;
        }
        if (now - lastUpdate > keepAlive) {
            log.debug(key + " expired after " + keepAlive + ", last update:" + lastUpdate + ", now:" + now);
            return true;
        }
        return false;
    }

    public <C, T> String keyOf(Class<T> type, String... moreParts) {
        StringBuilder buf = new StringBuilder();
        buf.append(type.getName());
        if (moreParts.length > 0) {
            buf.append(".");
        }
        for (int i = 0; i < moreParts.length; i++) {
            buf.append(moreParts[i]);
            if (i < moreParts.length - 1) {
                buf.append('.');
            }
        }
        return buf.toString();
    }

    public <T> T find(Class<T> returnType) throws Exception {
        T cached = get(returnType);
        if (cached != null) {
            log.debug("returning cached " + returnType.getSimpleName() + " object");
            return cached;
        }
        try {
            log.debug("trying to instantiate from Lookup: " + returnType.getSimpleName());
            T instance = Lookup.newInstance(returnType);
            if (instance == null) {
                log.debug("could not instantiate from Lookup" + returnType.getSimpleName());
                throw new Exception("could not instantiate " + returnType.getSimpleName());
            }
            log.debug("successfully instantiated from Lookup: " + instance.toString());
            return instance;
        } catch (Exception e) {
            log.debug("error instantiating " + returnType.getSimpleName() + ":" + e.getMessage());
            throw new Exception("error instantiating " + returnType.getSimpleName() + ":" + e.getMessage());
        }
    }

    public void setOnUBeforeUpdateListener(String key, AfterUpdateListener listener) {
        if (!afterUpdateListeners.containsKey(key)) {
            afterUpdateListeners.put(key, new ArrayList<>());
        }
        List<AfterUpdateListener> listenerSubList = afterUpdateListeners.get(key);
        listenerSubList.add(listener);
    }

    public <T> void setOnUpdateListener(Class<T> key, AfterUpdateListener listener) {
        setOnUpdateListener(keyOf(key),listener);
    }

    public void setOnUpdateListener(String key, AfterUpdateListener listener) {
        if (!afterUpdateListeners.containsKey(key)) {
            afterUpdateListeners.put(key, new ArrayList<>());
        }
        List<AfterUpdateListener> listenerSubList = afterUpdateListeners.get(key);
        if (listenerSubList.contains(listener)) {
            log.error("trying to add already added listener.");
            return;
        }
        listenerSubList.add(listener);
    }

    private boolean canUpdate(String key) {
        List<BeforeUpdateListener> beforeUpdateListeners = this.beforeUpdateListeners.get(key);
        if (beforeUpdateListeners == null) {
            return true;
        }
        for (BeforeUpdateListener checker : beforeUpdateListeners) {
            if (!checker.check()) {
                return false;
            }
        }
        return true;
    }

    public synchronized void notifyAfterUpdateListeners(String key, Object oldObject, Object newObject) {
        List<AfterUpdateListener> afterUpdateListeners = this.afterUpdateListeners.get(key);
        if (afterUpdateListeners == null) {
            return;
        }
        for (AfterUpdateListener listener : afterUpdateListeners) {
            listener.update(oldObject, newObject);
        }
    }


    public void removeOnUpdateListener(String key, String ident) {
        List<AfterUpdateListener> afterUpdateListeners = this.afterUpdateListeners.get(key);
        if (afterUpdateListeners == null) {
            return;
        }

        ArrayList<AfterUpdateListener> toRemove = new ArrayList<>();
        for (AfterUpdateListener listener : afterUpdateListeners) {
            if (!listener.getIdent().equals(ident)) {
                continue;
            }
            toRemove.add(listener);
        }
        afterUpdateListeners.removeAll(toRemove);
    }
}

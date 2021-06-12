package nkod3r.lookupcache.examples;

import nkod3r.lookupcache.cache.CacheEntryUpdater;
import nkod3r.lookupcache.lookup.LocalLookup;
import nkod3r.lookupcache.lookup.Lookup;
import nkod3r.lookupcache.lookup.LookupConsumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static nkod3r.lookupcache.time.TimeMachine.SECOND;

public class LookupAndCacheExample implements LookupConsumer {
    private LocalLookup lookup;

    public LookupAndCacheExample(LocalLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * adds a cache updater to update the example object
     */
    private void initCacheUpdaterAndListener() {
        cache().add(new CacheEntryUpdater<>(CacheExampleObject.class, 6 * SECOND) {
            @Override
            public CacheExampleObject update(Object[] params) throws IOException {
                //add your update code here
                //e.g. load data from the web or a DB
                return new CacheExampleObject();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        long time1, time2, time3;

        //create this class via lookup. Wil inject LocalLookup into it
        LookupAndCacheExample example = Lookup.get(LookupAndCacheExample.class);

        //register a cache updater to fetch a fresh version of the CacheExampleObject
        example.initCacheUpdaterAndListener();

        //get the object from cache
        CacheExampleObject exampleObject = example.cache(CacheExampleObject.class);

        ///save and print time of object creation
        time1=exampleObject.time;
        System.out.println("init:\t"+time1);

        //second fetch within keepAliveTime
        //This does not trigger a cache refresh (keepAliveTime == 6 * SECOND
        TimeUnit.SECONDS.sleep(5);
        exampleObject = example.cache(CacheExampleObject.class);
        time2=exampleObject.time;
        System.out.println("2nd:\t"+time2);


        //third fetch after keepAliveTime
        //wait 2 more secs and try again.
        //This does trigger a cache refresh
        TimeUnit.SECONDS.sleep(2);
        exampleObject = example.cache(CacheExampleObject.class);
        time3=exampleObject.time;
        System.out.println("3rd:\t"+time3);
    }

    @Override
    public <T> T lookup(Class<T> clazz) {
        return lookup.get(clazz);
    }
}

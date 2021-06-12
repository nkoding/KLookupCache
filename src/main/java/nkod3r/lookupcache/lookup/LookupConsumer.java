package nkod3r.lookupcache.lookup;


import nkod3r.lookupcache.cache.Cache;

public interface LookupConsumer {
    default Cache cache(){
        return lookup(Cache.class);
    }
    default <T>  T cache(Class<T> key){
        return lookup(Cache.class).get(key);
    }

    default void setLookup(LocalLookup lookup){ }

    <T> T lookup(Class<T> clazz);

}

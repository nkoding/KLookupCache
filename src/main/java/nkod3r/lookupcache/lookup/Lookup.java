/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nkod3r.lookupcache.lookup;

import java.util.List;

/**
 * @author kdot
 */
public class Lookup {
    private static LocalLookup lookup = new LocalLookup();

    public static LocalLookup newInstance() {
        return new LocalLookup();
    }
    public static  <T> T newInstance(Class<T> clazz) throws LinkageError{
        return lookup.newInstance(clazz);
    }

    public static void add(Object toAdd) {
        lookup.add(toAdd);
    }

    public static <T> boolean contains(Class<T> clazz) {
        return lookup.contains(clazz);
    }

    public static <T> T get(Class<T> clazz) {
        return lookup.get(clazz);
    }

    public static <T> T getNew(Class<T> clazz) {
        return lookup.getNew(clazz);
    }

    public static <T> List<T> getAll(Class<T> clazz) {
        return lookup.getAll(clazz);
    }

    public static void register(Class<?>... classes) {
        lookup.register(classes);
    }
}

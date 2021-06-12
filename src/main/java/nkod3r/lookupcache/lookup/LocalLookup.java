/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nkod3r.lookupcache.lookup;


import nkod3r.lookupcache.logger.KLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LocalLookup {
    KLogger log = new KLogger(LocalLookup.class);
    private HashMap<Class<?>, ArrayList<?>> contents = new HashMap<>();
    private HashMap<Class<?>, ArrayList<LookupListener>> listeners = new HashMap<>();

    public void add(Object toAdd) {
        Class<?> clazz = toAdd.getClass();
        //add to classes list
        ArrayList clazzContainer = ensureEntryContainer(clazz);
        clazzContainer.add(toAdd);
        //add to superclass container
        Class<?>[] ifces = toAdd.getClass().getInterfaces();
        for (Class<?> ifce : ifces) {
            clazzContainer = ensureEntryContainer(ifce);
            clazzContainer.add(toAdd);
        }
        Class<?> superClass = toAdd.getClass().getSuperclass();
        while (superClass != Object.class) {
            clazzContainer = ensureEntryContainer(superClass);
            clazzContainer.add(toAdd);
            superClass = superClass.getSuperclass();
        }

    }

    public <T> boolean contains(Class<T> clazz) {
        return contents.containsKey(clazz);
    }

    public <T> T get(Class<T> clazz) {
        if (!contents.containsKey(clazz)) {
            //try to instantiate default constructor
            T instance = newInstance(clazz);
            if (instance == null) {
//                log.debug("unable to create instance of " + clazz.getName());
                throw new NullPointerException("unable to create instance of " + clazz.getName());
            }
            add(instance);
            return instance;
        }
        ensureEntryContainer(clazz);
        ArrayList<?> objects = contents.get(clazz);
        if (objects.isEmpty()) {
            return null;
        }
        return (T) objects.get(0);
    }

    public  <T> T newInstance(Class<T> clazz) throws LinkageError{
        Constructor<?> constructor;
        T instance = null;
        if ((constructor = getLookupConstructor(clazz, LookupConsumer.class)) != null) {
            log.debug("instantiating with this nkod3r.lookupcache.lookup: " + clazz.getName());
            try {
                instance = instantiate(constructor, this);
            } catch (Exception e) {
                String msg="Error instantiation Class '" + clazz + "' e.G. error in constructor or has interface LookupConsumer but no nkod3r.lookupcache.lookup constructor";
                log.error(msg);
                e.printStackTrace();
                throw new LinkageError(msg, e);
            }
        } else if ((constructor = getLookupConstructor(clazz, LookupProvider.class)) != null) {
            log.debug("instantiating with new nkod3r.lookupcache.lookup: " + clazz.getName());
            try {
                instance = instantiate(constructor, Lookup.newInstance());
            } catch (Exception e) {
                throw new LinkageError("Class '" + clazz + "' has interface LookupProvider but no nkod3r.lookupcache.lookup constructor.", e);
            }
        } else if ((constructor = getNoArgsPublicConstructor(clazz)) != null) {
            log.debug("instantiating with NoArgsConstructor: " + clazz.getName());
            try {
                instance = instantiate(constructor);
            } catch (Exception e){
                e.printStackTrace();
                throw new LinkageError("Class '" + clazz + "' threw an error on instantiation.", e);
            }
        }
        if (instance == null) {
            throw new LinkageError("Class '" + clazz + "' unable to instantiate. Needs LookupConstructor or default constructtor.");
        }
        return instance;
    }

    private <T> T instantiate(Constructor<?> constructor, Object... params) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        T instance = (T) constructor.newInstance(params);
        return instance;
    }

    public <T> T getNew(Class<T> clazz) {
        //try to instantiate default constructor
        T instance = newInstance(clazz);
        if (instance == null) {
            throw new NullPointerException("unable to create instance of " + clazz.getName());
        }
        add(instance);
        return instance;
    }

    public <T> List<T> getAll(Class<T> clazz) {
        ensureEntryContainer(clazz);
        ArrayList<?> objects = contents.get(clazz);
        if (objects.isEmpty()) {
            return null;
        }
        return (List<T>) objects;
    }

    private <T> ArrayList<?> ensureEntryContainer(Class<T> clazz) {
        if (contents.containsKey(clazz)) {
            return contents.get(clazz);
        }
        ArrayList<Object> container = new ArrayList<>();
        contents.put(clazz, container);


        ArrayList<Object> ifceContainer;
        //add interface containers
        for (Class<?> ifce : clazz.getClass().getInterfaces()) {
            if (contents.containsKey(ifce)) {
                continue;
            }
            ifceContainer = new ArrayList<>();
            contents.put(ifce, ifceContainer);
        }
        //add superclass container
        Class<?> superClass = clazz.getSuperclass();
        do {
            if (superClass == null) {
                break;
            }
            if (contents.containsKey(superClass)) {
                superClass = superClass.getSuperclass();
                continue;
            }
            ArrayList<Object> superClassContainer = new ArrayList<>();
            contents.put(superClass, superClassContainer);
            superClass = superClass.getSuperclass();
        } while (superClass != Object.class);
        return container;
    }

    private Constructor<?> getNoArgsPublicConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            // In Java 7-, use getParameterTypes and check the length of the array returned
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }
        return null;
    }

    private Constructor<?> getLookupConstructor(Class<?> clazz, Class<?> ifce) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            // In Java 7-, use getParameterTypes and check the length of the array returned
            if (constructor.getParameterCount() == 1 &&
                    constructor.getParameterTypes()[0].equals(LocalLookup.class) &&
                    superClassHasInterface(clazz, ifce)) {
                return constructor;
            }
        }
        return null;
    }

    private boolean superClassHasInterface(Class<?> lookupConsumerClass, Class<?> ifce) {
        Class<?> superClass = lookupConsumerClass;

        while (superClass != Object.class) {
            if (hasInterface(superClass, ifce)) {
                return true;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }

    private boolean hasInterface(Class<?> clazz, Class<?> needleInterface) {
        Class<?>[] ifces = clazz.getInterfaces();
        for (Class<?> ifce : ifces) {
            if (ifce.getName().equals(needleInterface.getName())) {
                return true;
            }
            for (Class<?> subIfce : ifce.getInterfaces()) {
                if (subIfce.getName().equals(needleInterface.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void register(Class<?>... classes) {
        Arrays.stream(classes).forEach(this::get);
    }

    public <T> T register(T toAdd) {
        add(toAdd);
        return toAdd;
    }

    public <T> void register(T... toAdd) {
        Arrays.stream(toAdd).forEach(this::add);
    }

    public <T> void registerListener(Class<T> trigger, LookupListener listener) {
        ArrayList<LookupListener> triggerListeners;
        triggerListeners = this.listeners.get(trigger);
        if (triggerListeners == null) {
            triggerListeners = new ArrayList<>();
            listeners.put(trigger, triggerListeners);
        }
        triggerListeners.add(listener);
    }

    public <T> void notify(Class<T> trigger, T oldObject, T updatedObject) {
        listeners.get(trigger).forEach(listener -> listener.changed(oldObject, updatedObject));
    }
}

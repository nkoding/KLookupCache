package nkod3r.lookupcache.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalLookupTest {

    @org.junit.jupiter.api.Test
    void add() {
        LocalLookup localLookup = new LocalLookup();
        localLookup.add(new Boolean(true));

        assertEquals(localLookup.get(Boolean.class), true);
    }

    @org.junit.jupiter.api.Test
    void newInstance() {

    }

    @org.junit.jupiter.api.Test
    void getNew() {
    }

    @org.junit.jupiter.api.Test
    void getAll() {
    }

    @org.junit.jupiter.api.Test
    void register() {
    }

    @org.junit.jupiter.api.Test
    void registerListener() {
    }

    @org.junit.jupiter.api.Test
    void testNotify() {
    }
}
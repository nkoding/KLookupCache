package nkod3r.lookupcache.cache;

public interface AfterUpdateListener {
    default String getIdent(){
        return "";
    }

    void update(Object oldObject, Object newObject);
}

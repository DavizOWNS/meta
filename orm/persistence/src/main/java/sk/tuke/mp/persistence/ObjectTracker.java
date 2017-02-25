package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.annotations.Entity;

/**
 * Created by DAVID on 18.2.2017.
 */
@Entity(name = "Hello")
public class ObjectTracker {

    public ObjectTracker() {
    }

    public boolean hasObject(Object obj)
    {
        return false;
    }
    public void registerObject(Object value)
    {

    }
}

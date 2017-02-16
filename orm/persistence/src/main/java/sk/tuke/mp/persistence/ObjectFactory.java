package sk.tuke.mp.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by DAVID on 16.2.2017.
 */
public class ObjectFactory {
    private Map<Class, Constructor> constructorMap;

    public ObjectFactory() {
        constructorMap = new HashMap<>();
    }

    public boolean registerType(Class type)
    {
        try {
            Constructor constructor = type.getConstructor();

            constructorMap.put(type, constructor);
        } catch (NoSuchMethodException e) {
            return false;
        }

        return true;
    }

    public Object createObject(Class type)
    {
        Constructor constructor = constructorMap.get(type);

        Object instance = null;
        try {
            instance = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return instance;
    }
}

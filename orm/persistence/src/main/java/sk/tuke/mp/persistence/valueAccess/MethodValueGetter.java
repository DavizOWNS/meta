package sk.tuke.mp.persistence.valueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DAVID on 18.2.2017.
 */
public class MethodValueGetter implements IValueGetter {
    private Method method;

    public MethodValueGetter(Method method) {
        this.method = method;
        this.method.setAccessible(true);
    }

    @Override
    public Object get(Object instance) {
        try {
            return method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}

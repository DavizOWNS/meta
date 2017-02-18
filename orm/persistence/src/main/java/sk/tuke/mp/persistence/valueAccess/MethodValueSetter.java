package sk.tuke.mp.persistence.valueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DAVID on 18.2.2017.
 */
public class MethodValueSetter implements IValueSetter {
    private Method method;

    public MethodValueSetter(Method method) {
        this.method = method;
        this.method.setAccessible(true);
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            method.invoke(instance, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

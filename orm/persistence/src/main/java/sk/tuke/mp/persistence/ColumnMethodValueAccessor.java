package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.model.IValueAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DAVID on 18.2.2017.
 */
public class ColumnMethodValueAccessor implements IValueAccessor{
    private Method setter;
    private Field field;

    public ColumnMethodValueAccessor(Method setter, Field field) {
        this.setter = setter;
        this.field = field;
        field.setAccessible(true);
        setter.setAccessible(true);
    }

    @Override
    public Object get(Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            setter.invoke(instance, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Class getValueType() {
        return field.getType();
    }
}

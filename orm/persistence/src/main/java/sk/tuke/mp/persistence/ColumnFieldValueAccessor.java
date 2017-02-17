package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.model.IValueAccessor;

import java.lang.reflect.Field;

/**
 * Created by DAVID on 17.2.2017.
 */
public class ColumnFieldValueAccessor implements IValueAccessor {
    private Field field;

    public ColumnFieldValueAccessor(Field field) {
        field.setAccessible(true);
        this.field = field;
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
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Class getValueType() {
        return field.getType();
    }
}

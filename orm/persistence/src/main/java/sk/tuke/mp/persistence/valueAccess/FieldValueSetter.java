package sk.tuke.mp.persistence.valueAccess;

import java.lang.reflect.Field;

/**
 * Created by DAVID on 18.2.2017.
 */
public class FieldValueSetter implements IValueSetter {
    private Field field;

    public FieldValueSetter(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    @Override
    public void set(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

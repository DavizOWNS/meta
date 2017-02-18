package sk.tuke.mp.persistence.valueAccess;

import java.lang.reflect.Field;

/**
 * Created by DAVID on 18.2.2017.
 */
public class FieldValueGetter implements IValueGetter {
    private Field field;

    public FieldValueGetter(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    @Override
    public Object get(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}

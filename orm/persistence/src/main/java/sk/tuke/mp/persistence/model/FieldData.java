package sk.tuke.mp.persistence.model;

import java.lang.reflect.Field;

/**
 * Created by DAVID on 16.2.2017.
 */
public class FieldData {
    private Field field;
    private IValueAccessor valueAccessor;

    public FieldData(Field field, IValueAccessor valueAccessor) {
        this.field = field;
        this.valueAccessor = valueAccessor;
    }

    public Field getField() {
        return field;
    }

    public IValueAccessor getValueAccessor() {
        return valueAccessor;
    }
}

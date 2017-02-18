package sk.tuke.mp.persistence.valueAccess;

/**
 * Created by DAVID on 17.2.2017.
 */
public class ColumnValueAccessor implements IValueAccessor {
    private IValueSetter setter;
    private IValueGetter getter;
    private Class valueType;

    public ColumnValueAccessor(Class valueType, IValueSetter setter, IValueGetter getter) {
        this.setter = setter;
        this.getter = getter;
        this.valueType = valueType;
    }

    @Override
    public Object get(Object obj) {
        return getter.get(obj);
    }

    @Override
    public void set(Object instance, Object value) {
        setter.set(instance, value);
    }

    @Override
    public Class getValueType() {
        return valueType;
    }
}

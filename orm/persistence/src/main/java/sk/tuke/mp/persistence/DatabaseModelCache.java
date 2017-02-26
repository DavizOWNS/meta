package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.infrastructure.DatabaseModel;
import sk.tuke.mp.persistence.infrastructure.Entity;
import sk.tuke.mp.persistence.infrastructure.Property;
import sk.tuke.mp.persistence.valueAccess.ColumnValueAccessor;
import sk.tuke.mp.persistence.valueAccess.FieldValueGetter;
import sk.tuke.mp.persistence.valueAccess.FieldValueSetter;
import sk.tuke.mp.persistence.valueAccess.IValueAccessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by DAVID on 26.2.2017.
 */
public class DatabaseModelCache {
    private DatabaseModel model;
    private Map<Property, IValueAccessor> valueAccessors;

    public DatabaseModelCache(DatabaseModel model) {
        this.model = model;
        valueAccessors = new HashMap<>();
    }

    public Object createObject(Entity entity)
    {
        try {
            return entity.getEntityType().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    public IValueAccessor getValueAccessor(Property property)
    {
        if(valueAccessors.containsKey(property))
            return valueAccessors.get(property);

        Class implementationType = null;
        if(property.getLazyImplementation() != null)
            implementationType = property.getLazyImplementation();
        else
            implementationType = property.getPropertyType();
        try {
            IValueAccessor accessor = new ColumnValueAccessor(implementationType,
                    new FieldValueSetter(property.getEntity().getEntityType().getDeclaredField(property.getFieldName())),
                    new FieldValueGetter(property.getEntity().getEntityType().getDeclaredField(property.getFieldName())));

            valueAccessors.put(property, accessor);

            return accessor;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}

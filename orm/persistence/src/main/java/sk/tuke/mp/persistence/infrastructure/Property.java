package sk.tuke.mp.persistence.infrastructure;

import java.util.HashMap;

/**
 * Created by DAVID on 25.2.2017.
 */
public class Property {
    public static class Reference
    {
        private String entityName;
        private String propertyName;

        public Reference(String entityName, String propertyName) {
            this.entityName = entityName;
            this.propertyName = propertyName;
        }

        public String getEntityName() {
            return entityName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

    private String name;
    private boolean isPrimaryKey = false;
    private boolean isRequired = false;
    private HashMap<String, Object> annotations;
    private Class propertyType;
    private Class lazyImplementation;
    private String columnName;
    private Reference reference;
    private Entity entity;

    private Property(String typeName, String name) {
        this.name = name;
        try {
            propertyType = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        annotations = new HashMap<>();
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getColumnName()
    {
        if(columnName != null) return columnName;

        return name;
    }

    public String getFieldName() {
        return name;
    }

    public Object getAnnotation(String name)
    {
        return annotations.get(name);
    }
    public Class getLazyImplementation()
    {
        return lazyImplementation;
    }

    public Reference getReference() {
        return reference;
    }

    public Class getPropertyType() {
        return propertyType;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "Property[" + entity.getName() + "." + getColumnName() + ']';
    }

    public static class Builder
    {
        private Property prop;

        public Builder(Entity entity, String typeName, String name)
        {
            prop = new Property(typeName, name);
            prop.entity = entity;
        }

        public Property.Builder setPrimaryKey()
        {
            prop.isPrimaryKey = true;

            return this;
        }
        public Property.Builder setRequired()
        {
            prop.isRequired = true;

            return this;
        }
        public Property.Builder setAnnotation(String name, Object value)
        {
            prop.annotations.put(name, value);

            return this;
        }
        public Property.Builder setLazyImplementation(Class cls)
        {
            prop.lazyImplementation = cls;

            return this;
        }
        public Property.Builder setColumnName(String name)
        {
            prop.columnName = name;

            return this;
        }
        public Property.Builder references(String entityName, String propertyName)
        {
            prop.reference = new Reference(entityName, propertyName);

            return this;
        }

        public Property build()
        {
            return prop;
        }
    }
}

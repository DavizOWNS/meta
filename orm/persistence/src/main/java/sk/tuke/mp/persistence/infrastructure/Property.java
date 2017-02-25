package sk.tuke.mp.persistence.infrastructure;

import java.util.HashMap;

/**
 * Created by DAVID on 25.2.2017.
 */
public class Property {
    private String name;
    private boolean isPrimaryKey = false;
    private boolean isRequired = false;
    private HashMap<String, Object> annotations;
    private Class propertyType;
    private Class lazyImplementation;
    private String columnName;

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

    public String getName() {
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

    public Class getPropertyType() {
        return propertyType;
    }

    public static class Builder
    {
        private Property prop;

        public Builder(String typeName, String name)
        {
            prop = new Property(typeName, name);
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

        public Property build()
        {
            return prop;
        }
    }
}

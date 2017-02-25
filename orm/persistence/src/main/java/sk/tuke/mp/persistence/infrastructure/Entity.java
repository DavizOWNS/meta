package sk.tuke.mp.persistence.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by DAVID on 25.2.2017.
 */
public class Entity {
    private String name;
    private Class entityType;
    private List<Property> properties;

    private Entity(String typeName, String name) {
        this.name = name;
        try {
            entityType = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static class Builder
    {
        private Entity entity;
        private Map<String, Property.Builder> properties;

        public Builder(String typeName, String name)
        {
            entity = new Entity(typeName, name);
        }

        public Property.Builder property(String propertyType, String propertyName) {
            Property.Builder prop = properties.get(propertyType);
            if(prop != null) return prop;

            prop = new Property.Builder(propertyType, propertyName);
            properties.put(propertyType, prop);
            return prop;
        }

        public Entity build()
        {
            List<Property> props = new ArrayList<>();
            for(Property.Builder p : properties.values())
            {
                props.add(p.build());
            }
            entity.properties = props;

            return entity;
        }
    }

    public String getName() {
        return name;
    }

    public Class getEntityType() {
        return entityType;
    }

    public Iterable<Property> getProperties()
    {
        return properties;
    }
}

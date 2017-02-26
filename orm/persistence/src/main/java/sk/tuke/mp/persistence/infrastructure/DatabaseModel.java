package sk.tuke.mp.persistence.infrastructure;

import java.util.*;

/**
 * Created by DAVID on 25.2.2017.
 */
public class DatabaseModel {
    private List<Entity> entities;

    public Entity entity(String name)
    {
        for(Entity et : entities)
            if(et.getName().equals(name)) return et;

        return null;
    }
    public Entity entity(Class type)
    {
        for(Entity et : entities)
        {
            if(type.isAssignableFrom(et.getEntityType()))

                return et;
        }

        return null;
    }

    public Iterable<Entity> getEntities() {
        return entities;
    }

    public static class Builder implements IModelBuilder
    {
        private DatabaseModel dbModel;
        private Map<String, Entity.Builder> entities;

        public Builder()
        {
            entities = new HashMap<>();
            dbModel = new DatabaseModel();
        }

        public DatabaseModel build()
        {
            List<Entity> ents = new ArrayList<>();
            for(Entity.Builder e : entities.values())
            {
                ents.add(e.build());
            }
            dbModel.entities = ents;

            return dbModel;
        }

        @Override
        public Entity.Builder entity(String typeName, String name) {
            if(entities.containsKey(name))
                return entities.get(name);

            Entity.Builder b = new Entity.Builder(typeName, name);
            entities.put(name, b);
            return b;
        }
    }
}

package sk.tuke.mp.persistence.infrastructure;

import java.util.*;

/**
 * Created by DAVID on 25.2.2017.
 */
public class DatabaseModel {
    private List<Entity> entities;

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
            if(entities.containsKey(typeName))
                return entities.get(typeName);

            Entity.Builder b = new Entity.Builder(typeName, name);
            entities.put(typeName, b);
            return b;
        }
    }
}

package sk.tuke.mp.persistence.infrastructure;

/**
 * Created by DAVID on 25.2.2017.
 */
public interface IModelBuilder {
    Entity.Builder entity(String typeName, String name);
}

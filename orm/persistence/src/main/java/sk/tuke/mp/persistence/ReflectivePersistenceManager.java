package sk.tuke.mp.persistence;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import sk.tuke.mp.persistence.annotations.Id;
import sk.tuke.mp.persistence.infrastructure.DatabaseModel;
import sk.tuke.mp.persistence.infrastructure.Entity;
import sk.tuke.mp.persistence.infrastructure.IModelBuilder;
import sk.tuke.mp.persistence.infrastructure.Property;
import sk.tuke.mp.persistence.model.*;
import sk.tuke.mp.persistence.valueAccess.IValueAccessor;
import sk.tuke.mp.persistence.sql.IColumnValue;
import sk.tuke.mp.persistence.sql.QueryBuilder;
import sk.tuke.mp.persistence.sql.SqlCodes;

import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.sql.*;
import java.util.*;

/**
 * Created by DAVID on 15.2.2017.
 */
public class ReflectivePersistenceManager implements PersistenceManager {

    private Connection connection;
    private boolean isInitialized;

    private DatabaseModel dbModel;
    private DatabaseModelCache dbModelCache;

    public ReflectivePersistenceManager(Connection connection) {
        this.connection = connection;
        isInitialized = false;

        try {
            DatabaseModel.Builder modelBuilder = new DatabaseModel.Builder();
            Class modelSnapshotCls = Class.forName("sk.tuke.mp.persistence.generated.ModelSnapshot");
            Object modelSnapshot = modelSnapshotCls.newInstance();
            modelSnapshotCls.getMethod("configureModel", IModelBuilder.class).invoke(modelSnapshot, modelBuilder);

            dbModel = modelBuilder.build();
            System.out.println("Database model created successfully");
        }
        catch (InvocationTargetException ie)
        {
            ie.getCause().printStackTrace();
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        dbModelCache = new DatabaseModelCache(dbModel);
    }

    @Override
    public void initializeDatabase() {
        for(Entity e : dbModel.getEntities())
        {
            try {
                createTable(e);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return;
            }
        }

        isInitialized = true;
    }
    private void createTable(Entity entity) throws SQLException {
        for (Property p : entity.getProperties())
        {
            if(p.getReference() != null)
            {
                createTable(dbModel.entity(p.getReference().getEntityName()));
            }
        }

        try(Statement statement = connection.createStatement())
        {
            String query = QueryBuilder.createTableQuery(entity);
            System.out.println(query);
            statement.executeUpdate(query);
        }
        catch (SQLException ex)
        {
            if(ex.getSQLState().equals(SqlCodes.TABLE_ALREADY_EXISTS))
            {

            }
            else
            {
                throw ex;
            }
        }
    }

    @Override
    public <T> List<T> getAll(Class<T> clazz) throws PersistenceException {
        throwIfNotInitialized();

        Entity entity = dbModel.entity(clazz);
        if(entity == null)
            throw new PersistenceException("Provided class is not supported: " + clazz.getName());

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + entity.getName()))
        {
            ResultSet result =  statement.executeQuery();

            List<T> objects = new ArrayList<>();
            while(result.next())
            {
                T instance = (T) dbModelCache.createObject(entity);

                for(Property p : entity.getProperties())
                {
                    IValueAccessor valueAccessor = dbModelCache.getValueAccessor(p);

                    Object val = null;
                    if(p.getReference() != null)
                    {
                        int id = result.getInt(p.getColumnName());
                        val = get(valueAccessor.getValueType(), id);
                    }
                    else {
                        val = extractValue(result, p);
                    }


                    valueAccessor.set(instance, val);
                }

                objects.add(instance);
            }

            return objects;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException("Objects of type " + clazz + " could not be loaded from database", ex);
        }
    }

    @Override
    public <T> T get(Class<T> type, int id) throws PersistenceException {
        throwIfNotInitialized();

        if(type.isInterface())
        {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallback(new LazyLoader() {
                @Override
                public Object loadObject() throws Exception {
                    return getFromDb(type, id);
                }
            });

            return (T) enhancer.create();
        }

        return getFromDb(type, id);
    }
    private <T> T getFromDb(Class<T> type, int id) throws PersistenceException
    {
        Entity entity = dbModel.entity(type);
        if(entity == null)
            throw new PersistenceException("Provided class is not supported: " + type.getName());

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + entity.getName() + " WHERE id = ?"))
        {
            statement.setInt(1, id);
            ResultSet result =  statement.executeQuery();
            if(!result.next())
            {
                return null;
            }

            T instance = (T) dbModelCache.createObject(entity);

            for(Property p : entity.getProperties())
            {
                IValueAccessor valueAccessor = dbModelCache.getValueAccessor(p);

                if(p.getReference() != null)
                {
                    int refId = result.getInt(p.getColumnName());
                    valueAccessor.set(instance, get(valueAccessor.getValueType(), refId));
                }
                else
                {
                    Object val = extractValue(result, p);

                    valueAccessor.set(instance, val);
                }
            }

            return instance;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException("Object of type " + type.getName() + " with id " + id + " could not be loaded from database", ex);
        }
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        throwIfNotInitialized();

        Entity entity = dbModel.entity(type);
        if(entity == null)
            return null;

        boolean isFieldNameValid = false;
        String columnName = null;
        for(Property p : entity.getProperties()) {
            if (p.getFieldName().equals(fieldName))
            {
                isFieldNameValid = true;
                columnName = p.getColumnName();
                break;
            }
        }
        if(!isFieldNameValid)
            return null;

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + entity.getName() + " WHERE " + columnName + " = ?"))
        {
            statement.setObject(1, value);
            ResultSet result =  statement.executeQuery();

            List<T> objects = new ArrayList<>();
            while(result.next())
            {
                T instance = (T) dbModelCache.createObject(entity);

                for(Property p : entity.getProperties())
                {
                    IValueAccessor valueAccessor = dbModelCache.getValueAccessor(p);

                    Object val = null;
                    if(p.getReference() != null)
                    {
                        int id = result.getInt(p.getColumnName());
                        try {
                            val = get(valueAccessor.getValueType(), id);
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            return new ArrayList<>();
                        }
                    }
                    else {
                        val = extractValue(result, p);
                    }


                    valueAccessor.set(instance, val);
                }

                objects.add(instance);
            }

            return objects;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public int save(Object value) throws PersistenceException {
        throwIfNotInitialized();

        Entity entity = dbModel.entity(value.getClass());
        if(entity == null)
            throw new PersistenceException("Provided class is not supported: " + value.getClass().getName());

        Property primaryKeyProp = entity.primaryKeyProp();
        IValueAccessor idAccessor = dbModelCache.getValueAccessor(primaryKeyProp);

        if((int)idAccessor.get(value) != 0)
        {
            return updateObject(value, entity);
        }

        return insertObject(value, entity);
    }

    /**
     * Inserts object to database and returns its id
     * @param obj the object to insert
     * @return id of inserted object
     */
    private int insertObject(Object obj, Entity entity) throws PersistenceException {
        String query = QueryBuilder.createInsertQuery(entity, Collections.singletonList(obj), new IColumnValue() {
            @Override
            public Object getValue(Object obj, Property property) {
                IValueAccessor valueAccessor = dbModelCache.getValueAccessor(property);

                if(property.getReference() != null)
                {
                    try {
                        Object objRef = valueAccessor.get(obj);
                        if(objRef == null) return null;
                        Entity refEt = dbModel.entity(objRef.getClass());
                        int id = (int) dbModelCache.getValueAccessor(refEt.primaryKeyProp()).get(objRef);
                        if(id != 0)
                        {
                            return updateObject(objRef, refEt);
                        }
                        else
                        {
                            return insertObject(objRef, refEt);
                        }
                    } catch (PersistenceException e) {
                        e.printStackTrace(); //should not happen
                    }
                    return null;
                }
                else
                {
                    Object val = valueAccessor.get(obj);
                    if(val == null) return null;
                    return val;
                }
            }
        });

        try
        {
            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int rowsAffected = statement.executeUpdate();
            if(rowsAffected == 0) {
                throw new PersistenceException("Insert failed for object " + obj.toString());
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt(1);

            dbModelCache.getValueAccessor(entity.primaryKeyProp()).set(obj, id);

            return id;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException("Could not insert object of type " + obj.getClass(), ex);
        }
    }
    private int updateObject(Object obj, Entity entity) throws PersistenceException
    {
        int id = (int) dbModelCache.getValueAccessor(entity.primaryKeyProp()).get(obj);

        StringBuilder updateBuilder = new StringBuilder("UPDATE ");
        updateBuilder.append(entity.getName());
        updateBuilder.append(" SET ");
        boolean isFirst = true;
        for(Property p : entity.getProperties())
        {
            if(p.isPrimaryKey()) continue;

            if(!isFirst)
                updateBuilder.append(',');

            updateBuilder.append(p.getColumnName()).append('=').append('?');

            isFirst = false;
        }
        updateBuilder.append(" WHERE id=").append(id);

        try
        {
            PreparedStatement statement = connection.prepareStatement(updateBuilder.toString());
            int idx = 1;
            for(Property p : entity.getProperties())
            {
                if(p.isPrimaryKey()) continue;
                Object value = dbModelCache.getValueAccessor(p).get(obj);
                if(value == null) {
                    statement.setNull(idx, java.sql.Types.NULL);
                    continue;
                }
                if(p.getReference() != null) {
                    Object refObj = dbModelCache.getValueAccessor(p).get(obj);
                    Entity refEt = dbModel.entity(p.getReference().getEntityName());
                    int refId = (int) dbModelCache.getValueAccessor(refEt.primaryKeyProp()).get(refObj);
                    if(refId == 0)
                    {
                        value = insertObject(refObj, refEt);
                    }
                    else
                    {
                        value = updateObject(refObj, refEt);
                    }
                }

                statement.setObject(idx, value);

                idx++;
            }

            statement.execute();
        }
        catch (SQLException e)
        {
            throw new PersistenceException("Could not update object of type " + obj.getClass(), e);
        }

        return id;
    }

    private Object extractValue(ResultSet resultSet, String columnName, ColumnType columnType) throws SQLException {
        switch (columnType) {
            case INT:
                return resultSet.getInt(columnName);
            case DOUBLE:
                return resultSet.getDouble(columnName);
            case STRING:
                return resultSet.getString(columnName);
        }

        return null;
    }
    private Object extractValue(ResultSet resultSet, Property prop) throws SQLException {
        switch (prop.getPropertyType().getCanonicalName()) {
            case "java.lang.Integer":
                return resultSet.getInt(prop.getColumnName());
            case "java.lang.Double":
                return resultSet.getDouble(prop.getColumnName());
            case "java.lang.String":
                return resultSet.getString(prop.getColumnName());
        }

        return null;
    }
    private int getSqlType(ColumnType columnType)
    {
        switch (columnType)
        {
            case INT:
                return Types.INTEGER;
            case DOUBLE:
                return Types.DOUBLE;
            case STRING:
                return Types.VARCHAR;
            default:
                throw new InvalidParameterException();
        }
    }

    private void throwIfNotInitialized()
    {
        if(!isInitialized)
            throw new IllegalStateException("Not initialized. Call initializeDatabase() first.");
    }
}

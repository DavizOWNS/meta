package sk.tuke.mp.persistence;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
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

    private ObjectFactory objectFactory;
    private MetadataStore metaStore;
    private ObjectTracker objectTracker;

    public ReflectivePersistenceManager(Connection connection, Class... classes) {
        this.connection = connection;
        isInitialized = false;
        objectFactory = new ObjectFactory();
        metaStore = new MetadataStore(objectFactory);
        objectTracker = new ObjectTracker();

        DatabaseModel dbModel = null;
        try {
            DatabaseModel.Builder modelBuilder = new DatabaseModel.Builder();
            Class modelSnapshotCls = Class.forName("sk.tuke.mp.persistence.generated.ModelSnapshot");
            Object modelSnapshot = modelSnapshotCls.newInstance();
            modelSnapshotCls.getMethod("configureModel", IModelBuilder.class).invoke(modelSnapshot, modelBuilder);

            dbModel = modelBuilder.build();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        for(Entity entity : dbModel.getEntities())
        {
            Class cls = entity.getEntityType();
            for(Property property : entity.getProperties())
            {
                Class propCls = property.getPropertyType();
            }
        }

        try {
            metaStore.registerTablesForTypes(Arrays.asList(classes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided types are invalid. Parameter: classes", e);
        }
    }

    @Override
    public void initializeDatabase() {
        for(Table t : metaStore.getTables())
        {
            try {
                createTable(t);
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
        }

        isInitialized = true;
    }
    private void createTable(Table table) throws SQLException {
        for (Column c : table.getColumns())
        {
            if(c.getForeignKeyReference() != null)
            {
                createTable(c.getForeignKeyReference().getTable());
            }
        }

        try(Statement statement = connection.createStatement())
        {
            statement.executeUpdate(QueryBuilder.createTableQuery(table));
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

        Table table = metaStore.getTable(clazz);
        if(table == null)
            throw new PersistenceException("Provided class is not supported: " + clazz.getName());

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table.getName()))
        {
            ResultSet result =  statement.executeQuery();

            List<T> objects = new ArrayList<>();
            while(result.next())
            {
                T instance = (T) objectFactory.createObject(clazz);

                for(Column c : table.getColumns())
                {
                    IValueAccessor valueAccessor = metaStore.getValueAccessorForColumn(c);

                    Object val = null;
                    if(c.getForeignKeyReference() != null)
                    {
                        int id = result.getInt(c.getName());
                        val = get(valueAccessor.getValueType(), id);
                    }
                    else {
                        val = extractValue(result, c.getName(), c.getType());
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
        Table table = metaStore.getTable(type);
        if(table == null)
            throw new PersistenceException("Provided class is not supported: " + type.getName());

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table.getName() + " WHERE id = ?"))
        {
            statement.setInt(1, id);
            ResultSet result =  statement.executeQuery();
            if(!result.next())
            {
                return null;
            }

            T instance = (T) objectFactory.createObject(type);

            for(Column c : table.getColumns())
            {
                IValueAccessor valueAccessor = metaStore.getValueAccessorForColumn(c);

                if(c.getForeignKeyReference() != null)
                {
                    int refId = result.getInt(c.getName());
                    valueAccessor.set(instance, get(valueAccessor.getValueType(), refId));
                }
                else
                {
                    Object val = extractValue(result, c.getName(), c.getType());

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

        Table table = metaStore.getTable(type);
        if(table == null)
            return null;

        boolean isFieldNameValid = false;
        for(Column c : table.getColumns())
            if(c.getName().equals(fieldName)) isFieldNameValid = true;
        if(!isFieldNameValid)
            return null;

        try(PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table.getName() + " WHERE " + fieldName + " = ?"))
        {
            statement.setObject(1, value);
            ResultSet result =  statement.executeQuery();

            List<T> objects = new ArrayList<>();
            while(result.next())
            {
                T instance = (T) objectFactory.createObject(type);

                for(Column c : table.getColumns())
                {
                    IValueAccessor valueAccessor = metaStore.getValueAccessorForColumn(c);

                    Object val = null;
                    if(c.getForeignKeyReference() != null)
                    {
                        int id = result.getInt(c.getName());
                        try {
                            val = get(valueAccessor.getValueType(), id);
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            return new ArrayList<>();
                        }
                    }
                    else {
                        val = extractValue(result, c.getName(), c.getType());
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

        Table table = metaStore.getTable(value.getClass());
        if(table == null) throw new PersistenceException("Provided class is not supported: " + value.getClass().getName());
        Column primaryKeyColumn = table.primaryKeyColumn();
        IValueAccessor idAccessor = metaStore.getValueAccessorForColumn(primaryKeyColumn);

        if((int)idAccessor.get(value) != 0)
        {
            return updateObject(value, table);
        }

        return insertObject(value, table);
    }

    /**
     * Inserts object to database and returns its id
     * @param obj the object to insert
     * @return id of inserted object
     */
    private int insertObject(Object obj, Table table) throws PersistenceException {
        String query = QueryBuilder.createInsertQuery(table, Collections.singletonList(obj), new IColumnValue() {
            @Override
            public Object getValue(Object obj, Column column) {
                IValueAccessor valueAccessor = metaStore.getValueAccessorForColumn(column);

                if(column.getForeignKeyReference() != null)
                {
                    try {
                        Object objRef = valueAccessor.get(obj);
                        if(objRef == null) return null;
                        Table refTable = metaStore.getTable(objRef.getClass());
                        int id = (int) metaStore.getValueAccessorForColumn(refTable.primaryKeyColumn()).get(objRef);
                        if(id != 0)
                        {
                            return updateObject(objRef, refTable);
                        }
                        else
                        {
                            return insertObject(objRef, refTable);
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

            metaStore.getValueAccessorForColumn(table.primaryKeyColumn()).set(obj, id);

            return id;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException("Could not insert object of type " + obj.getClass(), ex);
        }
    }
    private int updateObject(Object obj, Table table) throws PersistenceException
    {
        int id = (int) metaStore.getValueAccessorForColumn(table.primaryKeyColumn()).get(obj);

        StringBuilder updateBuilder = new StringBuilder("UPDATE ");
        updateBuilder.append(table.getName());
        updateBuilder.append(" SET ");
        boolean isFirst = true;
        for(Column c : table.getColumns())
        {
            if(c.isPrimaryKey()) continue;

            if(!isFirst)
                updateBuilder.append(',');

            updateBuilder.append(c.getName()).append('=').append('?');

            isFirst = false;
        }
        updateBuilder.append(" WHERE id=").append(id);

        try
        {
            PreparedStatement statement = connection.prepareStatement(updateBuilder.toString());
            int idx = 1;
            for(Column c : table.getColumns())
            {
                if(c.isPrimaryKey()) continue;
                Object value = metaStore.getValueAccessorForColumn(c).get(obj);
                if(value == null) {
                    statement.setNull(idx, java.sql.Types.NULL);
                    continue;
                }
                if(c.getForeignKeyReference() != null) {
                    Object refObj = metaStore.getValueAccessorForColumn(c).get(obj);
                    int refId = (int) metaStore.getValueAccessorForColumn(c.getForeignKeyReference().getColumn()).get(refObj);
                    if(refId == 0)
                    {
                        value = insertObject(refObj, c.getForeignKeyReference().getTable());
                    }
                    else
                    {
                        value = updateObject(refObj, c.getForeignKeyReference().getTable());
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

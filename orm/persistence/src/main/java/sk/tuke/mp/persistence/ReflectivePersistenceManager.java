package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
import sk.tuke.mp.persistence.model.IValueAccessor;
import sk.tuke.mp.persistence.model.Table;
import sk.tuke.mp.persistence.sql.IColumnValue;
import sk.tuke.mp.persistence.sql.QueryBuilder;
import sk.tuke.mp.persistence.sql.SqlCodes;

import java.lang.reflect.Field;
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

    public ReflectivePersistenceManager(Connection connection, Class... classes) {
        this.connection = connection;

        isInitialized = false;
        objectFactory = new ObjectFactory();
        metaStore = new MetadataStore(objectFactory);

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
            throw new PersistenceException();
        String query = QueryBuilder.createSelectAllQuery(table);

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
                        switch (c.getType()) {
                            case INT:
                                val = result.getInt(c.getName());
                                break;
                            case DOUBLE:
                                val = result.getDouble(c.getName());
                                break;
                            case STRING:
                                val = result.getString(c.getName());
                                break;
                        }
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
            throw new PersistenceException();
        }
    }

    @Override
    public <T> T get(Class<T> type, int id) throws PersistenceException {
        throwIfNotInitialized();

        Table table = metaStore.getTable(type);
        if(table == null)
            throw new PersistenceException();
        //String query = QueryBuilder.createSelectAllQuery(table);

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
                    Object val = null;
                    switch (c.getType()) {
                        case INT:
                            val = result.getInt(c.getName());
                            break;
                        case DOUBLE:
                            val = result.getDouble(c.getName());
                            break;
                        case STRING:
                            val = result.getString(c.getName());
                            break;
                    }

                    valueAccessor.set(instance, val);
                }
            }

            return instance;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException();
        }
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        throwIfNotInitialized();

        Table table = metaStore.getTable(type);
        if(table == null)
            return new ArrayList<>();

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
                        switch (c.getType()) {
                            case INT:
                                val = result.getInt(c.getName());
                                break;
                            case DOUBLE:
                                val = result.getDouble(c.getName());
                                break;
                            case STRING:
                                val = result.getString(c.getName());
                                break;
                        }
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
            return new ArrayList<>();
        }
    }

    @Override
    public int save(Object value) throws PersistenceException {
        throwIfNotInitialized();

        return insertObject(value);
    }

    /**
     * Inserts object to database and returns its id
     * @param obj the object to insert
     * @return id of inserted object
     */
    private int insertObject(Object obj) throws PersistenceException {
        Table table = metaStore.getTable(obj.getClass());
        if(table == null)
            throw new PersistenceException();
        String query = QueryBuilder.createInsertQuery(table, Collections.singletonList(obj), new IColumnValue() {
            @Override
            public String getValue(Object obj, Column column) {
                IValueAccessor valueAccessor = metaStore.getValueAccessorForColumn(column);

                if(column.getForeignKeyReference() != null)
                {
                    try {
                        Object objRef = valueAccessor.get(obj);
                        if(objRef == null) return "NULL";
                        Table refTable = metaStore.getTable(objRef.getClass());
                        int id = (int) metaStore.getValueAccessorForColumn(refTable.primaryKeyColumn()).get(objRef);
                        if(id > 0)
                        {
                            return String.valueOf(id);
                        }
                        else
                        {
                            return String.valueOf(insertObject(objRef));
                        }
                    } catch (PersistenceException e) {
                        e.printStackTrace(); //should not happen
                    }
                    return null;
                }
                else
                {
                    Object val = valueAccessor.get(obj);
                    if(val == null) return "NULL";
                    return val.toString();
                }
            }
        });

        try
        {
            //connection.setAutoCommit(false);

            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int rowsAffected = statement.executeUpdate();
            if(rowsAffected == 0) {
                connection.setAutoCommit(true);
                throw new PersistenceException();
            }

            //connection.commit();
            //connection.setAutoCommit(true);

            ResultSet generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt(1);

            metaStore.getValueAccessorForColumn(table.primaryKeyColumn()).set(obj, id);

            return id;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException();
        }
    }

    private void throwIfNotInitialized()
    {
        if(!isInitialized)
            throw new IllegalStateException("Not initialized. Call initializeDatabase() first.");
    }
}

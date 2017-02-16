package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
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
    private Class[] supportedTypes;
    private Map<Class, Table> tableMap;
    private boolean isInitialized;

    private ObjectFactory objectFactory;

    public ReflectivePersistenceManager(Connection connection, Class... classes) {
        this.connection = connection;
        this.supportedTypes = classes;

        isInitialized = false;
        objectFactory = new ObjectFactory();

        Exception error = buildModel();
        if(error != null)
            throw new IllegalArgumentException("Provided types are invalid. Parameter: classes", error);
    }

    private Exception buildModel()
    {
        if(supportedTypes.length == 0)
            return new Exception("No types provided");

        Map<Class, Table> tableLookup = new HashMap<>(supportedTypes.length);
        Set<Class> supported = new HashSet<>();
        supported.addAll(Arrays.asList(supportedTypes));
        Set<Class> visitedTypes = new HashSet<>();

        for(Class cls : supportedTypes)
        {
            if(tableLookup.containsKey(cls)) continue;

            try {
                Table table = createTable(cls, tableLookup, supported, visitedTypes);
            } catch (Exception e) {
                return e;
            }
        }

        tableMap = tableLookup;

        return null;
    }

    private Table createTable(Class cls, Map<Class, Table> tableLookup,
                              Set<Class> supportedComplexTypes,
                              Set<Class> visitedTypes) throws Exception {
        visitedTypes.add(cls);

        if(!objectFactory.registerType(cls))
            throw new Exception("Type " + cls.getName() + " does not have parameterless constructor");

        String tableName = cls.getSimpleName();
        Field[] fields = cls.getDeclaredFields();
        List<Column> columns = new ArrayList<>();
        for(Field f : fields)
        {
            String columnName = f.getName();
            ColumnType columnType = ColumnType.Unknown;
            Class type = f.getType();
            Column column = null;
            if(type == int.class)
            {
                columnType = ColumnType.INT;
                if(Objects.equals(columnName, "id"))
                    column = Column.createPrimariKeyColumn();
                else
                    column = Column.createPrimitiveColumn(columnName, columnType);
            }
            else if(type == double.class)
            {
                columnType = ColumnType.DOUBLE;
                column = Column.createPrimitiveColumn(columnName, columnType);
            }
            else if(type == String.class)
            {
                columnType = ColumnType.STRING;
                column = Column.createPrimitiveColumn(columnName, columnType);
            }
            else
            {
                if(!supportedComplexTypes.contains(type))
                    throw new Exception("Required type not provided"); //TODO proper exception

                Table ref = null;
                if(!tableLookup.containsKey(type))
                {
                    if(visitedTypes.contains(type)) //circular dependency
                    {
                        throw new Exception("Circular dependency in provided types"); //TODO proper exception
                    }
                    ref = createTable(type, tableLookup, supportedComplexTypes, visitedTypes);
                }
                else
                {
                    ref = tableLookup.get(type);
                }

                column = Column.createReferenceColumn(columnName, ref);
            }

            columns.add(column);
        }

        Table table = new Table(tableName, columns);

        tableLookup.put(cls, table);
        return table;
    }

    @Override
    public void initializeDatabase() {
        for(Table t : tableMap.values())
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

        Table table = tableMap.get(clazz);
        if(table == null)
            throw new PersistenceException();
        String query = QueryBuilder.createSelectAllQuery(tableMap.get(clazz));

        try(Statement statement = connection.createStatement())
        {
            ResultSet result =  statement.executeQuery(query);

            List<T> objects = new ArrayList<T>();
            while(result.next())
            {
                T instance = (T) objectFactory.createObject(clazz);

                for(Column c : table.getColumns())
                {
                    Field field = null;
                    try {
                        field = clazz.getDeclaredField(c.getName());
                        field.setAccessible(true);

                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }

                    Object val = null;
                    if(c.getForeignKeyReference() != null)
                    {
                        int id = result.getInt(c.getName());
                        val = get(field.getClass(), id);
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


                    try {
                        field.set(instance, val);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                objects.add(instance);
            }

            return objects;
        }
        catch (SQLException ex)
        {
            throw new PersistenceException();
        }
    }

    @Override
    public <T> T get(Class<T> type, int id) throws PersistenceException {
        throwIfNotInitialized();
        return null;
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        throwIfNotInitialized();
        return null;
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
        Table table = tableMap.get(obj.getClass());
        if(table == null)
            throw new PersistenceException();
        String query = QueryBuilder.createInsertQuery(table, Collections.singletonList(obj), new IColumnValue() {
            @Override
            public String getValue(Object obj, Column column) {
                Field field = null;
                try {
                    field = obj.getClass().getDeclaredField(column.getName());
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }

                if(column.getForeignKeyReference() != null)
                {
                    try {
                        Object objRef = field.get(obj);
                        Field refIdField = objRef.getClass().getDeclaredField("id");
                        refIdField.setAccessible(true);
                        int id = (int) refIdField.get(objRef);
                        if(id > 0)
                        {
                            return String.valueOf(id);
                        }
                        else
                        {
                            return String.valueOf(insertObject(objRef));
                        }
                    } catch (IllegalAccessException | PersistenceException | NoSuchFieldException e) {
                        e.printStackTrace(); //should not happen
                    }
                    return null;
                }
                else
                {
                    try {
                        return field.get(obj).toString();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace(); //should not happen
                        return null;
                    }
                }
            }
        });

        try
        {
            connection.setAutoCommit(false);

            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int rowsAffected = statement.executeUpdate();
            if(rowsAffected == 0) {
                connection.setAutoCommit(true);
                throw new PersistenceException();
            }

            connection.commit();
            connection.setAutoCommit(true);

            ResultSet generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt(1);

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

package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
import sk.tuke.mp.persistence.model.Table;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Created by DAVID on 15.2.2017.
 */
public class ReflectivePersistenceManager implements PersistenceManager {

    private Connection connection;
    private Class[] supportedTypes;
    private Map<Class, Table> tableMap;
    private boolean isInitialized;

    public ReflectivePersistenceManager(Connection connection, Class... classes) {
        this.connection = connection;
        this.supportedTypes = classes;

        isInitialized = false;

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
        try(Statement stmt = connection.createStatement())
        {
            stmt.executeUpdate("create table items (id INT primary key) ");
        }
        catch (SQLException ex)
        {
            if(ex.getSQLState().equals("X0Y32")) //table already exists
            {
                return; // That's OK
            }
            ex.printStackTrace();
        }
    }

    @Override
    public <T> List<T> getAll(Class<T> clazz) throws PersistenceException {
        throwIfNotInitialized();
        return null;
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
        return 0;
    }

    private void throwIfNotInitialized()
    {
        if(!isInitialized)
            throw new IllegalStateException("Not initialized. Call initializeDatabase() first.");
    }
}

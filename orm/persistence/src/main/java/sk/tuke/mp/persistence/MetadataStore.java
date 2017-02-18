package sk.tuke.mp.persistence;

import sk.tuke.mp.persistence.annotations.Getter;
import sk.tuke.mp.persistence.annotations.Required;
import sk.tuke.mp.persistence.annotations.Setter;
import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
import sk.tuke.mp.persistence.valueAccess.*;
import sk.tuke.mp.persistence.model.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by DAVID on 17.2.2017.
 */
public class MetadataStore {
    private List<Class> availableTypes;
    private Map<Class, Table> tableMap;
    private ObjectFactory objectFactory;
    private Map<Column, IValueAccessor> valueAccessorMap;

    public MetadataStore(ObjectFactory objectFactory) {
        availableTypes = new ArrayList<>();
        tableMap = new HashMap<>();
        valueAccessorMap = new HashMap<>();
        this.objectFactory = objectFactory;
    }

    public void registerTablesForTypes(List<Class> types) throws Exception
    {
        availableTypes.addAll(types);

        Map<Class, Table> tableLookup = new HashMap<>(types.size());
        Set<Class> supported = new HashSet<>();
        supported.addAll(availableTypes);
        Set<Class> visitedTypes = new HashSet<>();

        for(Class cls : types)
        {
            if(tableLookup.containsKey(cls)) continue;

            try {
                Table table = createTable(cls, tableLookup, supported, visitedTypes);
            } catch (Exception e) {
                throw e;
            }
        }

        tableMap.putAll(tableLookup);
    }

    private Table createTable(Class cls, Map<Class, Table> tableLookup,
                              Set<Class> supportedComplexTypes,
                              Set<Class> visitedTypes) throws Exception {
        visitedTypes.add(cls);

        if(!objectFactory.registerType(cls))
            throw new Exception("Type " + cls.getName() + " does not have parameterless constructor");

        String tableName = cls.getSimpleName();
        sk.tuke.mp.persistence.annotations.Table tableAno = (sk.tuke.mp.persistence.annotations.Table) cls.getAnnotation(sk.tuke.mp.persistence.annotations.Table.class);
        if(tableAno != null)
        {
            tableName = tableAno.name();
        }
        Table.Builder tableBuilder = new Table.Builder(tableName);
        Field[] fields = cls.getDeclaredFields();
        for(Field f : fields)
        {
            String columnName = f.getName();
            ColumnType columnType = ColumnType.Unknown;
            Class type = f.getType();
            Required requiredAno = f.getAnnotation(Required.class);
            boolean canBeNull = requiredAno == null;
            Column column = null;
            if(type == int.class)
            {
                columnType = ColumnType.INT;
                if(Objects.equals(columnName, "id"))
                    column = tableBuilder.addPrimaryKeyColumn();
                else
                    column = tableBuilder.addColumn(columnName, columnType, canBeNull);
            }
            else if(type == double.class)
            {
                columnType = ColumnType.DOUBLE;
                column = tableBuilder.addColumn(columnName, columnType, canBeNull);
            }
            else if(type == String.class)
            {
                columnType = ColumnType.STRING;
                column = tableBuilder.addColumn(columnName, columnType, canBeNull);
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

                column = tableBuilder.addReferenceColumn(columnName, ref, canBeNull);
            }

            IValueSetter setter = null;
            IValueGetter getter = null;
            IValueAccessor valueAccessor = null;
            Setter setterAno = f.getAnnotation(sk.tuke.mp.persistence.annotations.Setter.class);
            Getter getterAno = f.getAnnotation(sk.tuke.mp.persistence.annotations.Getter.class);
            if(setterAno != null)
            {
                Method setterMethod =  cls.getDeclaredMethod(setterAno.methodName(), type);
                if(setterMethod == null)
                {
                    throw new Exception("Setter method for field " + f.getName() + " not found");
                }
                setter = new MethodValueSetter(setterMethod);
            }
            else
            {
                setter = new FieldValueSetter(f);
            }
            if(getterAno != null)
            {
                Method getterMethod = cls.getDeclaredMethod(getterAno.methodName(), type);
                if(getterMethod == null)
                {
                    throw new Exception("Getter method for field " + f.getName() + " not found");
                }
                getter = new MethodValueGetter(getterMethod);
            }
            else {
                getter = new FieldValueGetter(f);
            }

            valueAccessorMap.put(column, new ColumnValueAccessor(type, setter, getter));
        }

        Table table = tableBuilder.build();

        tableLookup.put(cls, table);
        return table;
    }

    public Table getTable(Class type)
    {
        return tableMap.get(type);
    }

    public Iterable<Table> getTables()
    {
        return tableMap.values();
    }

    public IValueAccessor getValueAccessorForColumn(Column column)
    {
        return valueAccessorMap.get(column);
    }
}

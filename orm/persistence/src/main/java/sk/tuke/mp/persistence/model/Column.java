package sk.tuke.mp.persistence.model;

import java.lang.reflect.Field;

/**
 * Created by DAVID on 15.2.2017.
 */
public class Column {
    private String name;
    private ColumnType type;
    private boolean isPrimaryKey;
    private ForeignReference foreignKeyReference;

    private Column()
    {
        isPrimaryKey = false;
        foreignKeyReference = null;
    }

    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public ForeignReference getForeignKeyReference() {
        return foreignKeyReference;
    }

    public static Column createReferenceColumn(String columnName, Table table)
    {
        Column column = new Column();
        column.name = columnName;
        column.type = ColumnType.INT;
        column.foreignKeyReference = new ForeignReference(table, table.primaryKeyColumn());

        return column;
    }
    public static Column createPrimitiveColumn(String columnName, ColumnType type)
    {
        Column column = new Column();
        column.name = columnName;
        column.type = type;

        return column;
    }
    public static Column createPrimariKeyColumn()
    {
        Column column = new Column();
        column.name = "id";
        column.type = ColumnType.INT;
        column.isPrimaryKey = true;

        return column;
    }
}

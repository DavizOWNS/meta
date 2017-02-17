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
    private Table table;
    private boolean canBeNull;

    private Column(Table table)
    {
        isPrimaryKey = false;
        foreignKeyReference = null;
        this.table = table;
        canBeNull = true;
    }

    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    public boolean canBeNull() {
        return canBeNull;
    }

    public Table getTable() {
        return table;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public ForeignReference getForeignKeyReference() {
        return foreignKeyReference;
    }

    public static Column createReferenceColumn(Table table, String columnName, Table foreignTable, boolean canBeNull)
    {
        Column column = new Column(table);
        column.name = columnName;
        column.type = ColumnType.INT;
        column.foreignKeyReference = new ForeignReference(foreignTable, foreignTable.primaryKeyColumn());
        column.canBeNull = canBeNull;

        return column;
    }
    public static Column createPrimitiveColumn(Table table, String columnName, ColumnType type, boolean canBeNull)
    {
        Column column = new Column(table);
        column.name = columnName;
        column.type = type;
        column.canBeNull = canBeNull;

        return column;
    }
    public static Column createPrimaryKeyColumn(Table table)
    {
        Column column = new Column(table);
        column.name = "id";
        column.type = ColumnType.INT;
        column.isPrimaryKey = true;
        column.canBeNull = false;

        return column;
    }

    @Override
    public String toString() {
        return "Column[" + table.getName() + "." + getName() + "]";
    }
}

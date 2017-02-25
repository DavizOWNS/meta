package sk.tuke.mp.persistence.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DAVID on 15.2.2017.
 */
public class Table {
    private String name;

    private List<Column> columns;

    private Table(String name) {
        this.name = name;
    }

    public Column primaryKeyColumn()
    {
        for(Column c : columns)
        {
            if(c.isPrimaryKey())
                return c;
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public Iterable<Column> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return "Entity[" + getName() + "]";
    }

    public static class Builder
    {
        private Table table;
        private List<Column> columns;
        private boolean hasPrimaryKey = false;

        public Builder(String tableName) {
            table = new Table(tableName);
            columns = new ArrayList<>();
        }

        public Column addColumn(String columnName, ColumnType type, boolean canBeNull)
        {
            Column column = Column.createPrimitiveColumn(table, columnName, type, canBeNull);
            columns.add(column);

            return column;
        }
        public Column addPrimaryKeyColumn()
        {
            if(hasPrimaryKey)
            {

            }

            Column column = Column.createPrimaryKeyColumn(table);
            columns.add(column);

            hasPrimaryKey = true;

            return column;
        }
        public Column addReferenceColumn(String columnName, Table foreignTable, boolean canBeNull)
        {
            Column column = Column.createReferenceColumn(table, columnName, foreignTable, canBeNull);
            columns.add(column);

            return column;
        }

        public Table build() throws Exception {
            if(!hasPrimaryKey)
                throw new Exception("Primary key missing");
            table.columns = columns;
            return table;
        }
    }
}

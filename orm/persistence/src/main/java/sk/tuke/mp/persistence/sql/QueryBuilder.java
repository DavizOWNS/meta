package sk.tuke.mp.persistence.sql;

import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
import sk.tuke.mp.persistence.model.Table;

/**
 * Created by DAVID on 16.2.2017.
 */
public final class QueryBuilder {
    private QueryBuilder()
    {

    }

    public static String createTableQuery(Table table)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(table.getName());
        sb.append(" (");
        boolean isFirst = true;
        for(Column c : table.getColumns())
        {
            if(!isFirst)
                sb.append(", ");

            sb.append(c.getName());
            sb.append(" ");
            sb.append(SqlTypeConverter.getSqlTypeName(c.getType()));

            if(!c.canBeNull())
            {
                sb.append(" NOT NULL");
            }

            if(c.isPrimaryKey())
            {
                sb.append(String.format(" constraint pk_%s primary key generated always as identity (START WITH 1, INCREMENT BY 1)", table.getName()));
            }
            else if(c.getForeignKeyReference() != null)
            {
                sb.append(String.format(" constraint fk_%s_%s references %s", table.getName(), c.getForeignKeyReference().getTable().getName(), c.getForeignKeyReference().getTable().getName()));
            }

            isFirst = false;
        }
        sb.append(")");

        return sb.toString();
    }
    public static String createSelectAllQuery(Table table)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ");
        sb.append(table.getName());

        return sb.toString();
    }
    public static String createInsertQuery(Table table, Iterable<Object> objects, IColumnValue columnValue)
    {
        StringBuilder sb = new StringBuilder();
        for(Object obj : objects) {
            sb.append("insert into ");
            sb.append(table.getName());
            sb.append("(");
            boolean isFirst = true;
            for (Column c : table.getColumns()) {
                if (c.isPrimaryKey())
                    continue;
                if (!isFirst)
                    sb.append(", ");
                sb.append(c.getName());
                isFirst = false;
            }
            sb.append(") values (");

            isFirst = true;
            for(Column c : table.getColumns())
            {
                if(c.isPrimaryKey())
                    continue;

                if(!isFirst)
                    sb.append(", ");

                String value = columnValue.getValue(obj, c);
                if(c.getType() == ColumnType.STRING && !value.equals("NULL")) sb.append("'");
                sb.append(value);
                if(c.getType() == ColumnType.STRING && !value.equals("NULL")) sb.append("'");

                isFirst = false;
            }

            sb.append(")\n");
        }

        return sb.toString();
    }
}

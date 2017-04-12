package sk.tuke.mp.persistence.sql;

import sk.tuke.mp.persistence.infrastructure.Entity;
import sk.tuke.mp.persistence.infrastructure.Property;
import sk.tuke.mp.persistence.model.Column;
import sk.tuke.mp.persistence.model.ColumnType;
import sk.tuke.mp.persistence.model.Table;

import java.util.Objects;

/**
 * Created by DAVID on 16.2.2017.
 */
public final class QueryBuilder {
    private QueryBuilder()
    {

    }

    public static String createTableQuery(Entity entity)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(entity.getName());
        sb.append(" (");
        boolean isFirst = true;
        for(Property p : entity.getProperties())
        {
            if(!isFirst)
                sb.append(", ");

            sb.append(p.getColumnName());
            sb.append(" ");
            sb.append(SqlTypeConverter.getSqlTypeName(p));

            if(p.isUnique())
            {
                sb.append(" UNIQUE");
            }
            if(p.isRequired())
            {
                sb.append(" NOT NULL");
            }

            if(p.isPrimaryKey())
            {
                sb.append(String.format(" constraint pk_%s primary key generated always as identity (START WITH 1, INCREMENT BY 1)", entity.getName()));
            }
            else if(p.getReference() != null)
            {
                sb.append(String.format(" constraint fk_%s_%s references %s", entity.getName(), p.getReference().getEntityName(), p.getReference().getEntityName()));
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
    public static String createInsertQuery(Entity entity, Iterable<Object> objects, IColumnValue columnValue)
    {
        StringBuilder sb = new StringBuilder();
        for(Object obj : objects) {
            sb.append("insert into ");
            sb.append(entity.getName());
            sb.append("(");
            boolean isFirst = true;
            for (Property p : entity.getProperties()) {
                if (p.isPrimaryKey())
                    continue;
                if (!isFirst)
                    sb.append(", ");
                sb.append(p.getColumnName());
                isFirst = false;
            }
            sb.append(") values (");

            isFirst = true;
            for(Property p : entity.getProperties())
            {
                if(p.isPrimaryKey())
                    continue;

                if(!isFirst)
                    sb.append(", ");

                Object value = columnValue.getValue(obj, p);
                if(Objects.equals(p.getPropertyType().getCanonicalName(), "java.lang.String") && value != null) sb.append("'");
                if(value == null) sb.append("NULL");
                else sb.append(value.toString());
                if(Objects.equals(p.getPropertyType().getCanonicalName(), "java.lang.String") && value != null) sb.append("'");

                isFirst = false;
            }

            sb.append(")\n");
        }

        return sb.toString();
    }
}

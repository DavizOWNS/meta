package sk.tuke.mp.persistence.sql;

import sk.tuke.mp.persistence.model.ColumnType;

/**
 * Created by DAVID on 16.2.2017.
 */
public final class SqlTypeConverter {
    private SqlTypeConverter() {}

    public static String getSqlTypeName(ColumnType columnType)
    {
        switch(columnType)
        {
            case INT:
                return "INT";
            case DOUBLE:
                return "DOUBLE";
            case STRING:
                return "VARCHAR(50)";
            default:
                return null;
        }
    }
}

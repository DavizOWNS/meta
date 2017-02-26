package sk.tuke.mp.persistence.sql;

import sk.tuke.mp.persistence.infrastructure.Property;
import sk.tuke.mp.persistence.infrastructure.PropertyAnnotations;
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

    public static String getSqlTypeName(Property prop)
    {
        if(prop.getReference() != null)
            return "INT";

        switch(prop.getPropertyType().getCanonicalName())
        {
            case "java.lang.Integer":
                return "INT";
            case "java.lang.Double":
                return "DOUBLE";
            case "java.lang.String":
                Object maxLength = prop.getAnnotation(PropertyAnnotations.MAX_LENGTH);
                if(maxLength != null)
                    return "VARCHAR(" + (int)maxLength + ")";

                return "TEXT";
            default:
                return null;
        }
    }
}

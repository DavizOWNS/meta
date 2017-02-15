package sk.tuke.mp.persistence.model;

import java.util.List;

/**
 * Created by DAVID on 15.2.2017.
 */
public class Table {
    private String name;

    private List<Column> columns;

    public Table(String name, List<Column> columns) {
        this.name = name;
        this.columns = columns;

        boolean hasPrimary = false;
        for(Column c : columns)
        {
            if(c.isPrimaryKey())
            {
                if(hasPrimary)
                {
                    throw new IllegalArgumentException("Parameter columns contains more than one primary keys");
                }
                hasPrimary = true;
            }
        }
        if(!hasPrimary)
        {
            throw new IllegalArgumentException("Parameter columns does not contain primary key column");
        }
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
}

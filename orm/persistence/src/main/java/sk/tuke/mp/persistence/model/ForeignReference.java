package sk.tuke.mp.persistence.model;

/**
 * Created by DAVID on 15.2.2017.
 */
public class ForeignReference {
    private Table table;
    private Column column;

    public ForeignReference(Table table, Column column) {
        this.table = table;
        this.column = column;
    }

    public Table getTable() {
        return table;
    }

    public Column getColumn() {
        return column;
    }
}

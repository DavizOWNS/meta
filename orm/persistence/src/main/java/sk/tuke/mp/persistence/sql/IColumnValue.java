package sk.tuke.mp.persistence.sql;

import sk.tuke.mp.persistence.infrastructure.Property;
import sk.tuke.mp.persistence.model.Column;

/**
 * Created by DAVID on 16.2.2017.
 */
public interface IColumnValue {
    Object getValue(Object obj, Property property);
}

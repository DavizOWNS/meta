package sk.tuke.mp.persistence.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Created by DAVID on 22.2.2017.
 */
public class ColumnAnnotations {
    private Getter getter;
    private Setter setter;
    private Ignored ignored;
    private Column column;
    private Required required;

    public ColumnAnnotations(Field field)
    {
        for(Annotation annotation : field.getAnnotations())
        {
            if(annotation instanceof Getter)
                getter = (Getter)annotation;
            if(annotation instanceof Setter)
                setter = (Setter)annotation;
            if(annotation instanceof Ignored)
                ignored = (Ignored)annotation;
            if(annotation instanceof Column)
                column = (Column)annotation;
            if(annotation instanceof Required)
                required = (Required)annotation;
        }
    }

    public Getter getGetter() {
        return getter;
    }

    public Setter getSetter() {
        return setter;
    }

    public Ignored getIgnored() {
        return ignored;
    }

    public Column getColumn() {
        return column;
    }

    public Required getRequired() {
        return required;
    }
}

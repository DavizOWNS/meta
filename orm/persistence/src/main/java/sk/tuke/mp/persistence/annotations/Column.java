package sk.tuke.mp.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by DAVID on 22.2.2017.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name();
}

package sk.tuke.mp.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by DAVID on 22.2.2017.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";
    boolean required() default false;
    String getter() default "";
    String setter() default "";
    boolean unique() default false;
    int maxLength() default 50;
}

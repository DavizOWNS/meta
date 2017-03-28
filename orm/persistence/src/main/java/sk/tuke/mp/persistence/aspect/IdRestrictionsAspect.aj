package sk.tuke.mp.persistence.aspect;

/**
 * Created by DAVID on 28.3.2017.
 */
public aspect IdRestrictionsAspect {
    declare error
        : set(@sk.tuke.mp.persistence.annotations.Id private int *.*) && !within(sk.tuke.mp.persistence.ReflectivePersistenceManager)
        :"Writing directly to field annotated with @Id is prohibited";
}

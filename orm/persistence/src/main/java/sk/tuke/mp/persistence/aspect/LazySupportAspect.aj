package sk.tuke.mp.persistence.aspect;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;
import sk.tuke.mp.persistence.infrastructure.Entity;

/**
 * Created by DAVID on 29.3.2017.
 */
public aspect LazySupportAspect {

    Object around (ReflectivePersistenceManager manager, Entity entity, Class interfaceType, int id)
            :execution(* sk.tuke.mp.persistence.ReflectivePersistenceManager.getFromDb(..))
            && this(manager) && args(entity, interfaceType, id)
    {
        if(interfaceType != null)
        {
            System.out.println("Lazy of " + interfaceType.toString());
            System.out.println("\t at " + thisJoinPoint);

            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(interfaceType);
            enhancer.setCallback(new LazyLoader() {
                @Override
                public Object loadObject() throws Exception {
                    return proceed(manager, entity, interfaceType, id);
                }
            });

            return enhancer.create();
        }

        return proceed(manager, entity, interfaceType, id);
    }
}

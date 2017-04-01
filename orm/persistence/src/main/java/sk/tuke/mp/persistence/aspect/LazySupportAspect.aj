package sk.tuke.mp.persistence.aspect;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import sk.tuke.mp.persistence.ReflectivePersistenceManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by DAVID on 29.3.2017.
 */
public aspect LazySupportAspect {

    Object around (ReflectivePersistenceManager manager)
            :execution(* sk.tuke.mp.persistence.ReflectivePersistenceManager.get(..))
            && this(manager)
    {
        Class type = (Class) thisJoinPoint.getArgs()[0];
//        Method method = null;
//        try {
//            method = manager.getClass().getDeclaredMethod("getFromDb", Class.class, int.class);
//            method.setAccessible(true);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }

        if(type.isInterface())
        {
            System.out.println("Lazy...");
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            //Method finalMethod = method;
            enhancer.setCallback((LazyLoader) () -> {
                return proceed(manager);
                //System.out.println(result.toString());
                //return finalMethod.invoke(manager, thisJoinPoint.getArgs());
            });

            return enhancer.create();
        }

//        try {
//            System.out.println("Normal");
//            return method.invoke(manager, thisJoinPoint.getArgs());
//        } catch (IllegalAccessException | InvocationTargetException e) {
//            e.printStackTrace();
//            return null;
//        }
        return proceed(manager);
    }
}

package sk.tuke.mp.persistence.aspect;

import sk.tuke.mp.persistence.PersistenceException;
import sk.tuke.mp.persistence.PersistenceManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Stack;

/**
 * Created by DAVID on 28.3.2017.
 */
public aspect AutoSaveAspect {
    private Stack<WeakReference<PersistenceManager>> pmRefs;

    public AutoSaveAspect()
    {
        pmRefs = new Stack<>();
    }

    private PersistenceManager getPersistenceManager()
    {
        while(!pmRefs.isEmpty())
        {
            PersistenceManager result = pmRefs.peek().get();
            if(result != null)
                return result;
            pmRefs.pop();
        }

        return null;
    }

    after (Object obj)
            : set(@sk.tuke.mp.persistence.annotations.Column !static * *.*)
            && within(@sk.tuke.mp.persistence.annotations.Entity *)
            && !withincode(sk.tuke.mp.example.*.new(..))
            && target(obj)
    {
        System.out.println("AutoSave");
        try {
            getPersistenceManager().save(obj);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    after (Object obj) returning
            : initialization(public (@sk.tuke.mp.persistence.annotations.Entity *).new(..))
            && within(sk.tuke.mp.example.*)
            && target(obj)
    {
        System.out.println("AutoSave after constructor");
        try {
            getPersistenceManager().save(obj);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    after (Object obj)
            :initialization(sk.tuke.mp.persistence.PersistenceManager+.new(..))
            && target(obj)
    {
        pmRefs.push(new WeakReference<PersistenceManager>((PersistenceManager) obj));
    }

    Object around ()
            :execution(@sk.tuke.mp.persistence.annotations.Transaction * *.*(..))
    {
        System.out.println("Creating transaction around " + thisJoinPoint);

        Connection conn = null;
        try {
            Field f = getPersistenceManager().getClass().getDeclaredField("connection");
            f.setAccessible(true);
            conn = (Connection) f.get(getPersistenceManager());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();

            return proceed();
        }

        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            return proceed();
        }

        Object ret = proceed();

        try
        {
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ret;
    }
}

package sk.tuke.mp.persistence.aspect;

import sk.tuke.mp.persistence.PersistenceException;
import sk.tuke.mp.persistence.PersistenceManager;

import java.lang.ref.WeakReference;

/**
 * Created by DAVID on 28.3.2017.
 */
public aspect AutoSaveAspect {
    private WeakReference<PersistenceManager> pmRef;

    after (Object obj)
            : set(@sk.tuke.mp.persistence.annotations.Column !static * *.*)
            && !withincode(sk.tuke.mp.example.*.new(..))
            && target(obj)
            {
                try {
                    pmRef.get().save(obj);
                } catch (PersistenceException e) {
                    e.printStackTrace();
                }
            }

    after (Object obj)
            :initialization(sk.tuke.mp.persistence.PersistenceManager+.new(..))
            && !within(IdRestrictionsAspect)
            && target(obj)
            {
                pmRef = new WeakReference<PersistenceManager>((PersistenceManager) obj);
            }
}

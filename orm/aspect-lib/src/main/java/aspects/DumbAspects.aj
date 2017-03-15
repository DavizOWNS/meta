package aspects;

import annotations.Dumb;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Created by DAVID on 27.2.2017.
 */
@Aspect
public aspect DumbAspects {

    @Before("execution(* *.*(..)) && @annotation(dumb)")
    public void aroundPersonGetter(JoinPoint point, Dumb dumb){
        System.out.println("Calling dumb method");
        //System.out.println(point.toString());
        //System.out.println("ASPECT: " + point.getSignature().toString());
    }
}

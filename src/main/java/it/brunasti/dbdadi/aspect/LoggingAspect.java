package it.brunasti.dbdadi.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Component
@Aspect
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);
    private long iterationCounter = 0;

    @Pointcut("@annotation(Loggable)")
    public void executeLogging(){
        //do nothing
    }

    private void logStartMethodCall(ProceedingJoinPoint joinPoint, long myIteration) {
        try {
            StringBuilder message = new StringBuilder("ITER=").append(myIteration);
            message.append(" Method=");
            if (ProceedingJoinPoint.METHOD_EXECUTION.equals(joinPoint.getKind())) {
                MethodInvocationProceedingJoinPoint methodInvocationJoinPoint = (MethodInvocationProceedingJoinPoint) joinPoint;
                message.append(methodInvocationJoinPoint.getSignature().getDeclaringTypeName()).append(".");
            }
            message.append(joinPoint.getSignature().getName());
            message.append(" - START");
            Object[] args = joinPoint.getArgs();
            if (null != args && args.length > 0) {
                message.append(" | args=[ ");
                Arrays.asList(args).forEach(arg -> message.append(arg).append(" | "));
                message.append("]");
            }
            LOGGER.info(message.toString().replace("| ]", "]"));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            LOGGER.error(ex.getMessage());
        }
    }

    private void logEndMethodCall(ProceedingJoinPoint joinPoint, long myIteration, long startTime, Object returnValue) {
        try {
            long totalTime = System.currentTimeMillis() - startTime;
            StringBuilder message = new StringBuilder("ITER=").append(myIteration);
            message.append(" Method=");
            if (ProceedingJoinPoint.METHOD_EXECUTION.equals(joinPoint.getKind())) {
                MethodInvocationProceedingJoinPoint methodInvocationJoinPoint = (MethodInvocationProceedingJoinPoint) joinPoint;
                message.append(methodInvocationJoinPoint.getSignature().getDeclaringTypeName()).append(".");
            }
            message.append(joinPoint.getSignature().getName());
            message.append(" - END  ");
            message.append( " | totalTime: ").append(totalTime).append("ms ");
            Object[]args = joinPoint.getArgs();
            if(null != args && args.length>0){
                message.append(" | args=[ ");
                Arrays.asList(args).forEach(arg-> message.append(arg).append(" | "));
                message.append("]");
            }
            if (returnValue != null) {
              if(returnValue instanceof Collection<?>){
                  message.append(" returning: Collection of ").append(((Collection)returnValue).size()).append(" instances");
              }else{
                  message.append(", returning: ").append(returnValue.toString());
              }
            } else {
              message.append(", returning null");
            }
            LOGGER.info(message.toString().replace("| ]", "]"));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            LOGGER.error(ex.getMessage());
        }
    }

    @Around("executeLogging()")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        iterationCounter ++;
        long myIteration = iterationCounter;

        logStartMethodCall(joinPoint, myIteration);
        long startTime = System.currentTimeMillis();
        Object returnValue = joinPoint.proceed();
        logEndMethodCall(joinPoint, myIteration, startTime, returnValue);
        return returnValue;
    }
}

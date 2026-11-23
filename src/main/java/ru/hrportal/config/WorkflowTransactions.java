package ru.hrportal.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class WorkflowTransactions {
    private final DatabaseWorkflow workflow;

    public WorkflowTransactions(DatabaseWorkflow workflow) {
        this.workflow = workflow;
    }

    @Around(
            "execution(public * ru.hrportal.controller..*(..)) || execution(public *"
                    + " ru.hrportal.service..*(..))")
    public Object execute(ProceedingJoinPoint call) {
        return workflow.execute(
                () -> {
                    try {
                        return call.proceed();
                    } catch (RuntimeException | Error failure) {
                        throw failure;
                    } catch (Throwable failure) {
                        throw new IllegalStateException(failure);
                    }
                });
    }
}

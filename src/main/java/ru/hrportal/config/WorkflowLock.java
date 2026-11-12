package ru.hrportal.config;

import org.springframework.stereotype.Component;

/**
 * One shared lock makes cross-repository transitions atomic in the in-memory prototype.
 */
@Component
public class WorkflowLock {
}

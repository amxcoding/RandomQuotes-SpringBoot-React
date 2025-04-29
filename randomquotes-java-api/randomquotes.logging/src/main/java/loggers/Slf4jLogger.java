package loggers;

import interfaces.common.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Objects;

/**
 * SLF4J based implementation of the ILogger interface.
 * This implementation gets the logger instance for the class where it's injected.
 */
public class Slf4jLogger implements ILogger {

    private final Logger logger;

    private Slf4jLogger(Logger logger) {
        this.logger = logger;
    }

    @Configuration
    static class LoggingConfiguration {
        @Bean
        @Scope("prototype") // Ensures a new logger instance per injection point
        public ILogger produceLogger(InjectionPoint injectionPoint) {
            // Get the class where the logger is being injected
            Class<?> declaringClass = Objects.requireNonNull(injectionPoint.getMethodParameter()).getContainingClass();
            // Create an SLF4J logger for that specific class
            return new Slf4jLogger(LoggerFactory.getLogger(declaringClass));
        }
    }


    @Override
    public void debug(String title, String message, Object... params) {
        logger.debug("[{}] - " + message, title, params);
    }

    @Override
    public void info(String title, String message, Object... params) {
        logger.info("[{}] - " + message, title, params);
    }

    @Override
    public void warn(String title, String message, Object... params) {
        logger.warn("[{}] - " + message, title, params);
    }

    @Override
    public void error(String title, String message,Object... params) {
        logger.error("[{}] - " + message, title, params);;
    }
}
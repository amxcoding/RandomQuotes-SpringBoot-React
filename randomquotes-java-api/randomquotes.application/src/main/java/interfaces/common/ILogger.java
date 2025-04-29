package interfaces.common;

public interface ILogger {

    void debug(String title, String message, Object... params);
    void info(String title, String message, Object... params);
    void warn(String title, String message, Object... params);
    void error(String title, String message, Object... params);
}

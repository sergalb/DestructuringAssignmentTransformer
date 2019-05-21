package main;

public class UnsupportedStatementException extends RuntimeException {

    public UnsupportedStatementException() {
        super();
    }

    public UnsupportedStatementException(String message) {
        super(message);
    }

    public UnsupportedStatementException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedStatementException(Throwable cause) {
        super(cause);
    }
}

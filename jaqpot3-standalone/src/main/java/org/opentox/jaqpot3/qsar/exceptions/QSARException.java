package org.opentox.jaqpot3.qsar.exceptions;

/**
 *
 * @author chung
 */
public class QSARException extends Exception {

    /**
     * Creates a new instance of <code>QSARException</code> without detail message.
     */
    public QSARException() {
    }

    /**
     * Constructs an instance of <code>QSARException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public QSARException(String msg) {
        super(msg);
    }

    public QSARException(Throwable cause) {
        super(cause);
    }

    public QSARException(String message, Throwable cause) {
        super(message, cause);
    }
}

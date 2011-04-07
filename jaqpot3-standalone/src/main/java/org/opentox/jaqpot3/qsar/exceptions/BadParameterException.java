package org.opentox.jaqpot3.qsar.exceptions;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class BadParameterException extends QSARException {

    private String details;

    public BadParameterException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public BadParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadParameterException(String message, String details) {
        super(message);
        this.details = details;
    }

    public BadParameterException(Throwable cause) {
        super(cause);
    }

    public BadParameterException(String msg) {
        super(msg);
    }

    public BadParameterException() {
    }

    public String getDetails() {
        return details;
    }


}

package io.github.zerroukzakaria.civicdata.importer;

/** Raised when an import file is malformed or fails validation. */
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
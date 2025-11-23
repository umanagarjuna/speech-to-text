package com.zoominfo.stt.exception;

/**
 * Exception thrown when the Vosk model is not properly loaded
 */
public class ModelNotLoadedException extends TranscriptionException {
    public ModelNotLoadedException(String message) {
        super(message);
    }

    public ModelNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }
}

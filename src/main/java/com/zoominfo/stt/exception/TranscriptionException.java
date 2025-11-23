package com.zoominfo.stt.exception;

/**
 * Base exception for transcription-related errors
 */
public class TranscriptionException extends RuntimeException {
    public TranscriptionException(String message) {
        super(message);
    }

    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

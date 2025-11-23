package com.zoominfo.stt.exception;

/**
 * Exception thrown when an unsupported audio format is provided
 */
public class InvalidAudioFormatException extends TranscriptionException {
    public InvalidAudioFormatException(String message) {
        super(message);
    }
}

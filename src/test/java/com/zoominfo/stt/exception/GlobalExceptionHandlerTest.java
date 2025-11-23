package com.zoominfo.stt.exception;

import com.zoominfo.stt.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(webRequest.getDescription(anyBoolean())).thenReturn("uri=/api/test");
    }

    @Test
    void handleInvalidAudioFormat_ReturnsBadRequest() {
        InvalidAudioFormatException ex = new InvalidAudioFormatException("Invalid format");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidAudioFormat(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Invalid Audio Format");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid format");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleModelNotLoaded_ReturnsServiceUnavailable() {
        ModelNotLoadedException ex = new ModelNotLoadedException("Model missing");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleModelNotLoaded(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getMessage()).contains("Transcription model is not available");
    }

    @Test
    void handleTranscriptionException_ReturnsInternalServerError() {
        TranscriptionException ex = new TranscriptionException("Processing failed");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTranscriptionException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().getError()).isEqualTo("Transcription Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Processing failed");
    }

    @Test
    void handleMaxUploadSizeExceeded_ReturnsPayloadTooLarge() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1000L);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceeded(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getBody().getError()).isEqualTo("File Too Large");
    }

    @Test
    void handleIllegalArgument_ReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad argument");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Bad argument");
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
    }
}

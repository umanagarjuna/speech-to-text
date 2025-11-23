package com.zoominfo.stt.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void of_CreatesErrorResponseWithTimestamp() {
        ErrorResponse error = ErrorResponse.of(400, "Bad Request", "Invalid Input", "/api/test");

        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getError()).isEqualTo("Bad Request");
        assertThat(error.getMessage()).isEqualTo("Invalid Input");
        assertThat(error.getPath()).isEqualTo("/api/test");
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void builder_CreatesObjectCorrectly() {
        Instant now = Instant.now();
        ErrorResponse error = ErrorResponse.builder()
                .status(500)
                .error("Internal Error")
                .message("Something went wrong")
                .path("/api/error")
                .timestamp(now)
                .build();

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getError()).isEqualTo("Internal Error");
        assertThat(error.getTimestamp()).isEqualTo(now);
    }

    @Test
    void setters_WorkCorrectly() {
        ErrorResponse error = new ErrorResponse();
        error.setStatus(404);
        error.setError("Not Found");

        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getError()).isEqualTo("Not Found");
    }
}

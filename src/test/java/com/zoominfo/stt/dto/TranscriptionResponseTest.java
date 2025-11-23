package com.zoominfo.stt.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionResponseTest {

    @Test
    void builder_CreatesObjectCorrectly() {
        Instant now = Instant.now();
        TranscriptionResponse response = TranscriptionResponse.builder()
                .transcript("Hello World")
                .processingTimeMs(100L)
                .filename("test.wav")
                .timestamp(now)
                .build();

        assertThat(response.getTranscript()).isEqualTo("Hello World");
        assertThat(response.getProcessingTimeMs()).isEqualTo(100L);
        assertThat(response.getFilename()).isEqualTo("test.wav");
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    void noArgsConstructor_CreatesEmptyObject() {
        TranscriptionResponse response = new TranscriptionResponse();
        assertThat(response).isNotNull();
        response.setTranscript("Test");
        response.setProcessingTimeMs(50L);

        assertThat(response.getTranscript()).isEqualTo("Test");
        assertThat(response.getProcessingTimeMs()).isEqualTo(50L);
    }
}

package com.zoominfo.stt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response object for transcription requests.
 * 
 * Contains the full transcript text, confidence score,
 * and optional detailed word-level results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranscriptionResponse {

    /**
     * The complete transcribed text
     */
    private String transcript;

    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;

    /**
     * Timestamp when the transcription was completed
     */
    private Instant timestamp;

    /**
     * Original filename (if provided)
     */
    private String filename;
}

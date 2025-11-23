package com.zoominfo.stt.controller;

import com.zoominfo.stt.dto.TranscriptionResponse;
import com.zoominfo.stt.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for the Speech-to-Text API.
 * 
 * Provides endpoints for:
 * - Health checks
 * - Audio transcription
 * - Service information
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transcribe")
@RequiredArgsConstructor
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    /**
     * Health check endpoint.
     * 
     * Returns the service status and readiness.
     * 
     * @return Service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        boolean isReady = transcriptionService.isReady();
        
        health.put("status", isReady ? "UP" : "DOWN");
        health.put("service", "speech-to-text");
        health.put("ready", isReady);
        health.put("timestamp", System.currentTimeMillis());
        
        if (isReady) {
            health.put("supportedFormats", transcriptionService.getSupportedFormats());
        }
        
        HttpStatus status = isReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }

    /**
     * Transcribe an audio file.
     * 
     * Accepts an audio file upload and returns the transcribed text.
     * Supported formats: WAV, MP3, FLAC, OGG, M4A
     * 
     * Note: For optimal results, audio should be:
     * - 16 kHz sample rate
     * - 16-bit depth
     * - Mono channel
     * 
     * The service will attempt to process other formats but results may vary.
     * 
     * @param file Audio file to transcribe
     * @return Transcription response with text and metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received transcription request for file: {} (size: {} bytes)", 
                file.getOriginalFilename(), file.getSize());
        
        TranscriptionResponse response = transcriptionService.transcribe(file);
        
        log.info("Transcription successful: {} characters in {} ms",
                response.getTranscript().length(), response.getProcessingTimeMs());
        
        return ResponseEntity.ok(response);
    }
}

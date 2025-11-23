package com.zoominfo.stt.controller;

import com.zoominfo.stt.dto.TranscriptionResponse;
import com.zoominfo.stt.exception.InvalidAudioFormatException;
import com.zoominfo.stt.exception.ModelNotLoadedException;
import com.zoominfo.stt.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TranscriptionController.
 * * Tests the REST API endpoints using MockMvc.
 */
@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscriptionService transcriptionService;

    @Test
    void testHealth_WhenServiceReady_ReturnsOk() throws Exception {
        when(transcriptionService.isReady()).thenReturn(true);
        when(transcriptionService.getSupportedFormats())
                .thenReturn(Set.of("wav", "mp3", "flac"));

        mockMvc.perform(get("/api/v1/transcribe/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.service").value("speech-to-text"))
                .andExpect(jsonPath("$.supportedFormats").isArray());
    }

    @Test
    void testHealth_WhenServiceNotReady_ReturnsServiceUnavailable() throws Exception {
        when(transcriptionService.isReady()).thenReturn(false);

        mockMvc.perform(get("/api/v1/transcribe/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.ready").value(false));
    }

    @Test
    void testTranscribe_WithValidFile_ReturnsTranscription() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.wav",
                "audio/wav",
                "fake audio data".getBytes()
        );

        TranscriptionResponse mockResponse = TranscriptionResponse.builder()
                .transcript("This is a test transcription")
                .processingTimeMs(1500L)
                .filename("test.wav")
                .timestamp(Instant.now())
                .build();

        when(transcriptionService.transcribe(any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transcript").value("This is a test transcription"))
                .andExpect(jsonPath("$.processingTimeMs").value(1500))
                .andExpect(jsonPath("$.filename").value("test.wav"));
    }

    @Test
    void testTranscribe_WithInvalidFormat_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not audio".getBytes()
        );

        when(transcriptionService.transcribe(any()))
                .thenThrow(new InvalidAudioFormatException("Unsupported format"));

        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Audio Format"));
    }

    @Test
    void testTranscribe_WhenModelNotLoaded_ReturnsServiceUnavailable() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.wav",
                "audio/wav",
                "fake audio".getBytes()
        );

        when(transcriptionService.transcribe(any()))
                .thenThrow(new ModelNotLoadedException("Model not loaded"));

        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }
}
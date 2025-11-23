package com.zoominfo.stt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Configuration properties for the transcription service.
 * 
 * These properties control model location, audio processing parameters,
 * and resource limits.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "transcription")
public class TranscriptionProperties {

    /**
     * Path to the Vosk model directory
     */
    @NotBlank
    private String modelPath;

    /**
     * Audio sample rate in Hz (Vosk typically uses 16000 Hz)
     */
    @NotNull
    @Min(8000)
    private Integer sampleRate;

    /**
     * Supported audio formats (comma-separated)
     */
    @NotNull
    private String supportedFormats;

    /**
     * Temporary directory for audio file processing
     */
    @NotBlank
    private String tempDirectory;

    /**
     * Get supported audio formats as a Set
     */
    public Set<String> getSupportedFormatsSet() {
        return Set.of(supportedFormats.toLowerCase().split(","));
    }

    /**
     * Check if a given file extension is supported
     */
    public boolean isSupportedFormat(String extension) {
        return getSupportedFormatsSet().contains(extension.toLowerCase().replace(".", ""));
    }
}

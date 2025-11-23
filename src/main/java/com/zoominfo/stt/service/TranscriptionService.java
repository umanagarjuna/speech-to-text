package com.zoominfo.stt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoominfo.stt.config.TranscriptionProperties;
import com.zoominfo.stt.dto.TranscriptionResponse;
import com.zoominfo.stt.exception.InvalidAudioFormatException;
import com.zoominfo.stt.exception.ModelNotLoadedException;
import com.zoominfo.stt.exception.TranscriptionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for audio transcription using the Vosk library.
 * This service handles:
 * - Model loading and lifecycle management
 * - Audio file validation
 * - Audio file conversion (using FFmpeg)
 * - Speech-to-text conversion
 * - Result formatting
 * Thread-safety: This service uses synchronized blocks for critical sections
 * to ensure safe concurrent access to the Vosk model.
 */
@Slf4j
@Service
public class TranscriptionService {

    private final TranscriptionProperties properties;
    private final ObjectMapper objectMapper;
    private Model model;
    private volatile boolean modelLoaded = false;

    public TranscriptionService(TranscriptionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize the Vosk model on application startup.
     */
    @PostConstruct
    public void initializeModel() {
        try {
            log.info("Loading Vosk model from: {}", properties.getModelPath());
            long startTime = System.currentTimeMillis();

            File modelFile = new File(properties.getModelPath());
            if (!modelFile.exists()) {
                throw new ModelNotLoadedException(
                        "Model directory not found: " + properties.getModelPath());
            }

            this.model = createModel(properties.getModelPath());
            this.modelLoaded = true;

            long loadTime = System.currentTimeMillis() - startTime;
            log.info("Vosk model loaded successfully in {} ms", loadTime);

        } catch (Exception e) {
            log.error("Failed to load Vosk model", e);
            throw new ModelNotLoadedException("Failed to initialize transcription model", e);
        }
    }

    /**
     * Clean up resources on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (model != null) {
            try {
                model.close();
                log.info("Vosk model closed successfully");
            } catch (Exception e) {
                log.warn("Error closing Vosk model", e);
            }
        }
    }

    /**
     * Transcribe audio from a multipart file upload.
     */
    public TranscriptionResponse transcribe(MultipartFile file) {
        validateAudioFile(file);

        if (!modelLoaded) {
            throw new ModelNotLoadedException("Transcription model is not loaded");
        }

        Path originalTempFile = null;
        Path convertedTempFile = null;
        try {
            originalTempFile = saveTempFile(file);

            log.debug("Converting {} to 16kHz mono WAV for transcription...", file.getOriginalFilename());
            convertedTempFile = convertAudioToWav(originalTempFile);
            log.debug("Conversion successful: {}", convertedTempFile);

            long startTime = System.currentTimeMillis();
            String transcript = performTranscription(convertedTempFile);
            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Transcription completed in {} ms for file: {}",
                    processingTime, file.getOriginalFilename());

            return TranscriptionResponse.builder()
                    .transcript(transcript)
                    .processingTimeMs(processingTime)
                    .filename(file.getOriginalFilename())
                    .timestamp(Instant.now())
                    .build();

        } catch (IOException | InterruptedException e) {
            log.error("Error processing audio file", e);
            // Determine if this was an interrupt or IO error, mostly generic here
            throw new TranscriptionException("Failed to process audio file", e);
        } finally {
            cleanupTempFile(convertedTempFile);
            cleanupTempFile(originalTempFile);
        }
    }

    /**
     * Transcribe audio from a file path.
     */
    public TranscriptionResponse transcribeFile(String audioFilePath) {
        Path path = Paths.get(audioFilePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Audio file not found: " + audioFilePath);
        }

        if (!modelLoaded) {
            throw new ModelNotLoadedException("Transcription model is not loaded");
        }

        Path convertedTempFile = null;
        try {
            log.debug("Converting {} to 16kHz mono WAV for transcription...", audioFilePath);
            convertedTempFile = convertAudioToWav(path);
            log.debug("Conversion successful: {}", convertedTempFile);

            long startTime = System.currentTimeMillis();
            String transcript = performTranscription(convertedTempFile);
            long processingTime = System.currentTimeMillis() - startTime;

            return TranscriptionResponse.builder()
                    .transcript(transcript)
                    .processingTimeMs(processingTime)
                    .filename(path.getFileName().toString())
                    .timestamp(Instant.now())
                    .build();

        } catch (IOException | InterruptedException e) {
            log.error("Error processing audio file from path: {}", audioFilePath, e);
            throw new TranscriptionException("Failed to process audio file", e);
        } finally {
            cleanupTempFile(convertedTempFile);
        }
    }

    /**
     * Method to convert any audio file to 16kHz mono WAV using FFmpeg
     */
    protected Path convertAudioToWav(Path inputFile) throws IOException, InterruptedException {
        Path tempDir = Paths.get(properties.getTempDirectory());
        Path outputFile = Files.createTempFile(tempDir, "converted_", ".wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.toAbsolutePath().toString(),
                "-ar", String.valueOf(properties.getSampleRate()), // 16000
                "-ac", "1", // Mono
                "-c:a", "pcm_s16le", // 16-bit PCM
                "-y", // Overwrite output file
                outputFile.toAbsolutePath().toString());

        pb.redirectErrorStream(true);
        Process process = startProcess(pb);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg: {}", line);
            }
        }

        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroy();
            throw new TranscriptionException("FFmpeg conversion timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            // ⭐️ FIX: This is the key change.
            // Throwing InvalidAudioFormatException triggers the 400 Bad Request in GlobalExceptionHandler
            log.warn("FFmpeg exited with code {}. The input file is likely corrupted or invalid.", exitCode);
            throw new InvalidAudioFormatException(
                    "Audio conversion failed. The file may be corrupted, password-protected, or in an unsupported format."
            );
        }

        return outputFile;
    }

    // Protected method to allow mocking ProcessBuilder
    protected Process startProcess(ProcessBuilder pb) throws IOException {
        return pb.start();
    }

    /**
     * Perform the actual transcription using Vosk.
     */
    protected synchronized String performTranscription(Path audioFile) throws IOException {
        try (AudioInputStream ais = getAudioInputStream(audioFile.toFile());
             Recognizer recognizer = createRecognizer(model, (float) properties.getSampleRate())) {

            log.debug("Starting transcription for file: {}", audioFile.getFileName());

            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder fullTranscript = new StringBuilder();

            while ((bytesRead = ais.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String result = recognizer.getResult();
                    String partialTranscript = extractText(result);
                    if (!partialTranscript.isEmpty()) {
                        fullTranscript.append(partialTranscript).append(" ");
                    }
                }
            }

            String finalResult = recognizer.getFinalResult();
            String finalTranscript = extractText(finalResult);
            if (!finalTranscript.isEmpty()) {
                fullTranscript.append(finalTranscript);
            }

            String transcript = fullTranscript.toString().trim();
            log.debug("Transcription complete. Length: {} characters", transcript.length());

            return transcript;

        } catch (UnsupportedAudioFileException e) {
            log.error("Unsupported audio file format (post-conversion): {}", audioFile.getFileName(), e);
            throw new InvalidAudioFormatException("Unsupported audio file format");
        } catch (Exception e) {
            log.error("Error during Vosk transcription", e);
            throw new TranscriptionException("Transcription processing failed", e);
        }
    }

    private String extractText(String jsonResult) {
        try {
            JsonNode node = objectMapper.readTree(jsonResult);
            if (node.has("text")) {
                return node.get("text").asText();
            }
            return "";
        } catch (Exception e) {
            log.warn("Failed to parse Vosk result: {}", jsonResult, e);
            return "";
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Filename is missing");
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        if (!properties.isSupportedFormat(extension)) {
            throw new InvalidAudioFormatException(
                    String.format("Unsupported audio format: %s. Supported formats: %s",
                            extension, properties.getSupportedFormats()));
        }

        log.debug("Audio file validated: {} ({})", originalFilename, file.getSize());
    }

    private Path saveTempFile(MultipartFile file) throws IOException {
        Path tempDir = Paths.get(properties.getTempDirectory());
        Files.createDirectories(tempDir);

        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);

        Path tempFile = Files.createTempFile(tempDir, "upload_", "." + extension);
        file.transferTo(tempFile);

        log.debug("Saved temp file: {}", tempFile);
        return tempFile;
    }

    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                log.debug("Cleaned up temp file: {}", tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }

    /**
     * Check if the service is ready to process requests
     */
    public boolean isReady() {
        return modelLoaded && model != null;
    }

    /**
     * Get supported formats from properties
     */
    public Set<String> getSupportedFormats() {
        return properties.getSupportedFormatsSet();
    }

    protected Model createModel(String modelPath) throws IOException {
        return new Model(modelPath);
    }

    protected Recognizer createRecognizer(Model model, float sampleRate) throws IOException {
        return new Recognizer(model, sampleRate);
    }

    protected AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(file);
    }
}
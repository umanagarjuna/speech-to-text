package com.zoominfo.stt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoominfo.stt.config.TranscriptionProperties;
import com.zoominfo.stt.dto.TranscriptionResponse;
import com.zoominfo.stt.exception.InvalidAudioFormatException;
import com.zoominfo.stt.exception.ModelNotLoadedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

    @Mock
    private TranscriptionProperties properties;

    @Mock
    private Model model;

    @Mock
    private Recognizer recognizer;

    @Mock
    private Process process;

    private TranscriptionService transcriptionService;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Create a spy of the service to mock protected methods
        transcriptionService = spy(new TranscriptionService(properties, objectMapper));

        lenient().when(properties.getSampleRate()).thenReturn(16000);
        lenient().when(properties.getSupportedFormats()).thenReturn("wav,mp3,flac,ogg,m4a");
        lenient().when(properties.getTempDirectory()).thenReturn(tempDir.toString());
        lenient().when(properties.isSupportedFormat(anyString())).thenAnswer(inv -> {
            String ext = inv.getArgument(0, String.class);
            return "wav".equals(ext) || "mp3".equals(ext);
        });
    }

    @Test
    void initializeModel_WhenDirectoryExists_LoadsModel() throws IOException {
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath);
        when(properties.getModelPath()).thenReturn(modelPath.toString());

        doReturn(model).when(transcriptionService).createModel(anyString());

        transcriptionService.initializeModel();

        assertThat(transcriptionService.isReady()).isTrue();
        verify(transcriptionService).createModel(modelPath.toString());
    }

    @Test
    void initializeModel_WhenDirectoryDoesNotExist_ThrowsException() {
        when(properties.getModelPath()).thenReturn("/non/existent/path");

        assertThatThrownBy(() -> transcriptionService.initializeModel())
                .isInstanceOf(ModelNotLoadedException.class);
    }

    @Test
    void transcribe_HappyPath_ReturnsTranscript() throws Exception {
        // Setup model
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath);
        when(properties.getModelPath()).thenReturn(modelPath.toString());
        doReturn(model).when(transcriptionService).createModel(anyString());
        transcriptionService.initializeModel();

        // Mock convertAudioToWav to return a dummy path
        Path convertedPath = tempDir.resolve("converted.wav");
        Files.createFile(convertedPath);
        doReturn(convertedPath).when(transcriptionService).convertAudioToWav(any());

        // Mock performTranscription
        doReturn("Final Transcript").when(transcriptionService).performTranscription(any());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.wav", "audio/wav", "audio data".getBytes());

        TranscriptionResponse response = transcriptionService.transcribe(file);

        assertThat(response.getTranscript()).isEqualTo("Final Transcript");
        assertThat(response.getFilename()).isEqualTo("test.wav");
        verify(transcriptionService).convertAudioToWav(any());
        verify(transcriptionService).performTranscription(convertedPath);
    }

    @Test
    void transcribeFile_HappyPath_ReturnsTranscript() throws Exception {
        // Setup model
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath);
        when(properties.getModelPath()).thenReturn(modelPath.toString());
        doReturn(model).when(transcriptionService).createModel(anyString());
        transcriptionService.initializeModel();

        // Mock convertAudioToWav
        Path convertedPath = tempDir.resolve("converted.wav");
        Files.createFile(convertedPath);
        doReturn(convertedPath).when(transcriptionService).convertAudioToWav(any());

        // Mock performTranscription
        doReturn("File Transcript").when(transcriptionService).performTranscription(any());

        Path inputFile = tempDir.resolve("input.wav");
        Files.createFile(inputFile);

        TranscriptionResponse response = transcriptionService.transcribeFile(inputFile.toString());

        assertThat(response.getTranscript()).isEqualTo("File Transcript");
        verify(transcriptionService).convertAudioToWav(any());
        verify(transcriptionService).performTranscription(convertedPath);
    }

    @Test
    void convertAudioToWav_ExecutesFfmpeg() throws Exception {
        // Setup process mock
        when(process.waitFor(anyLong(), any())).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("ffmpeg output".getBytes()));
        doReturn(process).when(transcriptionService).startProcess(any());

        Path inputFile = tempDir.resolve("input.mp3");
        Files.createFile(inputFile);

        Path result = transcriptionService.convertAudioToWav(inputFile);

        assertThat(result).exists();
        assertThat(result.toString()).endsWith(".wav");
        verify(transcriptionService).startProcess(any());
    }

    @Test
    void performTranscription_ReadsAudioAndReturnsText() throws Exception {
        // Setup model
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath);
        when(properties.getModelPath()).thenReturn(modelPath.toString());
        doReturn(model).when(transcriptionService).createModel(anyString());
        transcriptionService.initializeModel();

        // Setup Recognizer
        doReturn(recognizer).when(transcriptionService).createRecognizer(any(), anyFloat());
        when(recognizer.acceptWaveForm(any(byte[].class), anyInt())).thenReturn(true);
        when(recognizer.getResult()).thenReturn("{\"text\": \"partial\"}");
        when(recognizer.getFinalResult()).thenReturn("{\"text\": \" final\"}");

        // Mock getAudioInputStream
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[100]); // dummy audio data
        AudioInputStream ais = new AudioInputStream(bais, format, 100);
        doReturn(ais).when(transcriptionService).getAudioInputStream(any(File.class));

        Path wavFile = tempDir.resolve("test.wav");
        Files.createFile(wavFile); // just needs to exist as a file

        String result = transcriptionService.performTranscription(wavFile);

        assertThat(result).contains("partial", "final");
        verify(transcriptionService).getAudioInputStream(wavFile.toFile());
    }

    @Test
    void cleanup_ClosesModel() throws IOException {
        // Setup model loading
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath); // Added this line to create the directory
        when(properties.getModelPath()).thenReturn(modelPath.toString());
        doReturn(model).when(transcriptionService).createModel(anyString());

        transcriptionService.initializeModel(); // sets model
        transcriptionService.cleanup();
        verify(model).close();
    }

    @Test
    void cleanup_WhenException_LogsError() throws IOException {
        // Setup model loading
        Path modelPath = tempDir.resolve("model");
        Files.createDirectories(modelPath); // Added this line to create the directory
        when(properties.getModelPath()).thenReturn(modelPath.toString());
        doReturn(model).when(transcriptionService).createModel(anyString());

        transcriptionService.initializeModel();
        doThrow(new RuntimeException("Close error")).when(model).close();
        transcriptionService.cleanup(); // Should not throw
        verify(model).close();
    }

    @Test
    void transcribe_WhenFileEmpty_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", new byte[0]);
        assertThatThrownBy(() -> transcriptionService.transcribe(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audio file is empty");
    }

    @Test
    void transcribe_WhenFilenameMissing_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "", "audio/wav", "data".getBytes());
        assertThatThrownBy(() -> transcriptionService.transcribe(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is missing");
    }

    @Test
    void transcribe_WhenFormatUnsupported_ThrowsException() {
        lenient().when(properties.isSupportedFormat("xyz")).thenReturn(false);
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "audio/xyz", "data".getBytes());

        assertThatThrownBy(() -> transcriptionService.transcribe(file))
                .isInstanceOf(InvalidAudioFormatException.class);
    }

    @Test
    void transcribe_WhenModelNotLoaded_ThrowsException() {
        // Model not initialized
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", "data".getBytes());
        assertThatThrownBy(() -> transcriptionService.transcribe(file))
                .isInstanceOf(ModelNotLoadedException.class);
    }

    @Test
    void transcribeFile_WhenFileDoesNotExist_ThrowsException() {
        assertThatThrownBy(() -> transcriptionService.transcribeFile("/non/existent/file.wav"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
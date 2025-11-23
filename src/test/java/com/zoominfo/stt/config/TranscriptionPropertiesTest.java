package com.zoominfo.stt.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionPropertiesTest {

    private TranscriptionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TranscriptionProperties();
        properties.setSupportedFormats("wav,mp3,flac,ogg");
    }

    @Test
    void getSupportedFormatsSet_ReturnsCorrectSet() {
        Set<String> formats = properties.getSupportedFormatsSet();
        assertThat(formats).containsExactlyInAnyOrder("wav", "mp3", "flac", "ogg");
    }

    @Test
    void getSupportedFormatsSet_HandlesMixedCase() {
        properties.setSupportedFormats("WAV,Mp3,FLAC");
        Set<String> formats = properties.getSupportedFormatsSet();
        assertThat(formats).containsExactlyInAnyOrder("wav", "mp3", "flac");
    }

    @Test
    void isSupportedFormat_WithSupportedFormat_ReturnsTrue() {
        assertThat(properties.isSupportedFormat("wav")).isTrue();
        assertThat(properties.isSupportedFormat("MP3")).isTrue();
        assertThat(properties.isSupportedFormat(".flac")).isTrue();
    }

    @Test
    void isSupportedFormat_WithUnsupportedFormat_ReturnsFalse() {
        assertThat(properties.isSupportedFormat("txt")).isFalse();
        assertThat(properties.isSupportedFormat("exe")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "wav", "mp3", "flac", "ogg" })
    void isSupportedFormat_WithAllConfiguredFormats_ReturnsTrue(String format) {
        assertThat(properties.isSupportedFormat(format)).isTrue();
    }
}

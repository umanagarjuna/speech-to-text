package com.zoominfo.stt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application class for the Speech-to-Text Service.
 * 
 * This service provides open-source speech recognition capabilities
 * using the Vosk library, exposing a REST API for audio transcription.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpeechToTextApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeechToTextApplication.class, args);
    }
}

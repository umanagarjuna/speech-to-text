package com.zoominfo.stt;

import com.zoominfo.stt.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class SpeechToTextApplicationTest {

    @MockBean
    private TranscriptionService transcriptionService;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
    }
}

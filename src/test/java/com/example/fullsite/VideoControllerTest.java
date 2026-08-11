package com.example.fullsite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class VideoControllerTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private VideoController videoController;

    private User fakeUser;
    private MockMultipartFile fakeFile;

    @BeforeEach
    public void setUp() {
        fakeUser = new User();
        fakeUser.setId(42L);

        fakeFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                MediaType.MULTIPART_FORM_DATA_VALUE,
                "dummy video payload data".getBytes()
        );
    }


    @Test
    public void testGetMyVideosSuccess() {
        Video video1 = Video.builder().s3Key("key1.mp4").build();
        Video video2 = Video.builder().s3Key("key2.mp4").build();

        Mockito.when(videoRepository.findByUserId(42L)).thenAnswer(inv -> List.of(video1, video2));
        Mockito.when(s3Service.generatePresignedUrl("key1.mp4")).thenAnswer(inv -> "https://s3.com/key1.mp4");
        Mockito.when(s3Service.generatePresignedUrl("key2.mp4")).thenAnswer(inv -> "https://s3.com/key2.mp4");

        ResponseEntity<List<String>> response = videoController.getUserVideos(fakeUser);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("https://s3.com/key1.mp4", response.getBody().get(0));
    }

    @Test
    public void testProcessActionSilenceTrim() throws Exception {
        Mockito.when(s3Service.uploadLocalFile(any(), any())).thenAnswer(inv -> "processed-silence.mp4");
        Mockito.when(s3Service.generatePresignedUrl("processed-silence.mp4")).thenAnswer(inv -> "https://s3.com/silence.mp4");

        ResponseEntity<String> response = videoController.processVideo(fakeFile, "silence_trim", fakeUser);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://s3.com/silence.mp4", response.getBody());
    }

    @Test
    public void testProcessActionCompress() throws Exception {
        Mockito.when(s3Service.uploadLocalFile(any(), any())).thenAnswer(inv -> "processed-comp.mp4");
        Mockito.when(s3Service.generatePresignedUrl("processed-comp.mp4")).thenAnswer(inv -> "https://s3.com/compressed.mp4");

        ResponseEntity<String> response = videoController.processVideo(fakeFile, "compress", fakeUser);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://s3.com/compressed.mp4", response.getBody());
    }

    @Test
    public void testProcessActionAudio() throws Exception {
        Mockito.when(s3Service.uploadLocalFile(any(), any())).thenAnswer(inv -> "processed-audio.mp3");
        Mockito.when(s3Service.generatePresignedUrl("processed-audio.mp3")).thenAnswer(inv -> "https://s3.com/audio.mp3");

        ResponseEntity<String> response = videoController.processVideo(fakeFile, "audio", fakeUser);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://s3.com/audio.mp3", response.getBody());
    }

    @Test
    public void testProcessActionFallbackToProxy() throws Exception {
        Mockito.when(s3Service.uploadLocalFile(any(), any())).thenAnswer(inv -> "processed-proxy.mp4");
        Mockito.when(s3Service.generatePresignedUrl("processed-proxy.mp4")).thenAnswer(inv -> "https://s3.com/proxy.mp4");

        // Passing an unrecognized action to hit the default switch branch
        ResponseEntity<String> response = videoController.processVideo(fakeFile, "unknown_action", fakeUser);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://s3.com/proxy.mp4", response.getBody());
    }
}

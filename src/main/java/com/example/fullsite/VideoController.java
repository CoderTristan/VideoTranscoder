package com.example.fullsite;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
public class VideoController {

    private final S3Service s3Service;
    private final VideoRepository videoRepository;

    @GetMapping("/my-videos")
    public ResponseEntity<List<String>> getUserVideos(@AuthenticationPrincipal User user) {
        List<Video> videos = videoRepository.findByUserId(user.getId());

        List<String> liveUrls = videos.stream()
                .map(video -> s3Service.generatePresignedUrl(video.getS3Key()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(liveUrls);
    }
    // Route that does most ffmpeg operations
    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> processVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "action", defaultValue = "proxy") String action,
            @AuthenticationPrincipal User user
    ) throws Exception {

        Path input = Files.createTempFile("input-", ".mp4");
        file.transferTo(input.toFile());

        String extension = "audio".equals(action) ? ".mp3" : ".mp4";
        Path output = Files.createTempFile("output-", extension);

        ProcessBuilder pb;

        switch (action) {
            case "silence_trim":
                pb = new ProcessBuilder(
                        "ffmpeg", "-y", "-i", input.toString(),
                        "-filter_complex", "[0:v]fps=30[v_fps];[0:a]silenceremove=start_periods=1:start_threshold=-30dB:stop_periods=-1:stop_duration=0.3:stop_threshold=-30dB,asetpts=N/SR/TB[a_trimmed];[v_fps]mpdecimate,setpts=N/30/TB[v_trimmed]",
                        "-map", "[v_trimmed]",
                        "-map", "[a_trimmed]",
                        "-fps_mode", "vfr",
                        "-c:v", "libx264",
                        "-crf", "22",
                        "-preset", "veryfast",
                        "-pix_fmt", "yuv420p",
                        output.toString()
                );
                break;

            case "compress":
                pb = new ProcessBuilder(
                        "ffmpeg", "-y", "-i", input.toString(),
                        "-c:v", "libx264",
                        "-crf", "28",
                        "-preset", "slow",
                        "-pix_fmt", "yuv420p",
                        "-c:a", "aac",
                        "-b:a", "96k",
                        output.toString()
                );
                break;

            case "audio":
                pb = new ProcessBuilder(
                        "ffmpeg", "-y", "-i", input.toString(),
                        "-vn", "-c:a", "libmp3lame", "-b:a", "192k",
                        output.toString()
                );
                break;

            case "proxy":
            default:
                pb = new ProcessBuilder(
                        "ffmpeg", "-y", "-i", input.toString(),
                        "-vf", "scale=-2:480",
                        "-c:v", "libx264", "-crf", "20", "-preset", "ultrafast",
                        "-c:a", "aac",
                        output.toString()
                );
                break;
        }

        pb.inheritIO().start().waitFor();

        String s3Key = s3Service.uploadLocalFile(output, file.getOriginalFilename());

        Video videoRecord = Video.builder()
                .title(file.getOriginalFilename() + " (" + action.toUpperCase() + ")")
                .s3Key(s3Key)
                .user(user)
                .build();
        videoRepository.save(videoRecord);

        Files.deleteIfExists(input);
        Files.deleteIfExists(output);

        return ResponseEntity.ok(s3Service.generatePresignedUrl(s3Key));
    }
}

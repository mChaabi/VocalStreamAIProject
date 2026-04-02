package GenAI_To_SpringAI.migration4.controller;

import GenAI_To_SpringAI.migration4.service.VoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    @Autowired
    private VoiceService voiceService;

    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestBody Map<String, String> body) {
        byte[] audio = voiceService.textToSpeech(
                body.get("text"),
                body.getOrDefault("lang", "fr")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        return ResponseEntity.ok().headers(headers).body(audio);
    }

    @GetMapping("/play")
    public ResponseEntity<byte[]> play(@RequestParam String text) {
        byte[] audio = voiceService.textToSpeech(text, "fr");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .body(audio);
    }

    @PostMapping("/stt")
    public ResponseEntity<Map<String, String>> stt(@RequestParam("file") MultipartFile file) throws IOException {
        String text = voiceService.speechToText(file);
        return ResponseEntity.ok(Map.of("text", text));
    }
}


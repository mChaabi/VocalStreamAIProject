package GenAI_To_SpringAI.migration4.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class VoiceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String VOICE_SERVICE_URL = "http://localhost:5000";

    // Texte → Audio (retourne les bytes MP3)
    public byte[] textToSpeech(String text, String lang) {
        Map<String, String> body = new HashMap<>();
        body.put("text", text);
        body.put("lang", lang);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    VOICE_SERVICE_URL + "/tts",
                    request,
                    byte[].class
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erreur de communication avec Flask TTS: " + e.getMessage());
            return new byte[0];
        }
    }

    // Audio → Texte (retourne le texte transcrit)
    public String speechToText(MultipartFile audioFile) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // ✅ FIX 1: field name must be "audio" — Flask checks: request.files['audio']
        // ✅ FIX 2: filename must be "audio.wav" — speech_recognition needs a valid wav filename
        body.add("audio", new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() { return "audio.wav"; }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                VOICE_SERVICE_URL + "/stt",   // ✅ FIX 3: was "/voice/stt", correct path is "/stt"
                request,
                Map.class
        );
        return (String) response.getBody().get("text");
    }
}
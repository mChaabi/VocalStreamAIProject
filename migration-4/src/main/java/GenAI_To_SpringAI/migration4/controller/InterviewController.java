package GenAI_To_SpringAI.migration4.controller;

import GenAI_To_SpringAI.migration4.dto.EvaluationResponse;
import GenAI_To_SpringAI.migration4.service.InterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    // Démarrer l'interview → retourne audio de la 1ère question
    @PostMapping("/start")
    public org.springframework.http.ResponseEntity<byte[]> start(@RequestBody Map<String, String> body) {
        byte[] audio = interviewService.startInterview(body.get("poste"));

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .body(audio);
    }

    // Envoyer réponse audio → retourne audio de la prochaine question
    @PostMapping("/answer")
    public org.springframework.http.ResponseEntity<byte[]> answer(@RequestParam("file") MultipartFile file)
            throws IOException {
        byte[] audio = interviewService.processAnswer(file);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .body(audio);
    }

    // Obtenir l'évaluation finale
// Dans InterviewController.java

    @GetMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate() {
        // Appel au service qui retourne le Record
        EvaluationResponse evaluation = interviewService.evaluateStructured();

        // Retourne la réponse avec un statut 200 OK
        return org.springframework.http.ResponseEntity.ok(evaluation);
    }
}


package GenAI_To_SpringAI.migration4.service;

import GenAI_To_SpringAI.migration4.dto.EvaluationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewService {

    @Autowired
    private VoiceService voiceService;

    private final ChatClient chatClient;
    private List<String> conversationHistory = new ArrayList<>();
    private String currentPoste = "";
    private int questionCount = 0;
    private static final int MAX_QUESTIONS = 10;

    public InterviewService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // ─────────────────────────────────────────────
    // Démarrer l'interview → première question
    // ─────────────────────────────────────────────
    public byte[] startInterview(String poste) {
        this.currentPoste = poste;
        this.questionCount = 0;
        this.conversationHistory.clear();

        String systemPrompt = buildSystemPrompt(poste);
        String firstQuestion = callAI(systemPrompt);
        questionCount++;

        conversationHistory.add("Recruteur: " + firstQuestion);

        try {
            return voiceService.textToSpeech(firstQuestion, "fr");
        } catch (Exception e) {
            System.err.println("Avertissement TTS: " + e.getMessage());
            return new byte[0];
        }
    }

    // ─────────────────────────────────────────────
    // Traiter la réponse → question suivante
    // ─────────────────────────────────────────────
    public byte[] processAnswer(MultipartFile audioFile) throws IOException {
        String candidateAnswer = voiceService.speechToText(audioFile);
        conversationHistory.add("Candidat: " + candidateAnswer);

        String historique = String.join("\n", conversationHistory);
        String prompt = buildNextQuestionPrompt(currentPoste, historique, questionCount);

        String nextQuestion = callAI(prompt);
        questionCount++;
        conversationHistory.add("Recruteur: " + nextQuestion);

        return voiceService.textToSpeech(nextQuestion, "fr");
    }

    // ─────────────────────────────────────────────
    // Évaluation finale structurée
    // ─────────────────────────────────────────────
    public EvaluationResponse evaluateStructured() {
        var converter = new BeanOutputConverter<>(EvaluationResponse.class);

        String historique = String.join("\n", conversationHistory);
        String prompt = """
            Tu es un expert RH. Analyse l'interview complète ci-dessous pour le poste de %s.
            
            HISTORIQUE:
            %s
            
            Évalue le candidat sur :
            - Score global /100
            - Feedback général sur sa performance
            - Points forts démontrés
            - Points à améliorer
            
            {format}
        """.formatted(currentPoste, historique);

        String response = chatClient.prompt()
                .user(u -> u.text(prompt).param("format", converter.getFormat()))
                .call()
                .content();

        return converter.convert(response);
    }

    // ─────────────────────────────────────────────
    // PROMPT SYSTÈME — adapté au poste
    // ─────────────────────────────────────────────
    private String buildSystemPrompt(String poste) {
        return """
            Tu es un recruteur expert qui conduit un entretien professionnel pour le poste de : %s.
            
            RÈGLES DE L'INTERVIEW :
            - L'interview comporte exactement %d questions au total.
            - Tu dois adapter le TYPE et la DURÉE ATTENDUE de chaque question au poste :
            
              📌 STRUCTURE DES QUESTIONS (dans cet ordre) :
              1. Question de motivation/introduction        → réponse courte attendue (30 sec)
              2-4. Questions techniques spécifiques au poste → réponse longue attendue (2 min)
              5-7. Questions comportementales (STAR method)  → réponse moyenne attendue (1 min)
              8-9. Mise en situation / cas pratique          → réponse longue attendue (2 min)
              10. Question de clôture (questions du candidat) → réponse courte (30 sec)
            
            - Pose UNE seule question à la fois.
            - La question doit être précise, professionnelle et adaptée au niveau du poste.
            - Indique entre parenthèses la durée attendue après chaque question.
              Exemple : "Pouvez-vous vous présenter ? (30 secondes)"
            - Ne donne PAS les réponses, ne commente PAS les réponses du candidat.
            - Commence directement par la première question.
            
            COMMENCE L'INTERVIEW MAINTENANT avec la question 1/10.
        """.formatted(poste, MAX_QUESTIONS);
    }

    // ─────────────────────────────────────────────
    // PROMPT QUESTION SUIVANTE — avec contexte
    // ─────────────────────────────────────────────
    private String buildNextQuestionPrompt(String poste, String historique, int questionNumber) {
        String questionType = getQuestionType(questionNumber + 1);

        return """
            Tu conduis un entretien pour le poste de : %s.
            
            HISTORIQUE DE L'INTERVIEW :
            %s
            
            RÈGLES :
            - C'est maintenant la question %d/%d.
            - Type de question attendu : %s
            - Pose UNE seule question, sans commenter la réponse précédente.
            - Indique la durée attendue entre parenthèses à la fin.
            - Si c'est la dernière question (10/10), conclus l'interview poliment.
            
            Génère uniquement la question suivante.
        """.formatted(poste, historique, questionNumber + 1, MAX_QUESTIONS, questionType);
    }

    // ─────────────────────────────────────────────
    // Détermine le type de question selon le numéro
    // ─────────────────────────────────────────────
    private String getQuestionType(int questionNumber) {
        return switch (questionNumber) {
            case 1       -> "Motivation / Introduction (durée attendue : 30 secondes)";
            case 2, 3, 4 -> "Technique spécifique au poste (durée attendue : 2 minutes)";
            case 5, 6, 7 -> "Comportementale - méthode STAR (durée attendue : 1 minute)";
            case 8, 9    -> "Mise en situation / cas pratique (durée attendue : 2 minutes)";
            case 10      -> "Clôture - questions du candidat (durée attendue : 30 secondes)";
            default      -> "Question générale (durée attendue : 1 minute)";
        };
    }

    // ─────────────────────────────────────────────
    // Appel Spring AI
    // ─────────────────────────────────────────────
    private String callAI(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
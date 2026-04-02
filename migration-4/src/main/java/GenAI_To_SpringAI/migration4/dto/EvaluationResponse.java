package GenAI_To_SpringAI.migration4.dto;

/**
 * Représente la réponse d'évaluation sous forme de Record.
 * Les champs sont immuables par défaut.
 */
public record EvaluationResponse(
        int score,                // Note sur 100
        String feedback,
        String pointsForts,
        String pointsAmelioration
) {}

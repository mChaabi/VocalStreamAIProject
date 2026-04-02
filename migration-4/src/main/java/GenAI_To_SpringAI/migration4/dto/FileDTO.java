package GenAI_To_SpringAI.migration4.dto;

import GenAI_To_SpringAI.migration4.constants.MimeType;

public record FileDTO(
        String text,
        String b64EFile,
        MimeType mimeType,
        String schema
) {
    // Constructeur compact pour gérer la logique par défaut
    public FileDTO(String text, String b64EFile, MimeType mimeType, String schema) {
        this.text = text;
        this.b64EFile = b64EFile;
        this.schema = schema;

        // Logique pour la valeur par défaut du MimeType
        if (mimeType == null) {
            MimeType detected = MimeType.fromBase64(b64EFile);
            this.mimeType = (detected != null) ? detected : MimeType.TEXT_PLAIN;
        } else {
            this.mimeType = mimeType;
        }
    }

    // Un deuxième constructeur pour faciliter l'appel sans MimeType
    public FileDTO(String text, String b64EFile, String schema) {
        this(text, b64EFile, null, schema);
    }
}
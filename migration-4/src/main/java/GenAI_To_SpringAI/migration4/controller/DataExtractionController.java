package GenAI_To_SpringAI.migration4.controller;

import GenAI_To_SpringAI.migration4.dto.FileDTO;
import GenAI_To_SpringAI.migration4.service.CvDetectorService;
import GenAI_To_SpringAI.migration4.service.CvExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/api/v1/extract")
public class DataExtractionController {

    private static final Logger log = LoggerFactory.getLogger(DataExtractionController.class);

    private final CvDetectorService detectorService;
    private final CvExtractionService extractionService;

    public DataExtractionController(CvDetectorService detectorService,
                                    CvExtractionService extractionService) {
        this.detectorService   = detectorService;
        this.extractionService = extractionService;
    }

    @PostMapping
    public String extractData(@RequestBody FileDTO fileDTO) {

        long startTotal = System.currentTimeMillis();

        // ─────────────────────────────────────────
        log.info("╔══════════════════════════════════════════╗");
        log.info("║         📄 NOUVELLE REQUÊTE CV           ║");
        log.info("╚══════════════════════════════════════════╝");

        // Infos fichier entrant
        String mimeStr  = fileDTO.mimeType() != null ? fileDTO.mimeType().getMimeString() : "null";
        int    b64Len   = fileDTO.b64EFile() != null ? fileDTO.b64EFile().length() : 0;
        int    textLen  = fileDTO.text()     != null ? fileDTO.text().length()     : 0;
        double fileSizeKb = b64Len * 0.75 / 1024;

        log.info("┌─── 📥 FICHIER REÇU ───────────────────────");
        log.info("│  🗂️  MimeType     : {}", mimeStr);
        log.info("│  📏  Base64 taille : {} caractères", b64Len);
        log.info("│  💾  Taille estimée: {:.2f} KB", fileSizeKb);
        log.info("│  📝  Text direct   : {} caractères", textLen);
        log.info("│  📋  Schema fourni : {}", fileDTO.schema() != null ? "OUI" : "NON");
        log.info("└───────────────────────────────────────────");

        // ─── Partie 1 : Détection ─────────────────
        log.info("┌─── 🔍 DÉTECTION FORMAT ───────────────────");
        long startDetect = System.currentTimeMillis();

        String format = detectorService.detect(fileDTO);

        long detectTime = System.currentTimeMillis() - startDetect;
        log.info("│  ✅  Format détecté : {}", format);
        log.info("│  ⏱️   Temps détection : {} ms", detectTime);

        String formatIcon = switch (format) {
            case "PLAIN_TEXT"       -> "📄 Texte brut";
            case "DOCX"             -> "📝 Word DOCX";
            case "EUROPASS"         -> "🇪🇺 Europass XML";
            case "IMAGE_OCR"        -> "🖼️  Image OCR";
            case "GRAPHIQUE"        -> "🎨 PDF Graphique (OCR)";
            case "STANDARD_CHRONO"  -> "📋 PDF Standard";
            default                 -> "❓ Inconnu";
        };
        log.info("│  🏷️   Type CV       : {}", formatIcon);
        log.info("└───────────────────────────────────────────");

        // ─── Partie 2 : Extraction + LLM ──────────
        log.info("┌─── 🤖 EXTRACTION + LLM ───────────────────");
        long startExtract = System.currentTimeMillis();

        String result = extractionService.extract(fileDTO, format);

        long extractTime = System.currentTimeMillis() - startExtract;
        int  resultLen   = result != null ? result.length() : 0;

        log.info("│  ✅  Extraction terminée");
        log.info("│  📤  Taille réponse JSON : {} caractères", resultLen);
        log.info("│  ⏱️   Temps extraction+LLM: {} ms", extractTime);
        log.info("└───────────────────────────────────────────");

        // ─── Résumé final ─────────────────────────
        long totalTime = System.currentTimeMillis() - startTotal;
        log.info("╔══════════════════════════════════════════╗");
        log.info("║          ✅ TRAITEMENT TERMINÉ           ║");
        log.info("║  Format   : {:<32}║", formatIcon);
        log.info("║  Détection: {:<29} ms ║", detectTime);
        log.info("║  LLM      : {:<29} ms ║", extractTime);
        log.info("║  TOTAL    : {:<29} ms ║", totalTime);
        log.info("╚══════════════════════════════════════════╝");

        return result;
    }
}

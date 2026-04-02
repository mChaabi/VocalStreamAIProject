package GenAI_To_SpringAI.migration4.service;

import GenAI_To_SpringAI.migration4.constants.MimeType;
import GenAI_To_SpringAI.migration4.dto.FileDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class CvDetectorService {

    public String detect(FileDTO fileDTO) {
        try {
            MimeType mime = fileDTO.mimeType();

            if (mime == MimeType.TEXT_PLAIN)
                return "PLAIN_TEXT";

            if (mime == MimeType.APPLICATION_MSWORD
                    || mime == MimeType.APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT)
                return "DOCX";

            if (mime == MimeType.TEXT_XML)
                return "EUROPASS";

            if (isImage(mime))
                return "IMAGE_OCR";

            if (mime == MimeType.APPLICATION_PDF)
                return analyzePdf(decodeBase64(fileDTO.b64EFile()));

            return "STANDARD_CHRONO";

        } catch (Exception e) {
            throw new RuntimeException("Erreur détection format CV : " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    // ANALYSE HEURISTIQUE PDF
    // ═══════════════════════════════════════════════════════

    private String analyzePdf(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {

            if (doc.getNumberOfPages() == 0)
                return "GRAPHIQUE";

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text    = stripper.getText(doc);
            String trimmed = text.trim();

            // Trop peu de texte → scanné / image pure
            if (trimmed.length() < 150)
                return "GRAPHIQUE";

            // Détection Europass
            if (trimmed.contains("Europass") ||
                    (trimmed.contains("Personal information")
                            && trimmed.contains("Work experience")))
                return "EUROPASS";

            // ── Heuristiques lignes ──────────────────────────
            String[] lines = text.lines()
                    .filter(l -> !l.trim().isEmpty())
                    .toArray(String[]::new);
            int totalLines = lines.length;

            if (totalLines == 0) return "GRAPHIQUE";

            long shortLines     = 0;
            long veryShortLines = 0;
            int  totalWords     = 0;

            for (String line : lines) {
                String t   = line.trim();
                int len    = t.length();
                int words  = t.split("\\s+").length;
                totalWords += words;
                if (len < 20) shortLines++;
                if (len < 8)  veryShortLines++;
            }

            double avgWords       = (double) totalWords     / totalLines;
            double shortLineRatio = (double) shortLines     / totalLines;
            double veryShortRatio = (double) veryShortLines / totalLines;

            boolean manyShortLines  = shortLineRatio > 0.50;
            boolean lowAvgWords     = avgWords        < 4.5;
            boolean manySymbolLines = veryShortRatio  > 0.10;

            if ((manyShortLines && lowAvgWords) || manySymbolLines) {
                // Texte quand même riche → garder STANDARD
                if (trimmed.length() > 1500 && avgWords >= 3.0)
                    return "STANDARD_CHRONO";
                return "GRAPHIQUE";
            }

            return "STANDARD_CHRONO";
        }
    }

    // ═══════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════

    private boolean isImage(MimeType mime) {
        return mime == MimeType.IMAGE_JPEG
                || mime == MimeType.IMAGE_PNG
                || mime == MimeType.IMAGE_GIF
                || mime == MimeType.IMAGE_BMP
                || mime == MimeType.IMAGE_WEBP;
    }

    private byte[] decodeBase64(String b64) {
        if (b64 == null || b64.isBlank())
            throw new IllegalArgumentException("b64EFile est vide ou null.");
        try {
            return Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("b64EFile n'est pas un base64 valide.", e);
        }
    }
}

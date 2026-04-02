package GenAI_To_SpringAI.migration4.service;

import GenAI_To_SpringAI.migration4.constants.JsonExtractionPromptConstants;
import GenAI_To_SpringAI.migration4.dto.FileDTO;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class CvExtractionService {

    private static final int MIN_TEXT_PDF       = 300;
    private static final int MIN_TEXT_GRAPHIQUE = 400;

    private final OpenAiChatModel chatModel;

    public CvExtractionService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // ═══════════════════════════════════════════════════════
    // POINT D'ENTRÉE
    // ═══════════════════════════════════════════════════════

    /**
     * Reçoit le FileDTO + le format détecté par CvDetectorService.
     * Extrait le texte brut, puis appelle Groq LLM.
     * @return JSON structuré (String)
     */
    public String extract(FileDTO fileDTO, String format) {
        try {
            // 1. Extraction texte brut selon le format
            String rawText = extractRawText(decodeBase64(fileDTO.b64EFile()), format);

            // 2. Appel LLM → JSON structuré
            return callLlm(rawText);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur extraction CV : " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    // EXTRACTION TEXTE BRUT
    // ═══════════════════════════════════════════════════════

    private String extractRawText(byte[] bytes, String format) throws Exception {
        return switch (format) {
            case "PLAIN_TEXT"                    -> new String(bytes, StandardCharsets.UTF_8);
            case "DOCX"                          -> extractDocx(bytes);
            case "IMAGE_OCR"                     -> ocrImage(ImageIO.read(new ByteArrayInputStream(bytes)));
            case "GRAPHIQUE"                     -> extractGraphique(bytes);
            case "EUROPASS", "STANDARD_CHRONO"   -> extractPdfText(bytes);
            default                              -> extractPdfText(bytes);
        };
    }

    // ── PDF texte normal ────────────────────────────────────
    private String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(doc.getNumberOfPages());
            String text = stripper.getText(doc);
            // Trop peu → fallback OCR
            if (text.trim().length() < MIN_TEXT_PDF)
                return extractGraphique(pdfBytes);
            return text;
        }
    }

    // ── PDF graphique → essai texte, sinon OCR Tesseract ───
    private String extractGraphique(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);

            if (text.trim().length() > MIN_TEXT_GRAPHIQUE) return text;

            // OCR page par page
            StringBuilder result   = new StringBuilder();
            PDFRenderer   renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img      = renderer.renderImageWithDPI(i, 300);
                String        pageText = ocrImage(img);
                result.append("--- Page ").append(i + 1).append(" ---\n")
                        .append(pageText).append("\n");
            }
            return result.toString();
        }
    }

    // ── DOCX via Apache POI ─────────────────────────────────
    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFHeader header : doc.getHeaderList()) {
                String t = header.getText().trim();
                if (!t.isEmpty()) sb.append(t).append("\n");
            }
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (!t.isEmpty()) sb.append(t).append("\n");
            }
            appendTables(doc.getTables(), sb);
            for (XWPFFooter footer : doc.getFooterList()) {
                String t = footer.getText().trim();
                if (!t.isEmpty()) sb.append(t).append("\n");
            }
            return sb.toString();
        }
    }

    private void appendTables(List<XWPFTable> tables, StringBuilder sb) {
        for (XWPFTable table : tables) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String t = cell.getText().trim();
                    if (!t.isEmpty()) sb.append(t).append(" | ");
                    appendTables(cell.getTables(), sb);
                }
                sb.append("\n");
            }
        }
    }

    // ── OCR Tesseract ───────────────────────────────────────
    private String ocrImage(BufferedImage img) throws Exception {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setLanguage("fra+eng+ara");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(3);
        return tesseract.doOCR(img);
    }

    // ═══════════════════════════════════════════════════════
    // APPEL GROQ LLM
    // ═══════════════════════════════════════════════════════

    private String callLlm(String rawText) {
        if (rawText == null || rawText.isBlank())
            throw new IllegalArgumentException("Texte extrait vide : impossible d'analyser le CV.");

        SystemMessage system = new SystemMessage(
                JsonExtractionPromptConstants.SYSTEM_PROMPT
        );
        String userContent = String.format(
                JsonExtractionPromptConstants.USER_TEMPLATE,
                JsonExtractionPromptConstants.jsonSchema,
                rawText
        );

        String response = chatModel
                .call(new Prompt(List.of(system, new UserMessage(userContent))))
                .getResult()
                .getOutput()
                .getText();

        return response
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();
    }

    // ═══════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════

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
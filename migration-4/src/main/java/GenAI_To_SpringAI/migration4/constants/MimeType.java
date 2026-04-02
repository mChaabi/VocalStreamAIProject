package GenAI_To_SpringAI.migration4.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Base64;

public enum MimeType {

    // --- Text Types ---
    TEXT_PLAIN("text/plain", "txt"),
    TEXT_HTML("text/html", "html", "htm"),
    TEXT_CSS("text/css", "css"),
    TEXT_JAVASCRIPT("text/javascript", "js"),
    TEXT_CSV("text/csv", "csv"),
    TEXT_XML("text/xml", "xml"),

    // --- Image Types ---
    IMAGE_JPEG("image/jpeg", "jpeg", "jpg"),
    IMAGE_PNG("image/png", "png"),
    IMAGE_GIF("image/gif", "gif"),
    IMAGE_BMP("image/bmp", "bmp"),
    IMAGE_WEBP("image/webp", "webp"),
    IMAGE_SVG("image/svg+xml", "svg"),

    // --- Audio Types ---
    AUDIO_MPEG("audio/mpeg", "mp3"),
    AUDIO_WAV("audio/wav", "wav"),
    AUDIO_OGG("audio/ogg", "ogg"),
    AUDIO_AAC("audio/aac", "aac"),

    // --- Video Types ---
    VIDEO_MP4("video/mp4", "mp4"),
    VIDEO_MPEG("video/mpeg", "mpeg", "mpg"),
    VIDEO_WEBM("video/webm", "webm"),
    VIDEO_OGG("video/ogg", "ogv"),

    // --- Application Types ---
    APPLICATION_JSON("application/json", "json"),
    APPLICATION_PDF("application/pdf", "pdf"),
    APPLICATION_ZIP("application/zip", "zip"),
    APPLICATION_GZIP("application/gzip", "gz"),
    APPLICATION_OCTET_STREAM("application/octet-stream"), // Liste vide par défaut
    APPLICATION_MSWORD("application/msword", "doc"),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"
    ),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"
    ),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"
    ),
    APPLICATION_X_SHOCKWAVE_FLASH("application/x-shockwave-flash", "swf"),
    APPLICATION_JAVA_ARCHIVE("application/java-archive", "jar"),
    APPLICATION_POSTSCRIPT("application/postscript", "ps"),

    // --- Font Types ---
    FONT_WOFF("font/woff", "woff"),
    FONT_WOFF2("font/woff2", "woff2"),
    FONT_TTF("font/ttf", "ttf"),
    FONT_OTF("font/otf", "otf");

    private final String mimeString;
    private final List<String> extensions;

    // Constructeur pour gérer les arguments variables (varargs) pour les extensions
    MimeType(String mimeString, String... extensions) {
        this.mimeString = mimeString;
        this.extensions = Arrays.asList(extensions);
    }

    public String getMimeString() {
        return mimeString;
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    public boolean matchesExtension(String extension) {
        if (extension == null) return false;
        String cleanExt = extension.startsWith(".") ? extension.substring(1) : extension;
        return extensions.contains(cleanExt.toLowerCase());
    }

    // --- Static Methods (Equivalent du Companion Object) ---

    public static MimeType fromString(String mimeString) {
        if (mimeString == null) return null;
        return Arrays.stream(MimeType.values())
                .filter(m -> m.mimeString.equalsIgnoreCase(mimeString))
                .findFirst()
                .orElse(null);
    }

    public static MimeType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) return null;
        String cleanExt = extension.startsWith(".") ? extension.substring(1) : extension;
        String finalExt = cleanExt.toLowerCase();

        return Arrays.stream(MimeType.values())
                .filter(m -> m.extensions.contains(finalExt))
                .findFirst()
                .orElse(null);
    }

    /**
     * Note: Nécessite la bibliothèque Apache Tika dans votre pom.xml/build.gradle
     */
    public static MimeType fromBase64(String b64) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(b64);
            // Ici, j'utilise une simulation car Tika n'est pas une classe standard Java
            // org.apache.tika.Tika tika = new org.apache.tika.Tika();
            // String mimeTypeString = tika.detect(decodedBytes);
            // return fromString(mimeTypeString);

            return null; // À implémenter avec votre instance Tika
        } catch (Exception e) {
            return null;
        }
    }
}

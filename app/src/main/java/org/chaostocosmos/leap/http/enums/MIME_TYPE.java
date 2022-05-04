package org.chaostocosmos.leap.http.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mime type enum
 * 
 * @author 9ins
 */
public enum MIME_TYPE {
    //binary & encoded
    APPLICATION_OCTET_STREAM("application/octet-stream"),    
    APPLICATION_PKCS12("application/pkcs12"),
    APPLICATION_VND_MSPOWERPOINT("application/vnd.mspowerpoint"),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    MULTIPART_BYTERANGES("multipart/byteranges"),
    APPLICATION_VND_APPLE_INSTALLER_XML("application/vnd.apple.installer+xml"),
    APPLICATION_OGG("application/ogg"),

    //compressed
    APPLICATION_ZIP("application/zip"),
    APPLICATION_JAVA_ARCHIVE("application/java-archive"),
    APPLICATION_X_BZIP("application/x-bzip"),
    APPLICATION_X_BZIP2("application/x-bzip2"),    
    APPLICATION_EPUB_ZIP("application/epub+zip"),
    APPLICATION_X_RAR_COMPRESSED("application/x-rar-compressed"),
    APPLICATION_X_TAR("application/x-tar"),
    APPLICATION_X_ZIP("application/x-zip-compressed"),
    APPLICATION_X_7Z_COMPRESSED("application/x-7z-compressed"),

    //document
    APPLICATION_PDF("application/pdf"),
    APPLICATION_VND_MS_POWERPOINT("application/vnd.ms-powerpoint"),
    APPLICATION_VND_MS_EXCEL("application/vnd.ms-excel"),
    APPLICATION_VND_VISIO("application/vnd.visio"),
    APPLICATION_RTF("application/rtf"),
    APPLICATION_X_ABIWORD("application/x-abiword"),
    APPLICATION_VND_AMAZON_EBOOK("application/vnd.amazon.ebook"),
    APPLICATION_MSWORD("application/msword"),
    APPLICATION_VND_OASIS_OPENDOCUMENT_PRESENTATION("application/vnd.oasis.opendocument.presentation"),
    APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET("application/vnd.oasis.opendocument.spreadsheet"),
    APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT("application/vnd.oasis.opendocument.text"),

    APPLICATION_X_CSH("application/x-csh"),
    APPLICATION_X_SH("application/x-sh"),
    APPLICATION_JSON("application/json"),
    APPLICATION_XML("application/xml"),
    APPLICATION_VND_MOZILLA_XUL_XML("application/vnd.mozilla.xul+xml"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    APPLICATION_JAVASCRIPT("application/javascript"),

    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    TEXT_XML("text/xml"),
    TEXT_JSON("text/json"),
    TEXT_CSS("text/css"),
    TEXT_CSV("text/csv"),
    TEXT_JAVASCRIPT("text/javascript"),
    APPLICATION_JS("application/js"),
    TEXT_CALENDAR("text/calendar"),

    //image
    IMAGE_X_ICON("image/x-icon"),
    IMAGE_GIF("image/gif"),
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_BMP("image/bmp"),
    IMAGE_WEBP("image/webp"),
    IMAGE_SVG_XML("image/svg+xml"),
    IMAGE_TIFF("image/tiff"),
    APPLICATION_X_SHOCKWAVE_FLASH("application/x-shockwave-flash"),

    //audio
    AUDIO_AAC("audio/aac"),
    AUDIO_MIDI("audio/midi"),
    AUDIO_MPEG("audio/mpeg"),
    AUDIO_WEBM("audio/webm"),
    AUDIO_OGG("audio/ogg"),
    AUDIO_X_WAV("audio/x-wav"),

    //video
    VIDEO_MP4("video/mp4"),
    VIDEO_X_FLV("video/x-flv"),
    VIDEO_QUICKTIME("video/quicktime"),
    VIDEO_X_MSVIDEO("video/x-msvideo"),
    VIDEO_X_MS_WMV("video/x-ms-wmv"),
    APPLICATION_X_MPEGURL("application/x-mpegURL"),
    AUDIO_WAV("audio/wav"),
    VIDEO_WEBM("video/webm"),
    VIDEO_OGG("video/ogg"),
    VIDEO_MPEG("video/mpeg"),
    VIDEO_3GPP("video/3gpp"),
    VIDEO_3GPP2("video/3gpp2"),

    //etc stuff
    APPLICATION_X_FONT_WOFF("application/x-font-woff"),
    APPLICATION_X_FONT_TTF("application/x-font-ttf")
    ;

    String mimeType;

    /**
     * Initializer
     * @param mimeType
     */
    MIME_TYPE(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Get mime type
     * @return
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * Get mime type from content type string
     * @param mimeType
     * @return
     */
    public static MIME_TYPE getMimeType(String mimeType) {
        
        return mimeType == null ? null : MIME_TYPE.valueOf(mimeType.toUpperCase()
                                         .replace("/", "_")
                                         .replace("-", "_")
                                         .replace(".", "_")
                                         .replace("+", "_")
                                         );
    }

    /**
     * Get mime type list of specfied parameter
     * @param type
     * @return
     */
    public List<MIME_TYPE> getMimeTypes(String type) {
        return Arrays.asList(MIME_TYPE.values()).stream().filter(m -> m.name().startsWith(type)).collect(Collectors.toList());
    }
}

package org.chaostocosmos.leap.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Constants
 * 
 * @author 9ins
 */
public class Constants {
    /**
     * Http protocol version
     */
    public static final String HTTP_VERSION = "1.0";    
    /**
     * In-Memory limit resource size
     */
    public static final long IN_MEMORY_LIMIT_SIZE = (long)Integer.MAX_VALUE * 100;    
    /**
     * default fraction point
     */
    public static final int DEFAULT_FRACTION_POINT = 2;    
    /**
     * separator for string property value
     */
    public static final String PROPERTY_SEPARATOR = ",";    
    /**
     * default leap home path
     */
    public static final Path DEFAULT_HOME_PATH = Paths.get("./");    
    /**
     * default buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024;    
    /**
     * body part max size limit
     */
    public static final int MULTIPART_FLUSH_MINIMAL_SIZE = 512;    
    /**
     * Password vaildation pattern
     */
    public static final Pattern PASSWORD_REGEX = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
    /**
     * Searching parameter pattern
     */
    public static final Pattern PARAM_PATTERN = Pattern.compile("\\{.*?\\}");    
    /**
     * Line separator
     */
    public static final String LS = System.lineSeparator();    
    /**
     * File separator
     */
    public static final String FS = File.separator;    
    /**
     * HTML comment replacement tag prefix
     */
    public static final String TAG_REGEX_PREFIX = "(<!--.*?|//)";    
    /**
     * HTML comment replacement tag suffix
     */
    public static final String TAG_REGEX_SUFFIX = "(.*?-->|//)";
    /**
     * Session ID key 
     */
    public static final String SESSION_ID_KEY = "__Leap-Session-ID";
    /**
     * Authorization key
     */
    public static final String AUTHORIZATION = "Authorization";
    /**
     * Clear site data key
     */
    public static final String CLEAR_SITE_DATA = "Clear-Site-Data";
}

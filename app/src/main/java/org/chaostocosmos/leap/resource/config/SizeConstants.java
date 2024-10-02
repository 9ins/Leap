package org.chaostocosmos.leap.resource.config;

import java.util.regex.Pattern;

/**
 * Size constants
 * 
 * @author 9ins
 */
public interface SizeConstants {

    /**
     * Byte
     */
    public static final long BYTE = 1L;

    /**
     * Kilo byte
     */
    public static final long KiB = BYTE << 10;

    /**
     * Mega byte
     */
    public static final long MiB = KiB << 10;

    /**
     * Giga byte
     */
    public static final long GiB = MiB << 10;

    /**
     * Tera byte
     */
    public static final long TiB = GiB << 10;

    /**
     * Peta byte
     */
    public static final long PiB = TiB << 10;

    /**
     * Exa byte
     */
    public static final long EiB = PiB << 10;

    /**
     * Percentage
     */
    public static final long PCT = 100L;

    /**
     * Count
     */
    public static final long CNT = 0L;

    /**
     * default fraction point
     */
    public static final int DEFAULT_FRACTION_POINT = 4;    

    /**
     * Number string split regex
     */
    public static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]+)");
}

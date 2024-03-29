package org.chaostocosmos.leap.http;

import java.net.MalformedURLException;
import java.net.URL;

import org.chaostocosmos.leap.enums.HTTP;
import org.chaostocosmos.leap.exception.LeapException;

/**
 * RedirectException
 * 
 * @author 9ins
 */
public class RedirectException extends LeapException {
    /**
     * URL
     */
    URL url;

    public RedirectException(String url) throws MalformedURLException {
        this(new URL(url));
    }
    
    /**
     * Constructs with URL object
     * @param url
     */
    public RedirectException(URL url) {
        super(HTTP.RES307, "Redirect to "+url.toString());
        this.url = url;
    }    

    /**
     * Get URL
     * @return
     */
    public URL getURL() {
        return this.url;        
    }

    /**
     * Get url string
     * @return
     */
    public String getURLString() {
        return this.url.toExternalForm();
    }
}

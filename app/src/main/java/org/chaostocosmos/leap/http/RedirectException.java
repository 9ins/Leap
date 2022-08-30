package org.chaostocosmos.leap.http;

import java.net.MalformedURLException;
import java.net.URL;

import org.chaostocosmos.leap.http.enums.RES_CODE;

/**
 * RedirectException
 * 
 * @author 9ins
 */
public class RedirectException extends HTTPException {
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
        super(RES_CODE.RES307, "Redirect to "+url.toString());
        super.addHeader("Location", url.toString());
        this.url = url;
    }    

    /**
     * Get URL
     * @return
     */
    public URL getURL() {
        return this.url;        
    }
}
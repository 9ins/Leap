package org.chaostocosmos.leap.http.services.format;

import org.chaostocosmos.leap.http.services.entity.Message;

import com.google.gson.Gson;

/**
 * MessageDecoder
 * 
 * @author 9ins
 */
public class MessageDecoder {

    private static Gson gson = new Gson();

    public void destroy() {
    }

    public Message decode(String message) {
        return gson.fromJson(message, Message.class);
    }

    public boolean willDecode(String message) {
        return (message != null);
    }    
}


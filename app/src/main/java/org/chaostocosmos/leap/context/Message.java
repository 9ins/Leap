package org.chaostocosmos.leap.context;

import org.chaostocosmos.leap.common.constant.Constants;

/**
 * Messages 
 * 
 * Message configuration object 
 * 
 * @author 9ins
 */
public class Message <T> extends Metadata <T> {

    /**
     * Constructor
     * @param messageMap
     */
    public Message(T messagesMap) {
        super(messagesMap);
    }

    /**
     * Get http message by specified code
     * @param code
     * @return
     */
    public String http(int code) {
        return message(code, new Object[0]);
    }

    /**
     * Get http message by specified code and parameters
     * @param code
     * @param params
     * @return
     */
    public String http(int code, Object... params) {
        return message(code, params);
    }

    /**
     * Get leap message by specified code
     * @param code
     * @return
     */
    public String leap(int code) {
        return message(code, new Object[0]);
    }

    /**
     * Get leap message by specified code and parameters
     * @param code
     * @param params
     * @return
     */
    public String leap(int code, Object... params) {
        return message(code, new Object[0]);
    }
    
    /**
     * Get leap message by specified code and parameters
     * @param code
     * @param params
     * @return
     */
    public String message(int code, Object... params) {
        String msg = null;
        if(code >= 100 && code < 600) {
            msg = super.getValue("http."+code);
        } else if(code >= 900 && code < 1000) {
            msg = super.getValue("leap."+code);
        } else {
            throw new IllegalArgumentException("The code is not supported. Code: "+code);
        }
        if(params == null || (params != null && params.length < 1)) {
            return msg.replace("{", "").replace("}", "");
        } else {
            String applied = "";            
            int i = 0;
            //String[] ss = Constants.PARAM_PATTERN.split(msg, 5);
            for(String s : Constants.PARAM_PATTERN.split(msg, 5)) {
                if(s.trim().equals("") && i < params.length) {
                    applied += params[i];
                    i++;
                }
                applied += s;                    
            }
            return applied;
        }
    }
}

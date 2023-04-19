package org.chaostocosmos.leap.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.chaostocosmos.leap.common.Constants;
import org.chaostocosmos.leap.common.DateUtils;
import org.chaostocosmos.leap.common.TIME;
import org.chaostocosmos.leap.context.Context;
import org.chaostocosmos.leap.context.Host;
import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.security.SessionIDGenerator;

/**
 * SessionManager
 * 
 * @author 9ins
 */
public class SessionManager {
    /**
     * Host object
     */
    final Host<?> host;

    /**
     * Session Map
     */
    final Map<String, Session> sessionMap = new HashMap<String, Session>();
    
    /**
     * Constructs with Host
     * @param host
     */
    public SessionManager(Host<?> host) {
        this.host = host;
    }

    /**
     * Get whether session support
     * @return
     */
    public boolean isApplySession() {
        return this.host.isSessionApply();
    }

    /**
     * Set apply session support flag
     * @param applySession
     */
    public void setApplySession(boolean applySession) {
        this.host.setSessionApply(applySession);
    }

    /**
     * Whether request exists in sessions
     * @param request
     * @return
     */
    public synchronized boolean exists(Request request) {
        String sessionId = request.getCookie(Constants.SESSION_ID_KEY);
        if(this.sessionMap.containsKey(sessionId)) {
            return true;
        }
        return false;
    }

    /**
     * Get session by session ID
     * @param sessionId
     * @return
     */
    public synchronized Session getSessionCreateIfNotExists(String sessionId) {        
        if(this.sessionMap.containsKey(sessionId)) {
            Session session = this.sessionMap.get(sessionId);
            session.setNew(false);            
            return session;
        } else {
            Session session = createSession(sessionId);
            addSession(session);
            return session;    
        }
    }

    /**
     * Create session
     * @param request
     * @return
     */
    public synchronized Session createSession(String sessionId) {
        int idLength = Context.host(this.host.getHostId()).getSessionIDLength();
        long creationTime = System.currentTimeMillis();
        long lastAccessedTime = creationTime;
        int maxInteractiveInteralSecond = Context.host(this.host.getHostId()).<Integer> getSessionTimeoutSeconds();
        sessionId = sessionId != null && !sessionId.equals("") ? sessionId : SessionIDGenerator.get(this.host.<String> getHostId()).generateSessionId(idLength);
        //System.out.println(sessionId+"================================");
        Session session = new HttpSession(this, sessionId, creationTime, lastAccessedTime, maxInteractiveInteralSecond);         
        //System.out.println(session.toString());
        session.setAttribute("Expires", DateUtils.getDateAddedOffset(this.host.<Integer>getExpireDays(), this.host.<String> getLocale()));
        session.setAttribute("Max-Age", TIME.HOUR.duration(this.host.<Integer> getMaxAgeHours(), TimeUnit.SECONDS));
        //session.setAttribute("Path", this.host.<String> getPath());
        return session;
    }

    /**
     * Add session
     * @param session
     */
    public synchronized void addSession(Session session) {
        this.sessionMap.put(session.getId(), session);
    }

    /**
     * Remove session
     * @param session
     */
    public synchronized boolean removeSession(String sessionId) {
        if(this.sessionMap.containsKey(sessionId)) {
            this.sessionMap.remove(sessionId);
            return true;
        }
        return false;
    }

    /**
     * Close session
     * @param session
     */
    public synchronized boolean removeSession(Session session) {
        return removeSession(session.getId());
    }

    /**
     * Clear SessionManager
     */
    public void reset() {
        this.sessionMap.values().stream().forEach(s -> removeSession(s));
        this.sessionMap.clear();
    }

    /**
     * Get session Map
     * @return
     */
    public Map<String, Session> getSessionMap() {
        return this.sessionMap;
    }

    /**
     * Get Host object
     * @return
     */
    public Host<?> getHost() {
        return this.host;
    }

    @Override
    public String toString() {
        return "{" +
            " host='" + host + "'" +
            //", sessionMap='" + sessionMap + "'" +
            "}";
    }    
}

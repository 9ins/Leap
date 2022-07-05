package org.chaostocosmos.leap.http.resources;

import java.nio.file.Path;
import java.util.Map;

import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.resources.WatchResources.ResourceInfo;

/**
 * Resource model
 * @author 9ins
 */
public interface Resources {    
    /**
     * Filtering and get resource List by mime-type
     * @param mimeType
     * @return
     */
    public ResourceInfo filter(MIME_TYPE mimeType) throws Exception;
    /**
     * Resolve real path
     * @param contextPath
     * @return
     */
    public Path resolveRealPath(String contextPath);
    /**
     * Get context mapping with local Path
     * @param resourcePath
     * @return
     */
    public String getContextPath(Path resourcePath);
    /**
     * Add resource 
     * @param resourcePath
     */
    public void addResource(Path resourcePath) throws Exception;
    /**
     * Add resource raw data with Path
     * @param resourcePath
     * @param resourceRawData
     */
    public void addResource(Path resourcePath, byte[] resourceRawData, boolean inMemoryFlag) throws Exception;
    /**
     * Remove resource
     * @param resourcePath
     * @throws Exception
     */
    public void removeResource(Path resourcePath) throws Exception;    
    /**
     * Get resource matching with path
     * @param resourcePath
     * @return
     */
    public ResourceInfo getResourceInfo(Path resourcePath) throws Exception;
    /**
     * Get resource content
     * @param contentName
     * @return
     * @throws Exception
     */
    public ResourceInfo getContextResourceInfo(String contextPath) throws Exception;
    /**
     * Whether resource exist in Resource
     * @param resourcePath
     * @return
     */
    public boolean exists(Path resourcePath) throws Exception;
    /**
     * Whether resource is In-Memory 
     * @param resourcePath
     * @return
     */
    public boolean isInMemory(Path resourcePath);
    /**
     * Get welcome page
     * @param params
     * @return
     * @throws Exception
     */
    public String getWelcomePage(Map<String, Object> params) throws Exception;
    /**
     * Get response page
     * @param params
     * @return
     * @throws Exception
     */
    public String getResponsePage(Map<String, Object> params) throws Exception;
    /**
     * Get error page
     * @param params
     * @return
     * @throws Exception
     */
    public String getErrorPage(Map<String, Object> params) throws Exception;
    /**
     * Get resource page
     * @param params
     * @return
     * @throws Exception
     */
    public String getResourcePage(Map<String, Object> params) throws Exception;
    /**
     * Get template page
     * @param hostId
     * @parma templatePath
     * @param params
     * @return
     * @throws Exception
     */
    public String getTemplatePage(String templatePath, Map<String, Object> params) throws Exception;
    /**
     * Get static page
     * @param resourceName
     * @param params
     * @return
     * @throws Exception
     */
    public String getStaticPage(String resourceName, Map<String, Object> params) throws Exception;
    /**
     * Resolve HTML page between comment replacement id and params
     * @param htmlPage
     * @param params
     * @return
     * @throws Exception
     */
    public String resolvePage(String htmlPage, Map<String, Object> params) throws Exception;
}

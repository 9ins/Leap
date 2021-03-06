package org.chaostocosmos.leap.http;

import java.lang.reflect.Method;
import java.util.List;

import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;
import org.chaostocosmos.leap.http.services.filters.IFilter;
import org.chaostocosmos.leap.http.services.model.ServiceModel;

/**
 * ServiceHolder
 * @author 9ins
 */
public class ServiceHolder {
    /**
     * Context path
     */
    String contextPath;
    /**
     * Request type
     */
    REQUEST_TYPE requestType;
    /**
     * Leap service
     */
    ServiceModel service;
    /**
     * Service method
     */
    Method serviceMethod;
    /**
     * Service filter for pre process
     */
    protected List<IFilter> preFilters;
    /**
     * Service filter for post process
     */
    protected List<IFilter> postFilters;
    /**
     * Constructor with parameters
     * @param contextPath
     * @param service
     * @param requestType
     * @param serviceMethod
     */
    public ServiceHolder(String contextPath, ServiceModel service, REQUEST_TYPE requestType, Method serviceMethod) {
        this.contextPath = contextPath;
        this.requestType = requestType;
        this.service = service;
        this.serviceMethod = serviceMethod;
    }

    public String getServicePath() {
        return this.contextPath;
    }

    public void setServicePath(String servicePath) {
        this.contextPath = servicePath;
    }

    public REQUEST_TYPE getRequestType() {
        return this.requestType;
    }

    public void setRequestType(REQUEST_TYPE requestType) {
        this.requestType = requestType;
    }

    public ServiceModel getService() {
        return this.service;
    }

    public void setService(ServiceModel service) {
        this.service = service;
    }

    public Method getServiceMethod() {
        return this.serviceMethod;
    }

    public void setServiceMethod(Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    @Override
    public String toString() {
        return "{" +
            " servicePath='" + getServicePath() + "'" +
            " requestType='" + getRequestType()+ "'" +
            ", serviceMethod='" + getServiceMethod() + "'" +
            "}";
    }
}


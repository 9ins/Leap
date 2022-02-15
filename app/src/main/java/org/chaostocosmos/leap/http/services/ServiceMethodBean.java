package org.chaostocosmos.leap.http.services;

import java.lang.reflect.Method;
import java.util.List;

import org.chaostocosmos.leap.http.filters.ILeapFilter;

/**
 * Service info bean object
 */
public class ServiceMethodBean {
    /**
     * Service object
     */
    protected ILeapService service;

    /**
     * Service method
     */
    protected Method serviceMethod;

    /**
     * Service filter for pre process
     */
    protected List<ILeapFilter> preFilters;

    /**
     * Service filter for post process
     */
    protected List<ILeapFilter> postFilters;

    /**
     * Constructor with attributes
     * @param service
     * @param serviceMethod
     * @param preFilters
     * @param postFilters
     */
    public ServiceMethodBean(ILeapService service, Method serviceMethod, List<ILeapFilter> preFilters, List<ILeapFilter> postFilters) {
        this.service = service;
        this.serviceMethod = serviceMethod;
        this.preFilters = preFilters;
        this.postFilters = postFilters;
    }

    public ILeapService getService() {
        return this.service;
    }

    public void setService(ILeapService service) {
        this.service = service;
    }

    public Method getServiceMethod() {
        return this.serviceMethod;
    }

    public void setServiceMethod(Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public List<ILeapFilter> getPreFilters() {
        return this.preFilters;
    }

    public void setPreFilters(List<ILeapFilter> preFilters) {
        this.preFilters = preFilters;
    }

    public List<ILeapFilter> getPostFilters() {
        return this.postFilters;
    }

    public void setPostFilters(List<ILeapFilter> postFilters) {
        this.postFilters = postFilters;
    }

    @Override
    public String toString() {
        return "{" +
            " service='" + getService() + "'" +
            ", serviceMethod='" + getServiceMethod() + "'" +
            ", preFilters='" + getPreFilters() + "'" +
            ", postFilters='" + getPostFilters() + "'" +
            "}";
    }
}


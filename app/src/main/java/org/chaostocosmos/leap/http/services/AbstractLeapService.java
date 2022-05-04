package org.chaostocosmos.leap.http.services;

import java.lang.reflect.Method;
import java.util.List;

import org.chaostocosmos.leap.http.HttpTransferBuilder.HttpTransfer;
import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.WASException;
import org.chaostocosmos.leap.http.annotation.AnnotationHelper;
import org.chaostocosmos.leap.http.annotation.AnnotationOpr;
import org.chaostocosmos.leap.http.annotation.PostFilter;
import org.chaostocosmos.leap.http.annotation.PreFilter;
import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;
import org.chaostocosmos.leap.http.enums.RES_CODE;
import org.chaostocosmos.leap.http.filters.ILeapFilter;
import org.chaostocosmos.leap.http.resources.Context;
import org.chaostocosmos.leap.http.resources.Resources;

import ch.qos.logback.classic.Logger;

/**
 * Abstraction of SimpleServlet object
 * @author Kooin-Shin
 * @since 2021.09.15
 */
public abstract class AbstractLeapService implements IGetService, IPostService, IPutService, IDeleteService {
    /**
     * Logger
     */
    protected Logger logger;
    /**
     * Context
     */
    protected static final Context context = Context.get();
    /**
     * Method to be called for request
     */
    protected Method invokingMethod;
    /**
     * Filters for previous filtering process of service method
     */
    protected List<ILeapFilter> preFilters;
    /**
     * Filter for after filtering process of service method
     */
    protected List<ILeapFilter> postFilters;
    /**
     * Leap service manager object
     */
    protected ServiceManager serviceManager;
    /**
     * Resource object
     */
    protected Resources resource;
    /**
     * HttpTransfer object
     */
    protected HttpTransfer httpTransfer;

    @Override
    public Response serve(final HttpTransfer httpTransfer, final Method invokingMethod) throws Exception {        
        this.logger = LoggerFactory.getLogger(httpTransfer.getRequest().getRequestedHost());
        this.httpTransfer = httpTransfer;
        this.invokingMethod = invokingMethod;
        this.resource = this.httpTransfer.getHosts().getResource();

        if(this.preFilters != null) {
            for(ILeapFilter filter : this.preFilters) {
                List<Method> methods = AnnotationHelper.getFilterMethods(filter, PreFilter.class); 
                for(Method method : methods) {
                    ServiceInvoker.invokeMethod(filter, method, this.httpTransfer.getRequest());
                }
            }
        }        
        Request request = httpTransfer.getRequest();
        Response response = httpTransfer.getResponse();

        Class<?>[] paramTypes = this.invokingMethod.getParameterTypes();
        if(paramTypes.length != 2 || paramTypes[0] != request.getClass() || paramTypes[1] != response.getClass()) {
            //org.chaostocosmos.leap.http.WASException: Not Implemented.
            throw new WASException(MSG_TYPE.HTTP, RES_CODE.RES501.getCode(), Context.getErrorMsg(201, this.invokingMethod.getName()));
        }

        //setting JPA link
        new AnnotationOpr<ILeapService>(httpTransfer.getHosts().getHost(), this).injectToAutowired();
        //aOpr.injectToAutowired();
        switch(httpTransfer.getRequest().getRequestType()) {
            case GET: 
            serveGet(request, response);
            break;
            case POST: 
            servePost(request, response);
            break;
            case PUT:
            servePost(request, response);
            break;
            case DELETE:
            serveDelete(request, response);
            break;
            default:
                throw new WASException(MSG_TYPE.ERROR, 16, request.getRequestType().name());
        }
        if(this.postFilters != null) {
            for(ILeapFilter filter : this.postFilters) {
                List<Method> methods = AnnotationHelper.getFilterMethods(filter, PostFilter.class);
                for(Method method : methods) {
                    ServiceInvoker.invokeMethod(filter, method, response);
                }
            }
        }
        return response;
    }

    @Override
    public void serveGet(final Request request, final Response response) throws Exception {        
        this.invokingMethod.invoke(this, request, response);
    }

    @Override
    public void servePost(final Request request, final Response response) throws Exception {
        this.invokingMethod.invoke(this, request, response);
    }

    @Override
    public void servePut(final Request request, final Response response) throws Exception {
        this.invokingMethod.invoke(this, request, response);
    }

    @Override
    public void serveDelete(final Request request, final Response response) throws Exception {
        this.invokingMethod.invoke(this, request, response);
    }    

    @Override
    public void setFilters(final List<ILeapFilter> preFilters, final List<ILeapFilter> postFilters) {
        this.preFilters = preFilters;
        this.postFilters = postFilters;
    } 

    @Override
    public void sendResponse(final Response response) throws Throwable {
        this.httpTransfer.sendResponse(response);
        this.httpTransfer.close();     
    }

    @Override
    public Resources getResource() {
        return this.resource;
    }

    @Override
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    @Override
    public void setServiceManager(final ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }    
}

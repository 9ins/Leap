package org.chaostocosmos.leap.http.service;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;

import org.chaostocosmos.leap.http.HTTPException;
import org.chaostocosmos.leap.http.HttpTransferBuilder.HttpTransfer;
import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.ServiceInvoker;
import org.chaostocosmos.leap.http.ServiceManager;
import org.chaostocosmos.leap.http.annotation.AnnotationHelper;
import org.chaostocosmos.leap.http.annotation.AnnotationOpr;
import org.chaostocosmos.leap.http.annotation.PostFilter;
import org.chaostocosmos.leap.http.annotation.PreFilter;
import org.chaostocosmos.leap.http.common.LoggerFactory;
import org.chaostocosmos.leap.http.context.Context;
import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;
import org.chaostocosmos.leap.http.enums.RES_CODE;
import org.chaostocosmos.leap.http.resource.ResourcesModel;
import org.chaostocosmos.leap.http.resource.SpringJPAManager;
import org.chaostocosmos.leap.http.service.filter.IFilter;
import org.chaostocosmos.leap.http.service.model.DeleteServiceModel;
import org.chaostocosmos.leap.http.service.model.GetServiceModel;
import org.chaostocosmos.leap.http.service.model.PostServiceModel;
import org.chaostocosmos.leap.http.service.model.PutServiceModel;
import org.chaostocosmos.leap.http.service.model.ServiceModel;
import org.chaostocosmos.leap.http.session.Session;

import ch.qos.logback.classic.Logger;

/**
 * Abstraction of SimpleServlet object
 * 
 * @author Kooin Shin
 * @since 2021.09.15
 */
public abstract class AbstractService implements GetServiceModel, PostServiceModel, PutServiceModel, DeleteServiceModel {
    /**
     * Logger
     */
    protected Logger logger;

    /**
     * Filters for previous filtering process of service method
     */
    protected List<IFilter> preFilters;

    /**
     * Filter for after filtering process of service method
     */
    protected List<IFilter> postFilters;

    /**
     * Leap service manager object
     */
    protected ServiceManager serviceManager;

    /**
     * Resource object
     */
    protected ResourcesModel resource;
    
    /**
     * HttpTransfer object
     */
    protected HttpTransfer httpTransfer;

    /**
     * Request
     */
    protected Request request;

    /**
     * Response
     */
    protected Response response;

    /**
     * Target method
     */
    private Method targetMethod;

    @Override
    public Response serve(final HttpTransfer httpTransfer) throws Exception {        
        this.logger = LoggerFactory.getLogger(httpTransfer.getRequest().getRequestedHost());
        this.httpTransfer = httpTransfer;
        this.resource = this.httpTransfer.getHost().getResource();
        this.request = this.httpTransfer.getRequest();
        this.response = this.httpTransfer.getResponse();

        if(this.preFilters != null) {
            for(IFilter filter : this.preFilters) {
                List<Method> methods = AnnotationHelper.getFilterMethods(filter, PreFilter.class);
                for(Method method : methods) {
                    ServiceInvoker.invokeMethod(filter, method, request);
                }
            }
        }        
        //Set service method
        this.targetMethod = this.serviceManager.getServiceMethod(REQUEST_TYPE.GET, request.getContextPath(), this);
        Class<?>[] paramTypes = this.targetMethod.getParameterTypes();
        if(paramTypes.length != 2 || paramTypes[0] != request.getClass() || paramTypes[1] != response.getClass()) {
            throw new HTTPException(RES_CODE.RES501, Context.getMessages().<String> getErrorMsg(201, this.targetMethod.getName()));
        }
        //setting JPA link
        AnnotationOpr<ServiceModel> annotaionOpr = new AnnotationOpr<>(httpTransfer.getHost().getHost(), this);
        annotaionOpr.injectToAutowired();
        switch(this.request.getRequestType()) {
            case GET :
            GET(this.request, this.response);
            break;
            case POST :
            POST(this.request, this.response);
            break;
            case PUT :
            PUT(this.request, this.response);
            break;
            case DELETE :
            DELETE(this.request, this.response);
            break;
            default :
            throw new HTTPException(RES_CODE.RES405, "Requested method is not allowed: "+request.getRequestType().name());
        }        
        if(this.postFilters != null) {
            for(IFilter filter : this.postFilters) {
                List<Method> methods = AnnotationHelper.getFilterMethods(filter, PostFilter.class);
                for(Method method : methods) {
                    ServiceInvoker.invokeMethod(filter, method, response);
                }
            }
        }
        //Set session cookies
        Session session = this.request.getSession();
        if(session != null) {
            session.setAttribute("__Leap-Session-ID", session.getId());
            Enumeration<String> enu = session.getAttributeNames();        
            while(enu.hasMoreElements()) {
                String attrName = enu.nextElement();
                this.response.addSetCookie(attrName, String.valueOf(session.getAttribute(attrName)));
                System.out.print("Set-Cookie: "+attrName+"="+session.getAttribute(attrName)+"; ");
            }
            System.out.println();    
        }
        return response;
    }

    @Override
    public void GET(final Request request, final Response response) throws Exception {                
        this.targetMethod.invoke(this, request, response);
    }

    @Override
    public void POST(final Request request, final Response response) throws Exception {
        this.targetMethod.invoke(this, request, response);
    }

    @Override
    public void PUT(final Request request, final Response response) throws Exception {
        this.targetMethod.invoke(this, request, response);
    }

    @Override
    public void DELETE(final Request request, final Response response) throws Exception {
        this.targetMethod.invoke(this, request, response);
    }    

    @Override
    public void setFilters(final List<IFilter> preFilters, final List<IFilter> postFilters) {
        this.preFilters = preFilters;
        this.postFilters = postFilters;
    } 

    @Override
    public void sendResponse(final Response response) throws Exception {
        this.httpTransfer.sendResponse(response);
        this.httpTransfer.close();     
    }

    @Override
    public ResourcesModel getResource() {
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

    @Override
    public <T> T getBean(String beanName, Object... args) throws Exception {
        return SpringJPAManager.get().getBean(beanName, args);
    }

    @Override
    public <T> T getBean(Class<?> beanClass, Object... args) throws Exception {
        return SpringJPAManager.get().getBean(beanClass, args);
    }
}
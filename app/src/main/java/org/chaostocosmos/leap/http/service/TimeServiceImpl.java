package org.chaostocosmos.leap.http.service;

import java.util.Date;

import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.annotation.FilterMapper;
import org.chaostocosmos.leap.http.annotation.MethodMapper;
import org.chaostocosmos.leap.http.annotation.ServiceMapper;
import org.chaostocosmos.leap.http.common.LoggerFactory;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;
import org.chaostocosmos.leap.http.service.filter.BasicAuthFilter;
import org.chaostocosmos.leap.http.service.filter.BasicHttpFilter;

/**
 * Time serving servlet object
 * 
 * @author 9ins
 * @since 2021.09.17
 */
@ServiceMapper(path = "/time")
public class TimeServiceImpl extends AbstractService {  

    public String cloneTestString = "";

    /**
     * Get current time
     * @param request
     * @param response
     */
    @MethodMapper(mappingMethod = REQUEST_TYPE.GET, path = "/GetTime")
    @FilterMapper(preFilters = {BasicAuthFilter.class, BasicHttpFilter.class})
    public void getTime(Request request, Response response) {
        LoggerFactory.getLogger(request.getRequestedHost()).debug("getTime servlet started....+++++++++++++++++++++++++++++++++++++++++++++++++");
        String resBody = "<html><title>This is what time</title><body><h2>"+new Date().toString()+"</h2><body></html>";
        response.addHeader("Content-Type", MIME_TYPE.TEXT_HTML.mimeType());
        response.setResponseCode(200);
        response.setBody(resBody.getBytes());
    }

    @Override
    public Exception errorHandling(Response response, Exception e) {
        return e;
    }
} 
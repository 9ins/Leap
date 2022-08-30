package org.chaostocosmos.leap.http.service;

import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.annotation.MethodMapper;
import org.chaostocosmos.leap.http.annotation.ServiceMapper;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;

/**
 * SimpleStreamingService
 * 
 * @author 9ins
 */
@ServiceMapper(path = "")
public class SimpleStreamingService extends AbstractStreamingService {

    public SimpleStreamingService() {
        super(MIME_TYPE.VIDEO_MP4);
    } 

    @MethodMapper(mappingMethod = REQUEST_TYPE.GET, path = "/video")
    public void streaming(Request request, Response response) {
        
    }

    @Override
    public Exception errorHandling(Response response, Exception e) {
        e.printStackTrace();
        return null;
    }    
}
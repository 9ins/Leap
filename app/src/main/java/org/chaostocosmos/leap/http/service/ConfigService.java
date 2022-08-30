package org.chaostocosmos.leap.http.service;

import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.annotation.FilterMapper;
import org.chaostocosmos.leap.http.annotation.MethodMapper;
import org.chaostocosmos.leap.http.annotation.ServiceMapper;
import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;
import org.chaostocosmos.leap.http.service.filter.BasicAuthFilter;
import org.chaostocosmos.leap.http.service.filter.ConfigFilter;

/**
 * ConfigurationService
 * 
 * @author 9ins
 */
@ServiceMapper(path = "/config")
public class ConfigService extends AbstractService {

    @FilterMapper(preFilters = { BasicAuthFilter.class, ConfigFilter.class })
    @MethodMapper(mappingMethod = REQUEST_TYPE.POST, path="/set")
    public void setConfig(Request request) {
    }

    @FilterMapper(preFilters = { BasicAuthFilter.class, ConfigFilter.class })
    @MethodMapper(mappingMethod = REQUEST_TYPE.POST, path="/save")
    public void saveConfig(Request request) {

    }

    @Override
    public Exception errorHandling(Response response, Exception e) {
        return e;
    }    
}
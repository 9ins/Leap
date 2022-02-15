package org.chaostocosmos.leap.http.filters;

import org.chaostocosmos.leap.http.WASException;

/**
 * IHttpRequestFilter
 * @author 9ins
 */
public interface IHttpRequestFilter<R> extends ILeapFilter{
    /**
     * Filter http request before servlet process
     * @param request
     * @return
     */
    public void filterRequest(R request) throws WASException;    
}

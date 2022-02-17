package org.chaostocosmos.leap.http.services;

import org.chaostocosmos.leap.http.HttpRequestDescriptor;
import org.chaostocosmos.leap.http.HttpResponseDescriptor;

/**
 * IDeployService interface
 */
public interface IDeployService extends ILeapService {
    /**
     * Serve service deploy
     * @param request
     * @param response
     * @throws Exception
     */
    public void serveDeploy(HttpRequestDescriptor request, HttpResponseDescriptor response) throws Exception;
}
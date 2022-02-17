package org.chaostocosmos.leap.http;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.chaostocosmos.leap.http.HttpTransferBuilder.HttpTransfer;
import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.chaostocosmos.leap.http.commons.ResourceHelper;
import org.chaostocosmos.leap.http.commons.UtilBox;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;
import org.chaostocosmos.leap.http.services.ServiceHolder;
import org.chaostocosmos.leap.http.services.ServiceInvoker;
import org.chaostocosmos.leap.http.services.ServiceManager;

/**
 * Client request processor object
 * 
 * @author 9ins
 * @since 2021.09.16
 */
public class LeapRequestHandler implements Runnable {
    /**
     * Path doc root
     */
    private Path rootPath;
    /**
     * welcome index html
     */
    private String welcome = Context.getWelcome();
    /**
     * Server
     */
    LeapHttpServer httpServer;
    /**
     * Client socket
     */
    Socket client;

    /**
     * Constructor with HeapHttpServer, root direcotry, index.html file, client socket 
     * @param httpServer
     * @param rootPath
     * @param welcome
     * @param client
     * @throws IOException
     * @throws WASException
     */
    public LeapRequestHandler(LeapHttpServer httpServer, Path rootPath, String welcome, Socket client) throws IOException, WASException {
        this.httpServer = httpServer;
        this.client = client;
        this.rootPath = rootPath;
        if (welcome != null) {
            this.welcome = welcome;
        }
    }

    @Override
    public void run() {
        HttpRequestDescriptor request = null;
        HttpResponseDescriptor response = null;
        HttpTransfer httpTransfer = null;
        String requestedHost = null;
        try {            
            httpTransfer = HttpTransferBuilder.buildHttpTransfer(httpServer.getHost(), this.client);
            request = httpTransfer.getRequest();
            response = httpTransfer.getResponse();
            if(request != null && response != null) {
                requestedHost = request.getRequestedHost();            
                //Put requested host to request header Map for ip filter
                request.getReqHeader().put("@Client", requestedHost);
                //LoggerFactory.getLogger(requestedHost).debug("Request host: "+requestedHost); 
                Path resourcePath = ResourceHelper.getResourcePath(request);
                ServiceManager serviceManager = httpServer.getServiceManager();
                ServiceHolder serviceHolder = serviceManager.getMappingServiceHolder(request.getContextPath());
    
                //If client request context path in Services.
                if (serviceHolder != null) {
                    // Request method validation
                    if(serviceHolder.getRequestType() != request.getRequestType()) {
                        throw new WASException(MSG_TYPE.HTTP, 405, "<br>Not supported: "+request.getRequestType().name());
                    } else if (serviceManager.vaildateRequestMethod(request.getRequestType(), request.getContextPath())) {
                        // Do requested service to execute
                        response = ServiceInvoker.invokeService(serviceHolder, httpTransfer, true);
                    } else {
                        String message = Context.getHttpMsg(405);
                        String body = ResourceHelper.getResponsePage(requestedHost, Map.of("@code", 405, "@message", message));
                        response.addHeader("Content-Type", MIME_TYPE.TEXT_HTML);
                        response.setBody(body.getBytes());
                        response.setStatusCode(405);                    
                    }
                } else { // When client request static resources
                    if(request.getContextPath().equals("/")) {
                        String body = ResourceHelper.getWelcomePage(requestedHost, Map.of("@serverName", requestedHost));
                        response.addHeader("Content-Type", MIME_TYPE.TEXT_HTML);
                        response.setBody(body.getBytes());
                        response.setStatusCode(200);
                    } else {
                        if (resourcePath.toFile().exists()) {
                            File resourceFile = resourcePath.toFile();
                            String resourceName = resourceFile.getName();
                            String mimeType = UtilBox.probeContentType(resourcePath);
                            LoggerFactory.getLogger(requestedHost).debug("DOWNLOAD RESOURCE MIME-TYPE: "+mimeType);
                            //Condition of requeset resource in forbidden list
                            if(Context.getResourceForbidden().stream().anyMatch(f -> !f.trim().equals("") && resourceName.matches(Arrays.asList(f.split(Pattern.quote("*"))).stream().map(s -> s.equals("") ? "" : Pattern.quote(s)).collect(Collectors.joining(".*"))+".*"))) {
                                String message = Context.getHttpMsg(403);
                                String body = ResourceHelper.getResponsePage(requestedHost, Map.of("@code", 403, "@type", "HTTP", "@message", message));
                                response.addHeader("Content-Type", MIME_TYPE.TEXT_HTML);
                                response.setBody(body.getBytes());
                                response.setStatusCode(403);
                            } else if(Context.getResourceAllowed().stream().anyMatch(a -> !a.trim().equals("") && resourceName.matches(Arrays.asList(a.split(Pattern.quote("*"))).stream().map(s -> s.equals("") ? "" : Pattern.quote(s)).collect(Collectors.joining(".*"))+".*"))) {
                                //Condition of request resource in allowed list
                                response.addHeader("Content-Type", mimeType);
                                response.setBody(resourcePath);
                                response.setStatusCode(200);
                            } else {
                                LoggerFactory.getLogger(requestedHost).debug("Not allowed resource requested: "+resourceName);
                            }
                        } else {
                            //When requested resource is not found
                            throw new WASException(MSG_TYPE.HTTP, 404, request.getContextPath());
                        }
                    }
                }                    
            } else {
                LoggerFactory.getLogger(requestedHost).warn(Context.getErrorMsg(50, this.client.getRemoteSocketAddress().toString()));
            }
        } catch(Throwable t) {
            try {
                String responseMessage = HttpTransferBuilder.buildErrorResponse(requestedHost, t);
                if(response != null) {
                    response.setBody(responseMessage.getBytes(Context.charset()));                
                }
            } catch(Exception e) {
                LoggerFactory.getLogger(requestedHost).error(e.getMessage(), e);
            }
        } finally {
            try {
                if(response != null) {
                    //Send response to client
                    httpTransfer.sendResponse(response);
                }
                httpTransfer.close();
            } catch (Exception e) {
                LoggerFactory.getLogger(requestedHost).error(e.getMessage(), e);
            }
        }
    }
}
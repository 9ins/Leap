package org.chaostocosmos.leap.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;
import org.chaostocosmos.leap.http.enums.RES_CODE;
import org.chaostocosmos.leap.http.resources.Context;
import org.chaostocosmos.leap.http.resources.Hosts;
import org.chaostocosmos.leap.http.resources.HostsManager;
import org.chaostocosmos.leap.http.resources.StaticResourceManager;

/**
 * HttpResponseTransfer
 * 
 * @author 9ins
 */
public class HttpTransferBuilder {
    /**
     * Build error response messasge
     * @param requestedHost
     * @param e
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws WASException
     */
    public static String buildErrorResponse(String requestedHost, MSG_TYPE msgType, int errorCode, String errorMsg) throws IOException, InterruptedException {
        LoggerFactory.getLogger(requestedHost).error("["+msgType.name()+": "+errorCode+"] - "+errorMsg);
        return buildHttpErrorPage(requestedHost, msgType, errorCode, errorMsg);
    }

    /**
     * Build http error page
     * @param host
     * @param type
     * @param errorCode
     * @param message
     * @return
     * @throws IOException
     */
    public static String buildHttpErrorPage(String host, MSG_TYPE type, int errorCode, String message) throws IOException {
        String title = Context.getHttpMsg(errorCode, "- "+type.name());        
        System.out.println(title+"  "+message);
        Map<String, Object> map = Map.of("@code", errorCode, "@type", title, "@message", message);
        return StaticResourceManager.get(host).getErrorPage(map);
    }

    /**
     * Create http response page
     * @param host
     * @param type
     * @param code
     * @param message
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static String buildHttpResponsePage(String host, MSG_TYPE type, int code, String message) throws IOException, InterruptedException {
        Map<String, Object> map = Map.of("@code", code, "@type", type.name(), "@message", message);
        return StaticResourceManager.get(host).getResponsePage(map);
    }

    /**
     * Build HttpTransfer object
     * @param vHost
     * @param client
     * @throws IOException
     * @throws Exception
     */
    public static HttpTransfer buildHttpTransfer(String vHost, Socket client) throws IOException {
        return new HttpTransfer(vHost, client);
    } 

    /**
     * Add key-value to header Map
     * @param headers
     * @param key
     * @param value
     * @return
     */
    public static Map<String, List<Object>> addHeader(Map<String, List<Object>> headers, String key, Object value) {
        if(headers != null) {
            headers = new HashMap<>();
        }
        List<Object> values = headers.get(key);
        if(values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
        headers.put(key, values);
        return headers;
    }

    /**
     * HttpTransfer object
     * @author 9ins
     */
    public static class HttpTransfer {
        /**
         * Hosts, Information configured in config.yml
         */
        Hosts hosts;
        
        /**
         * Client Socket
         */
        Socket socket;

        /**
         * InputStream
         */
        InputStream clientInputStream;

        /**
         * OutputStream
         */
        OutputStream clientOutputStream;

        /**
         * Http request
         */
        HttpRequestDescriptor httpRequestDescriptor;

        /**
         * Http response
         */
        HttpResponseDescriptor httpResponseDescriptor;

        /**
         * Construct with request host and client socket
         * @param host
         * @param client
         * @throws IOException
         */
        public HttpTransfer(String host, Socket client) throws IOException {
            this.hosts = HostsManager.get().getHosts(host);
            this.socket = client;
            this.clientInputStream = client.getInputStream();
            this.clientOutputStream = client.getOutputStream();
        }

        /**
         * Get requested host
         * @return
         */
        public Hosts getHosts() {
            return this.hosts;
        }

        /**
         * Get client InputStream
         * @return
         */
        public InputStream getClientInputStream() {
            return clientInputStream;
        }

        /**
         * Get client OutputStream
         * @return
         */
        public OutputStream getClientOutputStream() {
            return clientOutputStream;
        }

        /**
         * Get HttpRequestDescriptor
         * @return
         * @throws IOException
         */
        public HttpRequestDescriptor getRequest() throws IOException {
            if(this.httpRequestDescriptor == null) {
                this.httpRequestDescriptor = parseRequest();
            }
            return this.httpRequestDescriptor;
        }

        /**
         * Get HttpResponseDescriptor
         * @return
         * @throws IOException
         * @throws InterruptedException
         */
        public HttpResponseDescriptor getResponse() throws IOException, InterruptedException {
            if(this.httpResponseDescriptor == null) {
                String msg = buildHttpResponsePage(this.httpRequestDescriptor.getRequestedHost(), MSG_TYPE.HTTP, 200, Context.getHttpMsg(200));
                Map<String, List<Object>> headers = addHeader(new HashMap<>(), "Content-Type", MIME_TYPE.TEXT_HTML.getMimeType());
                headers = addHeader(new HashMap<>(), "Content-Length", msg.getBytes().length);
                this.httpResponseDescriptor = HttpResponseBuilder.getBuilder()
                                                                .build(this.httpRequestDescriptor)
                                                                .setStatusCode(200)
                                                                .setBody(msg)
                                                                .setHeaders(headers)
                                                                .get();
            }
            return this.httpResponseDescriptor;
        }

        /**
         * Get HttpRequestDescriptor object
         * @return
         * @throws IOException
         */
        private HttpRequestDescriptor parseRequest() throws IOException {
            return HttpParser.buildRequestParser().parseRequest(clientInputStream);
        }

        /**
         * Send response to client by HttpResponseDescriptor object
         * @param response
         * @throws IOException
         */
        public void sendResponse(HttpResponseDescriptor response) throws IOException {
            sendResponse(response.getRequestedHost(), response.getStatusCode(), response.getHeaders(), response.getBody());
        }

        /**
         * Send response to client by requested host, status code, reponse headers, body object
         * @param host
         * @param resCode
         * @param headers
         * @param body
         * @throws IOException
         */
        public void sendResponse(String host, int resCode, Map<String, List<Object>> headers, Object body) throws IOException {
            Charset charset = HostsManager.get().charset(host);
            String str = HostsManager.get().getHosts(host).getProtocol().name();
            String protocol = str.substring(0, str.indexOf("_"));
            String version = str.substring(str.indexOf("_")+1).replace("_", ".");
            String resMsg = null;
            if(resCode >= 200 && resCode <= 600) {
                resMsg = RES_CODE.valueOf("RES"+resCode).getMessage();
            } else {
                resMsg = Context.getErrorMsg(resCode, host);
            }
            String res = protocol+"/"+version+" "+resCode+" "+resMsg+"\r\n"; 
            this.clientOutputStream.write(res.getBytes());
            if(body == null) {
                LoggerFactory.getLogger(host).warn("Response body is Null: "+resCode);
                return ;
            }            
            long contentLength = -1;
            if(body instanceof byte[]) {
                contentLength = ((byte[])body).length;
            } else if(body instanceof String) {
                contentLength = ((String)body).getBytes(charset).length;
            } else if(body instanceof Path) {
                contentLength = ((Path)body).toFile().length();
            } else if(body instanceof File) {
                contentLength = ((File)body).length();
            } else {
                throw new WASException(MSG_TYPE.ERROR, 4, body.getClass().getName());
            }
            List<Object> values = new ArrayList<>();
            values.add(contentLength);
            headers.put("Content-Length", values);

            //LoggerFactory.getLogger(response.getRequestedHost()).debug(response.toString());
            StringBuffer resStr = new StringBuffer();
            resStr.append("============================== RES : "+res.trim()+" - "+this.socket.getRemoteSocketAddress().toString()+" =============================="+System.lineSeparator());
            resStr.append("RES CODE: "+resCode+System.lineSeparator());
            for(Map.Entry<String, List<Object>> e : headers.entrySet()) {
                this.clientOutputStream.write((e.getKey()+": "+e.getValue().stream().map(v -> v.toString()).collect(Collectors.joining("; "))+"\r\n").getBytes());
                resStr.append(e.getKey()+": "+e.getValue()+System.lineSeparator());
            }
            LoggerFactory.getLogger(host).debug(resStr.substring(0, resStr.length()-1));
            this.clientOutputStream.write("\r\n".getBytes());
            this.clientOutputStream.flush(); 
            if(body instanceof byte[]) {
                this.clientOutputStream.write((byte[]) body);
            } else {
                if(body instanceof String) {                                        
                    this.clientOutputStream.write(body.toString().getBytes(charset));
                } else if(body instanceof File) {
                    writeToStream((File)body, this.clientOutputStream, Context.getFileBufferSize());
                } else if(body instanceof Path) {
                    writeToStream(((Path)body).toFile(), this.clientOutputStream, Context.getFileBufferSize());
                } else {
                    throw new IllegalArgumentException("Not supported response body type: "+body.getClass().getName());
                }
            }
            this.clientOutputStream.flush();
        }

        /**
         * Write resource to OutputStream for client
         * @param resource
         * @param out
         * @param bufferSize
         * @throws IOException
         */
        private void writeToStream(File resource, OutputStream out, int bufferSize) throws IOException {
            byte[] buffer = new byte[bufferSize];
            FileInputStream in = new FileInputStream(resource);
            int len;
            while((len=in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
        }

        /**
         * Close client connection
         * @throws IOException
         */
        public void close() {
            try {
                if(this.clientInputStream != null) {
                    this.clientInputStream.close();
                }
                if(this.clientOutputStream != null) {
                    this.clientOutputStream.close();
                }
                if(this.socket != null) {
                    this.socket.close();
                }    
            } catch(Exception e) {
                LoggerFactory.getLogger(this.hosts.getHost()).error(e.getMessage(), e);
            }
            LoggerFactory.getLogger(this.hosts.getHost()).info("Client closing......"+socket.getInetAddress().toString());
        }
    }
}

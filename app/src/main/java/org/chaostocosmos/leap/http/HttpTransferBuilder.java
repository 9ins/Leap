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

import org.chaostocosmos.leap.http.commons.Constants;
import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.chaostocosmos.leap.http.context.Context;
import org.chaostocosmos.leap.http.context.Host;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;
import org.chaostocosmos.leap.http.enums.RES_CODE;
import org.chaostocosmos.leap.http.resources.TemplateBuilder;

/**
 * HttpResponseTransfer
 * 
 * @author 9ins
 */
public class HttpTransferBuilder {
    /**
     * Build HttpTransfer object
     * @param hostId
     * @param client
     * @throws IOException
     * @throws Exception
     */
    public static HttpTransfer buildHttpTransfer(String hostId, Socket client) throws IOException {
        return new HttpTransfer(hostId, client);
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
        Host<?> host;
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
        Request httpRequestDescriptor;
        /**
         * Http response
         */
        Response httpResponseDescriptor;
        /**
         * Whether closed
         */
        boolean isClosed = false;
        /**
         * Construct with request host and client socket
         * @param hostId
         * @param socket
         * @throws IOException
         */
        public HttpTransfer(String hostId, Socket socket) throws IOException {
            this.host = Context.getHosts().getHost(hostId);
            this.socket = socket;
            this.clientInputStream = socket.getInputStream();
            this.clientOutputStream = socket.getOutputStream();
        }
        /**
         * Get requested host
         * @return
         */
        public Host<?> getHost() {
            return this.host;
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
         * @throws Exception
         */
        public Request getRequest() throws Exception {
            if(this.httpRequestDescriptor == null) {
                this.httpRequestDescriptor = parseRequest();
            }
            return this.httpRequestDescriptor;
        }
        /**
         * Get HttpResponseDescriptor
         * @return
         * @throws Exception
         * @throws ImageProcessingException
         */
        public Response getResponse() throws Exception {
            if(this.httpResponseDescriptor == null) {
                String msg = TemplateBuilder.buildResponseHtml(this.host, MSG_TYPE.HTTP, 200, Context.getMessages().getHttpMsg(200));
                Map<String, List<Object>> headers = addHeader(new HashMap<>(), "Content-Type", MIME_TYPE.TEXT_HTML.mimeType());
                headers = addHeader(new HashMap<>(), "Content-Length", msg.getBytes().length);
                headers = addHeader(new HashMap<>(), "Charset", this.host.<String> charset());
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
         * @throws Exception
         */
        private Request parseRequest() throws Exception {
            return HttpParser.buildRequestParser().parseRequest(socket.getInetAddress(), clientInputStream);
        }
        /**
         * Send response to client by HttpResponseDescriptor object
         * @param response
         * @throws IOException
         */
        public void sendResponse(Response response) throws Exception {
            sendResponse(response.getHostId(), response.getResponseCode(), response.getHeaders(), response.getBody());
        }
        /**
         * Send response to client by requested host, status code, reponse headers, body object
         * @param hostId
         * @param resCode
         * @param headers
         * @param body
         * @throws IOException
         */
        public void sendResponse(String hostId, int resCode, Map<String, List<Object>> headers, Object body) throws IOException {
            Charset charset = Context.getHosts().charset(hostId);
            String protocol = Context.getHosts().getHost(hostId).<String> getProtocol();
            String resMsg = null;
            if(resCode >= 200 && resCode <= 600) {
                resMsg = RES_CODE.valueOf("RES"+resCode).msg();
            } else {
                resMsg = Context.getMessages().getErrorMsg(resCode, hostId);
            }
            String res = protocol+"/"+Constants.HTTP_VERSION+" "+resCode+" "+resMsg+"\r\n"; 
            this.clientOutputStream.write(res.getBytes());
            if(body == null) {
                LoggerFactory.getLogger(hostId).warn("Response body is Null: "+resCode);
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
            resStr.append("============================== [RESPONSE] : "+res.trim()+" - "+this.socket.getRemoteSocketAddress().toString()+" =============================="+System.lineSeparator());
            resStr.append("RES CODE: "+resCode+System.lineSeparator());
            for(Map.Entry<String, List<Object>> e : headers.entrySet()) {
                String hv = e.getValue().stream().map(v -> v.toString()).collect(Collectors.joining("; "));
                this.clientOutputStream.write((e.getKey()+": "+hv+"\r\n").getBytes());
                resStr.append(e.getKey()+": "+e.getValue()+System.lineSeparator());
            }
            LoggerFactory.getLogger(hostId).debug(resStr.substring(0, resStr.length()-1));
            this.clientOutputStream.write("\r\n".getBytes()); 
            this.clientOutputStream.flush(); 
            if(body instanceof byte[]) {
                this.clientOutputStream.write((byte[]) body);
            } else { 
                if(body instanceof String) {                                       
                    this.clientOutputStream.write(body.toString().getBytes(charset));
                } else if(body instanceof File) {
                    writeToStream((File)body, this.clientOutputStream, Context.getServer().getFileBufferSize());
                } else if(body instanceof Path) {
                    writeToStream(((Path)body).toFile(), this.clientOutputStream, Context.getServer().getFileBufferSize());
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
                if(this.socket != null && !this.socket.isClosed()) {
                    this.socket.close();
                }    
            } catch(Exception e) {
                LoggerFactory.getLogger(this.host.getHost()).error(e.getMessage(), e);
            }
            LoggerFactory.getLogger(this.host.getHost()).info("Client closing......"+socket.getInetAddress().toString());
        }

        /**
         * Whether client socket is closed
         * @return
         */
        public boolean isClosed() {
            if(this.socket != null) {
                return this.socket.isClosed();
            }
            return false;
        }
    }
}

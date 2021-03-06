package org.chaostocosmos.leap.http.part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.chaostocosmos.leap.http.WASException;
import org.chaostocosmos.leap.http.commons.StreamUtils;
import org.chaostocosmos.leap.http.context.Context;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;

/**
 * Multi part descriptor
 * 
 * @author 9ins
 */
public class MultiPart extends BodyPart {
    /**
     * Multipart saved files
     */
    List<Path> filePaths;
    /**
     * Multipart boundary
     */
    String boundary;
    /**
     * Previously load body to MultiPart memory
     */
    boolean preLoadBody;
    /**
     * Constructor of Multipart
     * @param host
     * @param contentType
     * @param boundary
     * @param contentLength
     * @param requestStream
     * @param preLoadBody
     * @param charset
     * @throws IOException
     */
    public MultiPart(String host, MIME_TYPE contentType, String boundary, long contentLength, InputStream requestStream, boolean preLoadBody, Charset charset) throws IOException {
        super(host, contentType, contentLength, requestStream, false, charset);
        this.filePaths = new ArrayList<>();
        this.boundary = boundary;
        this.preLoadBody = preLoadBody;
        if(this.preLoadBody) {
            super.body = getBody();
        }
    }

    @Override
    public Map<String, byte[]> getBody() throws IOException {
        if(super.body == null) {
            return StreamUtils.getMultiPartContents(this.host, this.requestStream, this.boundary, super.charset);
        }
        return super.body;
    }

    /**
     * Get saved file Paths
     * @return
     */
    public List<Path> getFilePaths() {
        return this.filePaths;
    }
    /**
     * Get boundary String
     * @return
     */
    public String getBoundary() {
        return this.boundary;
    }
    /**
     * Delete all Multi-Part files
     */
    public void deleteFiles() {
        this.filePaths.stream().forEach(p -> p.toFile().delete());
    }
    
    @Override
    public void save(final Path targetPath) throws IOException {
        if(contentType != MIME_TYPE.MULTIPART_FORM_DATA || contentType != MIME_TYPE.MULTIPART_BYTERANGES) {
            throw new WASException(MSG_TYPE.ERROR, 23, "Can not save content. Not supported on Multi Part Operation.");
        }        
        if(targetPath.toFile().isDirectory()) {
            throw new IOException("Multi part saving must be provided directory Path: "+targetPath.toString());
        }
        if(super.isLoadedBody) {
            super.body.entrySet().stream().forEach(e -> {
                try {
                    Path path = targetPath.resolve(e.getKey());
                    Files.write(path, e.getValue(), StandardOpenOption.TRUNCATE_EXISTING);
                    filePaths.add(path);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        } else {
            this.filePaths = StreamUtils.saveMultiPart(this.host, super.requestStream, targetPath.normalize(), Context.getServer().getFileBufferSize(), this.boundary, super.charset);    
        }
        super.logger.debug("[MULTI-PART] "+super.contentType.name()+" saved: "+targetPath.normalize().toString()+"  Size: "+filePaths.stream().map(p -> p.toFile()).map(f -> f.getName()+": "+f.length()).collect(Collectors.joining(", ")));
    }    

    @Override
    public String toString() {
        return super.toString() + "{" +
            " filePaths='" + filePaths + "'" +
            ", boundary='" + boundary + "'" +
            "}";
    }
}

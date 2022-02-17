package org.chaostocosmos.leap.http.commons;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

import org.chaostocosmos.leap.http.Context;

/**
 * FileUtils
 * 
 * @author 9ins
 */
public class FileUtils {

    /**
     * Save binary data
     * @param data
     * @param target
     * @throws IOException
     */
    public static void saveBinary(byte[] data, Path target) throws IOException {
        saveBinary(data, target, Context.getFileBufferSize());
    }

    /**
     * Save binary data
     * @param data
     * @param target
     * @param bufferSize
     * @throws IOException
     */
    public static void saveBinary(byte[] data, Path target, int bufferSize) throws IOException {
        FileOutputStream out = new FileOutputStream(target.toFile());
        bufferSize = bufferSize < data.length ? data.length : bufferSize;
        int pos = 0;
        int rest = data.length % bufferSize;
        int times = data.length / bufferSize;
        byte[] part;
        for(int i=0; i<times; i++) {
            part = Arrays.copyOfRange(data, pos, bufferSize);
            out.write(part);
            pos += bufferSize;            
        }
        if(rest > 0) {
            part = Arrays.copyOfRange(data, pos, rest);
            out.write(part);
        }
        out.close();                                
    }

    /**
     * Save text
     * @param data
     * @param taget
     * @param charset
     * @throws IOException
     */
    public static void saveText(byte[] data, Path taget, Charset charset) throws IOException {
        saveText(data, taget, charset, Context.getFileBufferSize());
    }

    /**
     * Save text
     * @param data
     * @param target
     * @param charset
     * @param bufferSize
     * @throws IOException
     */
    public static void saveText(byte[] data, Path target, Charset charset, int bufferSize) throws IOException {
        FileWriter out = new FileWriter(target.toFile());
        out.write(new String(data, charset));
        out.close();        
    }    
}
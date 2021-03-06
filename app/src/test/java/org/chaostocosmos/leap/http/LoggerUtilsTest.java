package org.chaostocosmos.leap.http;

import java.io.IOException;
import java.net.URISyntaxException;

import org.chaostocosmos.leap.http.WASException;
import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.slf4j.Logger;

public class LoggerUtilsTest {

    public LoggerUtilsTest() throws WASException, IOException, URISyntaxException {
        Logger logger = LoggerFactory.getLogger("127.0.0.1");
    }

    public static void main(String[] args) throws Exception {
        new LoggerUtilsTest(); 
    }
}

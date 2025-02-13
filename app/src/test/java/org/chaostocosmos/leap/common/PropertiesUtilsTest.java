package org.chaostocosmos.leap.common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.transaction.NotSupportedException;

import org.chaostocosmos.leap.resource.config.ConfigUtils;
import org.junit.Test;

public class PropertiesUtilsTest {

    @Test
    public void testFlattenMap() {
        

    }

    @Test
    public void testLoadConfig() throws URISyntaxException, NotSupportedException, IOException {
        Path configPath = Paths.get(getClass().getResource("resource.yml").toURI());
        Map<String, Object> prop = ConfigUtils.loadConfig(configPath);
        System.out.println(prop.toString());
    }

    @Test
    public void testPropertiesToMap() {
        
    }
}

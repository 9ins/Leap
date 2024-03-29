package org.chaostocosmos.leap.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.NotSupportedException;

import org.chaostocosmos.leap.context.Context;
import org.chaostocosmos.leap.context.Host;
import org.chaostocosmos.leap.resource.LeapURLClassLoader;
import org.chaostocosmos.leap.service.filter.IFilter;
import org.chaostocosmos.leap.service.model.ServiceModel;

/**
 * ClassUtils object
 * 
 * @author 9ins
 */
public class ClassUtils {

    /**
     * Leap class loader
     */
    private static LeapURLClassLoader classLoader = null;

    /**
     * Get class loader for Leap
     * @return
     * @throws MalformedURLException
     */
    public static LeapURLClassLoader getClassLoader() {
        if(classLoader == null) {
            classLoader = new LeapURLClassLoader(Context.get().hosts().getAllDynamicClasspathURLs());
        }
        return classLoader;
    }

    /**
     * Find All Leap service instance
     * @param classLoader
     * @param reloadConfig
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws NotSupportedException
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends ServiceModel>> findAllLeapServices(URLClassLoader classLoader, boolean reloadConfig, Filtering filters) throws IOException, URISyntaxException, NotSupportedException {
        if(reloadConfig) {
            Context.get().refresh();
        }
        List<Class<? extends ServiceModel>> services = findClasses(classLoader, ServiceModel.class, classLoader.getResource(""), null)
                                                       .stream()
                                                       .filter(f ->!Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers()))
                                                       .map(c -> (Class<? extends ServiceModel>) c)
                                                       .collect(Collectors.toList());
        for(URL url : classLoader.getURLs()) {
            services.addAll(findClasses(classLoader, ServiceModel.class, url, filters)
                            .stream()
                            .filter(f ->!Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers()))
                            .map(c -> (Class<? extends ServiceModel>)c)
                            .collect(Collectors.toList()));
        }
        return services;
    }

    /**
     * Get all Leap filters
     * @param classLoader
     * @param reloadConfig
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<Class<? extends IFilter>> findAllLeapFilters(URLClassLoader classLoader, boolean reloadConfig, Filtering filters) throws IOException, URISyntaxException {
        List<Class<? extends IFilter>> filterClasses = findFilters(classLoader, IFilter.class, classLoader.getResource(""), null)
                                                          .stream()
                                                          .filter(f -> //f.isAssignableFrom(IFilter.class)
                                                                      !Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers())
                                                                 )
                                                          .map(f -> (Class<? extends IFilter>) f)
                                                          .collect(Collectors.toList());
        for(URL url : classLoader.getURLs()) {
            filterClasses.addAll(findFilters(classLoader, IFilter.class, url, filters)
                                 .stream()
                                 .filter(f -> //f.isAssignableFrom(IFilter.class)
                                            !Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers()))
                                 .map(f -> (Class<? extends IFilter>)f)
                                 .collect(Collectors.toList()));

        }
        return filterClasses;
    }

    /**
     * Find pre filters
     * @return
     * @param classLoader
     * @param url
     * @param filters
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<Class<? extends IFilter>> findPreFilters(URLClassLoader classLoader, URL url, Filtering filters) throws IOException, URISyntaxException {
        return findFilters(classLoader, IFilter.class, url, filters)
                    .stream()
                    .filter(f -> !Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers()))
                    .map(f -> (Class<? extends IFilter>)f)
                    .collect(Collectors.toList());
    }

    /**
     * Find post filters
     * @param classLoader the class loader
     * @param url
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<Class<? extends IFilter>> findPostFilters(URLClassLoader classLoader, URL url, Filtering filters) throws IOException, URISyntaxException {
        return findFilters(classLoader, IFilter.class, url, filters)
                    .stream()
                    .filter(f -> !Modifier.isAbstract(f.getModifiers()) && !Modifier.isInterface(f.getModifiers()))
                    .map(f -> (Class<? extends IFilter>)f)
                    .collect(Collectors.toList());
    }

    /**
     * Find all filters
     * @param classLoader
     * @param iFilter
     * @param url
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends IFilter>> findFilters(URLClassLoader classLoader, Class<? extends IFilter> iFilter, URL url, Filtering filters) throws IOException, URISyntaxException {
        return findClasses(classLoader, iFilter, url, filters)
               .stream()
               .map(c -> (Class<? extends IFilter>)c)
               .collect(Collectors.toList());
    }

    /**
     * Find dynamic classes
     * @param classLoader
     * @param clazz
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<Class<? extends Object>> findDynamicClasses(URLClassLoader classLoader, Class<?> clazz, Filtering filters) throws IOException, URISyntaxException {
        List<Class<? extends Object>> classes = new ArrayList<>();
        for(URL url : classLoader.getURLs()) {
            classes.addAll(findClasses(classLoader, clazz, url, filters));
        }
        return classes;
    }

    /**
     * Find all classes
     * @param classLoader
     * @param clazz
     * @param url
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<Class<? extends Object>> findClasses(URLClassLoader classLoader, Class<?> clazz, URL url, Filtering filters) throws IOException, URISyntaxException {
        List<String> classes = findClassNames(url, filters);
        return classes.stream().map(c -> getClass(classLoader, c)).filter(c -> c != null && clazz.isAssignableFrom(c)).collect(Collectors.toList());
    }

    /**
     * Get class names from URL
     * @param url
     * @param filters
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static List<String> findClassNames(URL url, Filtering filters) throws IOException, URISyntaxException {
        List<String> classes = null;
        String protocol = url.getProtocol();
        if(protocol.equals("file")) {
            classes = Files.walk(Paths.get(url.toURI()))
                           .filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class"))
                           .map(p -> p.toString().substring(new File(url.getFile()).getAbsolutePath().length()+1).replace(File.separator, "."))
                           .filter(pkg -> filters == null || filters.include(pkg))
                           .map(c -> c.substring(0, c.lastIndexOf(".")))
                           .collect(Collectors.toList());

        } else if(protocol.equals("jar")) {
            try(FileSystem filesystem = FileSystems.newFileSystem(url.toURI(), new HashMap<>())) {
                classes = Files.walk(filesystem.getPath(""))
                            .filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class"))
                            .map(p -> p.toString().replace(File.separator, "."))
                            .filter(pkg -> filters == null || filters.include(pkg))
                            .map(c -> c.substring(0, c.lastIndexOf(".")))
                            .collect(Collectors.toList());
            }
        } else {
            throw new IllegalArgumentException("Protocol not collect!!!");
        }
        return classes;
    }

    /**
     * Get Class object
     * @param classLoader
     * @param className
     * @return
     */
    public static Class<?> getClass(ClassLoader classLoader, String className) { 
        Class<?> clazz = null;
        try {
            clazz = classLoader.loadClass(className);
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return clazz;
    }

    /**
     * Instantiate with specified name
     * @param classLoader
     * @param qualifiedClassName
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @SuppressWarnings("unchecked")
    public static <T> T instantiate(ClassLoader classLoader, 
                                    String qualifiedClassName, 
                                    Object ... params) throws NoSuchMethodException, 
                                                              SecurityException, 
                                                              IllegalArgumentException, 
                                                              InvocationTargetException, 
                                                              ClassNotFoundException, 
                                                              InstantiationException, 
                                                              IllegalAccessException {
        Class<?> clazz = classLoader.loadClass(qualifiedClassName);
        Constructor<T> constructor = (Constructor<T>) clazz.getConstructor(Arrays.asList(params).stream().map(o -> o.getClass()).toArray(Class<?>[]::new));
        return constructor.newInstance(params);
    }

    /**
     * Instantiate with specified Class
     * @param classLoader
     * @param clazz
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static <T> T instantiate(URLClassLoader classLoader, 
                                    Class<?> clazz, 
                                    Object... params) throws NoSuchMethodException, 
                                                               SecurityException, 
                                                               IllegalArgumentException, 
                                                               InvocationTargetException, 
                                                               ClassNotFoundException, 
                                                               InstantiationException, 
                                                               IllegalAccessException {
        return ClassUtils.<T> instantiate(classLoader, clazz.getName(), params);
    }

    /**
     * @param <T>
     * @param classLoader
     * @param qualifiedClassName
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static <T> T instantiateDefaultConstructor(ClassLoader classLoader, String qualifiedClassName) throws NoSuchMethodException, 
                                                                                                                 SecurityException, 
                                                                                                                 IllegalArgumentException, 
                                                                                                                 InvocationTargetException, 
                                                                                                                 ClassNotFoundException, 
                                                                                                                 InstantiationException, 
                                                                                                                 IllegalAccessException {
        return ClassUtils.<T> instantiate(classLoader, qualifiedClassName, new Object[0]);
    }

    /**
     * Get Hosts object mapping with Map
     * @param map
     * @param isDefaultHost     * 
     * @return
     * @throws IOException
     * @throws ImageProcessingException
     */
    public static Host<?> mappingToHost(Map<String, Object> map) throws IOException {
        return new Host<Map<String, Object>>(map);
    }

    /**
     * Get Map object from Hosts object
     * @param host
     * @return
     */
    public static Map<String, Object> mappingToMap(Host<Map<String, Object>> host) {
        Map<String, Object> map = new HashMap<>();
        map.put("default", host.isDefaultHost());
        map.put("hostname", host.getHostId());
        map.put("protocol", host.<String> getProtocol());
        map.put("charset", host.<String> charset());
        map.put("host", host.getHost());
        map.put("port", host.getPort());
        map.put("users", host.getValue("users"));
        map.put("dynamic-classpath", host.getDynamicClasspaths().toString());
        map.put("dynamic-packages", host.getDynamicPackages());
            Map<Object, Object> filterMap = new HashMap<>();
            filterMap.put("in-memory-filters", host.getInMemoryFiltering()); 
            filterMap.put("access-filters", host.getAccessFiltering());
            filterMap.put("forbidden-filters", host.getForbiddenFiltering());
        map.put("resource", filterMap);
            Map<Object, Object> ipFilterMap = new HashMap<>();
            ipFilterMap.put("allowed", host.getIpAllowedFiltering());
            ipFilterMap.put("forbidden", host.getIpForbiddenFiltering());
        map.put("ip-filter", ipFilterMap);
        map.put("home", host.getDocroot().toString());
        map.put("welcome", host.getWelcomeFile().toPath().toString());
        map.put("logs", host.getLogPath().toString());
        map.put("log-level", host.getLogLevel().stream().map(l -> l.toString()).collect(Collectors.joining(",")));
        map.put("details", host.getLogsDetails());
        return map;
    }
}

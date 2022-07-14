package org.chaostocosmos.leap.http.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.transaction.NotSupportedException;

import org.chaostocosmos.leap.http.WASException;
import org.chaostocosmos.leap.http.commons.Constants;
import org.chaostocosmos.leap.http.commons.Filtering;
import org.chaostocosmos.leap.http.commons.LoggerFactory;
import org.chaostocosmos.leap.http.commons.UNIT;
import org.chaostocosmos.leap.http.context.Context;
import org.chaostocosmos.leap.http.context.Host;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.MSG_TYPE;
import org.chaostocosmos.leap.http.enums.RES_CODE;

import com.google.gson.Gson;

/**
 * WatchResource object
 * 
 * @author 9ins
 */
public class WatchResources extends Thread implements Resources {    
    String hostId;
    Host<?> host;
    Path watchPath;
    Kind<?>[] watchKind;
    Filtering accessFiltering, forbiddenFiltering, inMemoryFiltering;
    long inMemoryLimitSize;
    ResourceInfo resourceTree;
    Map<WatchKey, Path> watchMap;
    WatchService watchService;
    Gson gson = new Gson();

    /**
     * Create with params
     * @param host
     * @param watchKinds
     * @throws IOException
     * @throws InterruptedException
     */
    public WatchResources(Host<?> host, Kind<?>[] watchKinds) throws IOException, InterruptedException {
        this(host, watchKinds, Constants.IN_MEMORY_LIMIT_SIZE);
    }

    /**
     * Create with params
     * @param hosts
     * @param watchPath
     * @param watchKinds
     * @param accessFilters
     * @param forbiddenFilters
     * @param inMemoryFilters
     * @throws IOException
     * @throws InterruptedException
     * @throws ImageProcessingException
     */
    public WatchResources(Host<?> host, Kind<?>[] watchKinds, long inMemoryLimitSize) throws IOException, InterruptedException {
        this.host = host;
        this.watchKind = watchKinds;
        this.inMemoryLimitSize = inMemoryLimitSize;
        this.hostId = host.getHostId();
        this.watchPath = host.getStatic().normalize();
        this.accessFiltering = host.getAccessFiltering();
        this.accessFiltering.addFilter(host.getWelcomeFile().getName());
        this.forbiddenFiltering = host.getForbiddenFiltering();
        this.inMemoryFiltering = host.getInMemoryFiltering();
        this.resourceTree = new ResourceInfo(true, this.watchPath, false);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchMap = Files.walk(this.watchPath).sorted().filter(p -> p.toFile().isDirectory()).map(p -> {
            try {
                return new Object[]{ p.register(this.watchService, this.watchKind), p };
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(arr -> arr != null).collect(Collectors.toMap(k -> (WatchKey)k[0], v -> (Path)v[1]));
        // Load host resources
        long startMillis = System.currentTimeMillis();
        this.resourceTree = loadResoureTree(this.watchPath, this.resourceTree);
        //this.resourceTree = loadForkJoinResources();
        this.host.getLogger().info("[RESOURCE-LOAD] Host "+this.host.getHost()+" is complated: "+UNIT.SE.applyUnit(System.currentTimeMillis()-startMillis, 2));        
        //Have to set WatchResource to Hosts
        Context.getHosts().getHost(this.hostId).setResource(this);
        //Start watch thread
        start();
    }

    /**
     * Load resources by ForkJoin framework
     * @return
     * @throws IOException
     */
    protected ResourceInfo loadForkJoinResources() throws IOException {
        try {
            ForkJoinPool pool = new ForkJoinPool((int)ResourceMonitor.get().getAvailableProcessors());
            ResourceLoadProcessor proc = new ResourceLoadProcessor(new ResourceInfo(true, this.watchPath, false));
            pool.execute(proc);
            while(!proc.isDone()) {
                TimeUnit.SECONDS.sleep(1);
            }
            pool.shutdown();
            return proc.join();
        } catch (NotSupportedException | InterruptedException e) {
            host.getLogger().error(e.getMessage());
        }
        throw new IllegalStateException("Fail to load resources when boot up process.");
    }

    /**
     * ResourceLoadProcessor
     * 
     * @author 9ins
     */
    public class ResourceLoadProcessor extends RecursiveTask<ResourceInfo> {
        /**
         * ResourceInfo  
         */        
        private ResourceInfo res;
        /**
         * Constructe with ResourceInfo object
         * 
         * @param res
         */
        public ResourceLoadProcessor(ResourceInfo res) {
            this.res = res;
        }
        @Override
        protected ResourceInfo compute() {
            try {
                File[] files = this.res.getFile().listFiles();
                for(File file : files) {
                    if(file.isDirectory()) {
                        ResourceInfo fileRes = new ResourceInfo(true, file.toPath(), false);    
                        ResourceLoadProcessor task = new ResourceLoadProcessor(fileRes);
                        task.fork();
                        this.res.put(file.getName(), task.join());
                    } else {
                        String fileSize = (float)UNIT.MB.get(file.length())+" MB";
                        if(forbiddenFiltering.exclude(file.getName())) {
                            if(file.length() <= inMemoryLimitSize && inMemoryFiltering.include(file.getName())) {
                                this.res.put(file.getName(), new ResourceInfo(false, file.toPath(), true));
                                LoggerFactory.getLogger(hostId).debug("[HOST:"+hostId+"][LOAD] In-Memory resource loaded: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                            } else {
                                this.res.put(file.getName(), new ResourceInfo(false, file.toPath(), false));
                                LoggerFactory.getLogger(hostId).debug("[HOST:"+hostId+"][LOAD] File resource loaded: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                            }
                        } else {
                            LoggerFactory.getLogger(hostId).debug("[HOST:"+hostId+"][NOT-INCLUDED] Not included in access resources: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                        }                    
                    }
                }    
            } catch(Exception e) {
                e.printStackTrace();
            }
            return this.res;
        }            
    }    

    @Override
    public void run() {
        try {
            while(this.watchService != null) {
                final WatchKey key = this.watchService.take();
                for(WatchEvent<?> event : key.pollEvents()) {
                    final Path eventPath = this.watchMap.get(key);
                    long size = eventPath.toFile().length();
                    //LoggerFactory.getLogger(this.hostId).debug("[WATCH EVENT] KIND: "+event.kind()+"   Context: "+event.context()+"   Path: "+this.watchMap.get(key)+"   CNT: "+event.count());
                    if(event.kind() == StandardWatchEventKinds.OVERFLOW || eventPath == null) {
                        continue;
                    } 
                    if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path pathCreated = eventPath.resolve(event.context().toString());
                        LoggerFactory.getLogger(this.hostId).debug("[RESOURCE EVENT] KIND: "+event.kind()+" EVENT RESOURCE SIZE: "+UNIT.MB.get(pathCreated.toFile().length())+"  TOTAL RESOURCE SIZE: "+getResourceTotalSize(UNIT.MB));
                        ResourceInfo data = null;
                        if(pathCreated.toFile().isDirectory()) {
                            LoggerFactory.getLogger(this.hostId).debug("[RESOURCE CREATED] Directory resource created: "+pathCreated.toAbsolutePath());
                            this.watchMap.put(pathCreated.register(this.watchService, this.watchKind), pathCreated);
                            data = new ResourceInfo(true, pathCreated, false);
                        } else {
                            if(this.forbiddenFiltering.exclude(pathCreated.toFile().getName())) {                                
                                try {
                                    if(this.inMemoryFiltering.include(pathCreated.toFile().getName())) {
                                        //When In-Memory resource
                                        Files.move(pathCreated, pathCreated, StandardCopyOption.ATOMIC_MOVE);                                        
                                        if(pathCreated.toFile().length() <= this.inMemoryLimitSize) {
                                            data = new ResourceInfo(false, pathCreated, true);
                                            LoggerFactory.getLogger(this.hostId).debug("[IN-MEMORY CREATED LOADING] Loaded to memory: "+pathCreated.toAbsolutePath()+"  [SIZE: "+UNIT.MB.get(size)+"]");
                                        } else {
                                            //When In-Memory limit and reject
                                            data = new ResourceInfo(false, pathCreated, false);
                                            LoggerFactory.getLogger(this.hostId).debug("[IN-MEMORY CREATED LOADING] In-Memory file size overflow. Limit: "+UNIT.MB.get(this.inMemoryLimitSize)+"  File size: "+UNIT.MB.get(pathCreated.toFile().length()));
                                        }
                                    } else {                        
                                        // When File resource
                                        data = new ResourceInfo(false, pathCreated, false);                                
                                    }
                                } catch(FileSystemException e) {
                                    LoggerFactory.getLogger(this.hostId).warn("[FILE SYSTEM ALERT] "+e.getMessage());
                                    continue;
                                }
                            }
                        }
                        String[] paths = pathCreated.subpath(this.watchPath.getNameCount(), pathCreated.getNameCount()).toString().split(Pattern.quote(File.separator));
                        addResource(this.resourceTree, paths, data);
                    } else if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path pathModified = eventPath.resolve(event.context().toString());
                        if(pathModified.toFile().isFile() && this.forbiddenFiltering.exclude(pathModified.toFile().getName())) {
                            ResourceInfo data  = null;
                            try {
                                //When In-Memory resource
                                if(this.inMemoryFiltering.include(pathModified.toFile().getName())) {
                                    Files.move(pathModified, pathModified, StandardCopyOption.ATOMIC_MOVE);
                                    if(pathModified.toFile().length() <= this.inMemoryLimitSize) {
                                        LoggerFactory.getLogger(this.hostId).debug("[IN-MEMORY MODIFIED LOADING] Loaded to memory: "+pathModified.toAbsolutePath()+"  [SIZE: "+UNIT.MB.get(pathModified.toFile().length())+"]");
                                        data = new ResourceInfo(false, pathModified, true);
                                    } else {
                                        //When In-Memory limit and reject
                                        LoggerFactory.getLogger(this.hostId).debug("[IN-MEMORY MODIFIED LOADING] In-Memory file size overflow. Limit: "+UNIT.MB.get(this.inMemoryLimitSize)+"  File size: "+UNIT.MB.get(pathModified.toFile().length()));
                                        data = new ResourceInfo(false, pathModified, false);
                                    }
                                } else {
                                    // When File resource   
                                    LoggerFactory.getLogger(this.hostId).debug("[EXCLUDE IN-MEMORY FILTER] Exclude on In-Memory resources: "+pathModified.toAbsolutePath());
                                    data = new ResourceInfo(false, pathModified, false);
                                }
                            } catch(FileSystemException e) {
                                //e.printStackTrace();
                                //LoggerFactory.getLogger(this.hostId).warn("[FILE SYSTEM ALERT] "+e.getMessage());
                                continue;
                            }
                            String[] paths = pathModified.subpath(this.watchPath.getNameCount(), pathModified.getNameCount()).toString().split(Pattern.quote(File.separator));
                            addResource(this.resourceTree, paths, data);
                        }
                    } else if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        final Path pathDelete = eventPath.resolve(event.context().toString());
                        Map.Entry<WatchKey, Path> entry = this.watchMap.entrySet().stream().filter(e -> e.getValue().equals(pathDelete)).findAny().orElse(null);
                        if(entry != null) {
                            this.watchMap.remove(entry.getKey());
                        }
                        String[] paths = pathDelete.subpath(this.watchPath.getNameCount(), pathDelete.getNameCount()).toString().split(Pattern.quote(File.separator));
                        removeResource(this.resourceTree, paths);
                    }
                }
                key.reset();

            }
            this.watchService.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load resource tree from specified Path
     * @param path
     * @param tree
     * @return
     * @throws IOException
     * @throws ImageProcessingException
     */
    protected ResourceInfo loadResoureTree(Path path, ResourceInfo tree) throws IOException {
        File[] files = path.toFile().listFiles();
        for(File file : files) {
            if(file.isDirectory()) {
                tree.put(file.getName(), loadResoureTree(file.toPath(), new ResourceInfo(true, file.toPath(), false)));
            } else {
                if(file.isFile()) {
                    String fileSize = (float)UNIT.MB.get(file.length())+" MB";
                    if(this.forbiddenFiltering.exclude(file.getName())) {
                        if(file.length() <= this.inMemoryLimitSize && this.inMemoryFiltering.include(file.getName())) {
                            //System.out.println(file.getAbsolutePath());
                            tree.put(file.getName(), new ResourceInfo(false, file.toPath(), true));
                            LoggerFactory.getLogger(this.hostId).debug("[HOST:"+this.hostId+"][LOAD] In-Memory resource loaded: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                        } else {
                            tree.put(file.getName(), new ResourceInfo(false, file.toPath(), false));
                            LoggerFactory.getLogger(this.hostId).debug("[HOST:"+this.hostId+"][LOAD] File resource loaded: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                        }
                    } else {
                        LoggerFactory.getLogger(this.hostId).debug("[HOST:"+this.hostId+"][NOT-INCLUDED] Not included in access resources: "+file.getName()+"  Size: "+fileSize+"  Bytes: "+file.length());
                    }
                }
            }
        }
        return tree;
    }

    @Override
    public void addResource(Path resourcePath) throws Exception {        
        if(this.accessFiltering.include(resourcePath.toFile().getName()) && this.forbiddenFiltering.exclude(resourcePath.toFile().getName())) {            
            Path path = resourcePath.subpath(this.watchPath.getNameCount(), resourcePath.getNameCount());
            if(this.inMemoryFiltering.include(resourcePath.toFile().getName())) {
                addResource(this.resourceTree, path.toString().split(Pattern.quote(File.separator)), new ResourceInfo(false, resourcePath, true));
                LoggerFactory.getLogger(this.hostId).debug("[ADD IN-MEMORY RESOURCE] Resource Path: "+resourcePath.toString());
            } else {
                addResource(this.resourceTree, path.toString().split(Pattern.quote(File.separator)), new ResourceInfo(false, resourcePath, false));
                LoggerFactory.getLogger(this.hostId).debug("[ADD FILE RESOURCE] Resource Path: "+resourcePath.toString());
            }
        } else {
            LoggerFactory.getLogger(this.hostId).debug("[RESOURCE REGISTRATION FAIL] Specified resource registration fail: "+resourcePath);
        }
    }

    @Override
    public void addResource(Path resourcePath, byte[] resourceRawData, boolean inMemoryFlag) throws Exception {
        if(this.accessFiltering.include(resourcePath.toFile().getName()) && this.forbiddenFiltering.exclude(resourcePath.toFile().getName())) {
            Path path = resourcePath.subpath(this.watchPath.getNameCount(), resourcePath.getNameCount());
            if(this.inMemoryFiltering.include(resourcePath.toFile().getName())) {
                addResource(this.resourceTree, path.toString().split(Pattern.quote(File.separator)), new ResourceInfo(false, resourcePath, resourceRawData, inMemoryFlag));
                LoggerFactory.getLogger(this.hostId).debug("[ADD IN-MEMORY RESOURCE] Resource Path: "+resourcePath.toString());
            } else {                
                addResource(this.resourceTree, path.toString().split(Pattern.quote(File.separator)), new ResourceInfo(false, resourcePath, resourceRawData, inMemoryFlag));
                LoggerFactory.getLogger(this.hostId).debug("[ADD FILE RESOURCE] Resource Path: "+resourcePath.toString());
            }
        } else {
            LoggerFactory.getLogger(this.hostId).debug("[RESOURCE REGISTRATION FAIL] Specified raw resource registration fail: "+resourcePath);
        }
    }

    /**
     * Add resource to resource tree
     * @param tree
     * @param res
     * @throws IOException
     */
    protected void addResource(ResourceInfo resourceTree, String[] res, ResourceInfo data) throws IOException {        
        if(res.length == 1) {            
            resourceTree.put(res[0], data);
        } else {
            ResourceInfo val = (ResourceInfo)resourceTree.get(res[0]);
            if(val == null) {
                resourceTree.put(res[0], new ResourceInfo(true, this.watchPath.resolve(res[0]), false));
            }
            addResource((ResourceInfo)resourceTree.get(res[0]), Arrays.copyOfRange(res, 1, res.length), data);
        }
    }

    @Override
    public void removeResource(Path resourcePath) throws Exception {
        String subPath = resourcePath.subpath(this.watchPath.getNameCount(), resourcePath.getNameCount()).toString();
        removeResource(this.resourceTree, subPath.split(Pattern.quote(File.separator)));        
    }

    @Override
    public ResourceInfo getResourceInfo(Path resourcePath) throws Exception {
        resourcePath = resourcePath.normalize().toAbsolutePath();
        if(resourcePath.getNameCount() == watchPath.getNameCount()) {
            return this.resourceTree;
        } else if(resourcePath.getNameCount() < watchPath.getNameCount()) {
            throw new WASException(MSG_TYPE.HTTP, 403, "Requested path is not allowed.");
        }        
        return getResource(this.resourceTree, resourcePath.subpath(this.watchPath.getNameCount(), resourcePath.getNameCount()).toString().split(Pattern.quote(File.separator)));
    }

    /**
     * Filtering and get resource List by mime-type
     * @param mimeType
     * @return
     * @throws IOException
     */
    @Override
    public ResourceInfo filter(MIME_TYPE mimeType) throws IOException {
        return filterResourceTree(this.resourceTree, mimeType);
    }

    @Override
    public Path resolveRealPath(String contextPath) {
        Path staticPath = this.host.getStatic();
        contextPath = contextPath.charAt(0) == '/' ? contextPath.substring(0) : contextPath;
        contextPath = contextPath.charAt(contextPath.length() - 1) == '/' ? contextPath.substring(0, contextPath.lastIndexOf('/')) : contextPath;
        return staticPath.resolve(contextPath);
    }

    @Override
    public ResourceInfo getContextResourceInfo(String contextPath) throws Exception { 
        return getResourceInfo(this.watchPath.resolve(contextPath));
    }

    @Override
    public String getContextPath(Path resourcePath) {
        resourcePath = resourcePath.normalize().toAbsolutePath();
        return resourcePath.subpath(this.watchPath.getNameCount(), resourcePath.getNameCount()).toString().replace("\\", "/");
    }

    @Override
    public String getStaticPage(String contextPath, Map<String, Object> params) throws Exception {                
        ResourceInfo resourceInfo = getContextResourceInfo(contextPath);
        if(resourceInfo == null) {            
            throw new WASException(MSG_TYPE.HTTP, RES_CODE.RES404.code(), " Static page not found in Resource manager: "+contextPath);
        }
        String page = new String((resourceInfo).getBytes(), Context.getHosts().getHost(this.hostId).<String> charset());
        if(params != null) {
            page = resolvePage(page, params);
        }
        return page;
    }

    @Override
    public String getWelcomePage(Map<String, Object> params) throws Exception {
        return getStaticPage(Context.getHosts().getHost(this.hostId).getWelcomeFile().getName(), params);
    }

    @Override
    public String getTemplatePage(String templatePath, Map<String, Object> params) throws Exception {
        String page = getStaticPage(templatePath, params);
        if(params != null) {
            for(Entry<String, Object> e : params.entrySet()) {
                page = page.replace(e.getKey(), e.getValue()+"");
            }    
        }
        return page;
    }

    @Override
    public String getResponsePage(Map<String, Object> params) throws Exception {
        return getTemplatePage("templates/response.html", params);
    }

    @Override
    public String getErrorPage(Map<String, Object> params) throws Exception {
        return getTemplatePage("templates/error.html", params);
    }

    @Override
    public String getResourcePage(Map<String, Object> params) throws Exception {        
        return getTemplatePage("templates/resource.html", params);
    }

    @Override
    public String resolvePage(String html, Map<String, Object> params) {
        for(Map.Entry<String, Object> entry : params.entrySet()) {
            String regex = Constants.TAG_REGEX_PREFIX+entry.getKey()+Constants.TAG_REGEX_SUFFIX;
            Pattern ptrn = Pattern.compile(regex);
            Matcher matcher = ptrn.matcher(html);
            while(matcher.find()) {
                html = html.replace(matcher.group(), entry.getValue()+"");
            }
        }
        return html;
    }

    @Override
    public boolean exists(Path resourcePath) {
        try {
            ResourceInfo resourceInfo = getResourceInfo(resourcePath.normalize());
            if(resourceInfo != null) {
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    @Override
    public boolean isInMemory(Path resourcePath) {
        ResourceInfo resInfo = null;
        try {
            resInfo = getResourceInfo(resourcePath);
        } catch (Exception e) {
            return false;
        }
        return resInfo == null ? false : resInfo.isInMemory();
    }

    /**
     * Filtering resource tree and get filtered resource List
     * @param resourceTree
     * @param mimeType
     * @return
     * @throws IOException
     */
    private ResourceInfo filterResourceTree(ResourceInfo resourceTree, MIME_TYPE mimeType) throws IOException {
        ResourceInfo infos = new ResourceInfo(true, resourceTree.getPath(), false);
        for(Object obj : resourceTree.values()) {
            ResourceInfo info = (ResourceInfo) obj;
            if(!info.isNode()) {
                if(info.getMimeType() == mimeType) {
                    infos.put(info.getContextPath(), info);
                }
            } else {
                infos.putAll(filterResourceTree(info, mimeType));
            }
        }
        return infos;
    }

    /**
     * Get resource data
     * @param tree
     * @param res
     * @return
     */
    private final ResourceInfo getResource(ResourceInfo tree, String[] res) {
        if(res.length == 1) {
            return tree.get(res[0]);
        } else {
            return getResource(tree.get(res[0]), Arrays.copyOfRange(res, 1, res.length));
        }
    }

    /**
     * Remove resource
     * @param tree
     * @param res
     */
    private final void removeResource(ResourceInfo resourceTree, String[] res) {
        if(res.length == 1) {
            resourceTree.remove(res[0]);
        } else {
            removeResource((ResourceInfo)resourceTree.get(res[0]), Arrays.copyOfRange(res, 1, res.length));
        }
    }

    /**
     * Get resource total size
     * @param sizeUnit
     * @return
     */
    public double getResourceTotalSize(UNIT sizeUnit) {
        return sizeUnit.get(getResourceTotalSize(this.resourceTree));
    }

    /**
     * Get total resource size
     * @param res
     * @return
     */
    public long getResourceTotalSize(ResourceInfo tree) {
        long size = 0;
        for(Object obj : tree.values()) {
            ResourceInfo info = (ResourceInfo) obj;
            if(!info.isNode()) {
                size += info.getResourceSize();
            } else {
                size += getResourceTotalSize(info);
            } 
        }
        return size;
    }    

    /**
     * ResourceInfo
     * 
     * @author 9ins
     */
    public class ResourceInfo extends HashMap<String, ResourceInfo> {
        /**
         * Whether node resource
         */
        boolean isNode;

        /**
         * Mapping context path
         */
        String contextPath;

        /**
         * Resource path
         */
        Path resourcePath;

        /**
         * In-Memory flag 
         */
        boolean inMemoryFlag;

        /**
         * Resource data List
         */
        List<byte[]> resourceData;

        /**
         * Resource mime-type  
         */        
        MIME_TYPE mimeType;

        /**
         * Resource last modified time
         */
        long lastModified;

        /**
         * Resource File
         */
        File resourceFile;

        /**
         * Resource size
         */
        long resourceSize;

        /**
         * Constructs with resource path & in-memory flag
         * @param isNode
         * @param resourcePath
         * @param inMemoryFlag
         * @throws IOException
         * @throws ImageProcessingException
         */
        public ResourceInfo(boolean isNode, Path resourcePath, boolean inMemoryFlag) throws IOException {
            this.isNode = isNode;
            this.resourcePath = resourcePath;
            this.resourceFile = resourcePath.toFile();
            this.resourceSize = resourcePath.toFile().length();
            this.inMemoryFlag = inMemoryFlag;
            this.mimeType = MIME_TYPE.mimeType(Files.probeContentType(this.resourcePath));
            if(this.inMemoryFlag) {
                this.resourceData = new ArrayList<>();//Collections.synchronizedList(new ArrayList<>());
                int splitSize = Context.getHosts().getHost(hostId).getInMemorySplitUnit();
                try(FileInputStream fis = new FileInputStream(resourcePath.toFile())) {
                    long fileSize = this.resourcePath.toFile().length();
                    int cnt = (int) (fileSize / splitSize);
                    int rest = (int) (fileSize % splitSize);
                    for(int i=0; i<cnt; i++) {
                        byte[] part = new byte[splitSize];
                        fis.read(part);
                        this.resourceData.add(part);
                    }
                    byte[] part = new byte[rest];
                    fis.read(part);
                    this.resourceData.add(part);  
                };
            }
            this.lastModified = resourcePath.toFile().lastModified();
        }  

        /**
         * Constructs with node flag, path, raw data
         * @param isNode
         * @param resourcePath
         * @param resourceRawData
         * @throws IOException
         */
        public ResourceInfo(boolean isNode, Path resourcePath, byte[] resourceRawData, boolean inMemoryFlag) throws Exception {
            this.isNode = isNode;
            this.resourcePath = resourcePath;
            this.resourceFile = resourcePath.toFile();
            this.resourceSize = resourcePath.toFile().length();
            this.inMemoryFlag = inMemoryFlag;
            this.mimeType = MIME_TYPE.mimeType(Files.probeContentType(this.resourcePath));
            if(this.inMemoryFlag) {
                this.resourceData = new ArrayList<>();//Collections.synchronizedList(new ArrayList<>());
                int splitSize = Context.getHosts().getHost(hostId).getInMemorySplitUnit();
                long fileSize = resourceRawData.length;
                int cnt = (int)(fileSize / splitSize);
                int rest = (int)(fileSize % splitSize);
                byte[] part = null;
                int i = 0;
                for(i=0; i<cnt; i++) {
                    part = new byte[splitSize];
                    System.arraycopy(resourceRawData, i * part.length, part, 0, part.length);
                    this.resourceData.add(part);
                }
                part = new byte[rest];
                System.arraycopy(resourceRawData, cnt * part.length, part, 0, part.length);
                this.resourceData.add(part);      
            } else {
                try(FileOutputStream out = new FileOutputStream(resourcePath.toFile())) {
                    out.write(resourceRawData);
                }
            }
            this.lastModified = System.currentTimeMillis();
        }     

        /**
         * Get all bytes on resource
         * @return
         * @throws IOException
         */
        public byte[] getBytes() throws IOException {
            if(this.inMemoryFlag) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                for(byte[] part : this.resourceData) {
                    out.write(part);
                }
                return out.toByteArray();
            } else {
                return Files.readAllBytes(this.resourcePath);
            }
        }

        /**
         * Get partitial bytes of File resource by position & length
         * @param position
         * @param length
         * @return
         * @throws IOException
         */
        public byte[] getFileBytes(long position, int length) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            try(FileInputStream fis = new FileInputStream(this.resourcePath.toFile())) {
                FileChannel channel = fis.getChannel();
                channel.position(position);
                channel.read(buffer);
                channel.close();
                return buffer.array();    
            }
        }

        /**
         * Get bytes of resource(file or memory) with position & length
         * @param position
         * @param length
         * @return
         * @throws IOException
         */
        public synchronized byte[] getBytes1(long position, int length) throws Exception {
            byte[] bytes = new byte[length];
            if(this.inMemoryFlag) {                
                if(position < 0) {
                    throw new IllegalArgumentException("Offset position must be on positive side of the digit.");
                }
                int start = (int) position;
                int end  = (int) (position + length);
                int posStart = 0, posEnd = 0;
                int pos = 0;

                for(int i=0; i<this.resourceData.size(); i++) {
                    byte[] data = this.resourceData.get(i);
                    posEnd += data.length;
                    posStart = posEnd - data.length;
                    //System.out.println("start: "+start+" end: "+end+"  range: "+posStart+" - "+posEnd+"  i: "+i);
                    if(start >= posStart && start < posEnd) {
                        if(end <= posEnd) {
                            System.arraycopy(data, start - posStart, bytes, 0, length);
                            break;
                        } else {
                            pos = posEnd - start;
                            System.arraycopy(data, start - posStart, bytes, 0, pos);
                            continue;
                        }
                    } else if(start < posStart) {
                        if(end < posEnd) {
                            int len = posStart + (posEnd - end);
                            System.out.println("begin "+start+"  end: "+end+" data: "+data.length+"  posStart: "+posStart+"  posEnd: "+posEnd+"  len: "+len);
                            if(pos + len < end) {
                                System.arraycopy(data, 0, bytes, pos, len);
                            } else {
                                System.arraycopy(data, 0, bytes, pos, posEnd - len);
                            }
                            break;    
                        } else {
                            System.arraycopy(data, 0, bytes, pos, data.length);
                            //System.out.println("idx: "+idx);
                            pos += data.length;
                        }
                    }
                } 
            } else {
                bytes = getFileBytes(position, length);
            }
            return bytes;
        }

        /**
         * Get bytes of resource(file or memory) with position & length
         * @param position
         * @param length
         * @return
         * @throws IOException
         */
        public synchronized byte[] getBytes2(long position, int length) throws IOException {
            byte[] data = new byte[length];
            if(this.inMemoryFlag) {
                if(position < 0) {
                    throw new IllegalArgumentException("Offset position must be on positive side of the digit.");
                }
                long totalLen = this.resourcePath.toFile().length();
                int partCnt = this.resourceData.size();
                int partSize = (int)(totalLen / partCnt);
                int partIdx1 = (int)(position / partSize);
                
                partIdx1 = position % partSize > 0 ? partIdx1++ : partIdx1;
                int partIdx2 = (int)((position + partSize) / partSize);    
                int buffSize = (int)(totalLen / length);
                int buffIdx1 = (int)(position / length);
                //System.out.println("partIdx1: "+partIdx1+"  partIdx2: "+partIdx2+"  buffSize: "+buffSize+"  buffIdx1: "+buffIdx1);
                if(partIdx1 == partIdx2) {
                    byte[] part = this.resourceData.get(partIdx1);
                    int idx = (int)position;
                    System.arraycopy(part, idx, data, 0, length);
                } else {
                    if(partIdx2 - partIdx1 == 1) {
                        byte[] part = this.resourceData.get(partIdx1);
                        int idx = (int)(position % buffSize);
                        System.arraycopy(part, idx, data, 0, length - idx);
                        part = this.resourceData.get(partIdx2 - 1);
                        System.arraycopy(part, 0, data, length - idx, length - (length - idx));
                    } else {
                        byte[] part = this.resourceData.get(partIdx1 - 1);
                        int idx = (int)(position % buffSize);
                        System.arraycopy(part, idx, data, 0, length - idx);
                        for(int i=partIdx1+1; i<partIdx2; i++) {
                            part = this.resourceData.get(i);
                            System.arraycopy(part, 0, data, 0, length);
                        }
                        part = this.resourceData.get(partIdx2 - 1);
                        idx = (int)((position + length) % buffSize);
                        System.arraycopy(part, 0, data, 0, length - idx);
                    }
                }
            } else {
                data = getFileBytes(position, length);
            }
            return data;     
        }

        /**
         * Get whether node resource 
         * @return
         */
        public boolean isNode() {
            return this.isNode;
        }

        /**
         * Get resource Path object
         * @return
         */
        public Path getPath() {
            return this.resourcePath;
        }

        /**
         * Get context path of resource
         * @return
         */
        public String getContextPath() {
            return this.contextPath;
        }

        /**
         * Get resource name
         * @return
         */
        public String getResourceName() {
            return this.resourcePath.getFileName().toString();
        }

        /**
         * Get resource File
         * @return
         */
        public File getFile() {
            return this.resourceFile;
        }

        /**
         * Get resurce size
         * @return
         */
        public long getResourceSize() {
            return this.resourceSize;
        }

        /**
         * Get mime-type of resource
         * @return
         */
        public MIME_TYPE getMimeType() {
            return this.mimeType;
        }

        /**
         * Get last modified time of specfied unit
         * @param timeUnit
         * @return
         */
        public long getTime(TimeUnit timeUnit) {
            return TimeUnit.MILLISECONDS.convert(this.lastModified, timeUnit);
        }

        /**
         * Whether In-Memory resource
         * @return
         */
        public boolean isInMemory() {
            return this.inMemoryFlag;
        }
    }
}

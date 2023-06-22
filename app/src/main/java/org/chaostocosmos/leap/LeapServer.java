package org.chaostocosmos.leap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;
import javax.transaction.NotSupportedException;

import org.chaostocosmos.leap.common.Constants;
import org.chaostocosmos.leap.common.Filtering;
import org.chaostocosmos.leap.common.LoggerFactory;
import org.chaostocosmos.leap.common.RedirectHostSelection;
import org.chaostocosmos.leap.context.Context;
import org.chaostocosmos.leap.context.Host;
import org.chaostocosmos.leap.enums.HTTP;
import org.chaostocosmos.leap.enums.PROTOCOL;
import org.chaostocosmos.leap.enums.STATUS;
import org.chaostocosmos.leap.http.HttpsServerSocketFactory;
import org.chaostocosmos.leap.http.ServiceManager;
import org.chaostocosmos.leap.resource.ClassUtils;
import org.chaostocosmos.leap.resource.Html;
import org.chaostocosmos.leap.resource.LeapURLClassLoader;
import org.chaostocosmos.leap.resource.ResourceManager;
import org.chaostocosmos.leap.resource.ResourcesModel;
import org.chaostocosmos.leap.session.SessionManager;
import org.chaostocosmos.leap.security.SecurityManager;

import ch.qos.logback.classic.Logger;

/**
 * HttpServer object
 * 
 * @author 9ins
 * @since 2021.09.15
 */
public class LeapServer extends Thread {
    /**
     * logger
     */
    Logger logger = LoggerFactory.getLogger(Context.get().hosts().getDefaultHost().getHostId());

    /**
     * Whether default host
     */
    boolean isDefaultHost;

    /**
     * Protocol
     */
    PROTOCOL protocol;

    /**
     * InetSocketAddress
     */
    InetSocketAddress inetSocketAddress;

    /**
     * backlog
     */
    int backlog;

    /**
     * document root
     */
    Path docroot;

    /**
     * leap home path
     */
    Path homePath;

    /**
     * Hosts
     */
    Host<?> host;

    /**
     * Server socket
     */
    ServerSocket server;

    /**
     * thread pool
     */
    ThreadPoolExecutor threadpool;

    /**
     * Redirect host object
     */
    RedirectHostSelection redirectHostSelection;

    /**
     * IP filtering object
     */
    Filtering ipAllowedFilters, ipForbiddenFilters; 

    /**
     * Server started flag
     */
    boolean started;

    /**
     * SessionManager object
     */
    SessionManager sessionManager;

    /**
     * servlet loading & managing
     */
    ServiceManager serviceManager;

    /**
     * User manager object
     */
    SecurityManager userManager;

    /**
     * ResourcesModel object
     */
    ResourcesModel resourcesModel;

    /**
     * Default constructor 
     * @throws NotSupportedException
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     */
    public LeapServer() throws IOException, InterruptedException, URISyntaxException, NotSupportedException {
        this(Constants.DEFAULT_HOME_PATH);
    }

    /**
     * Construct with home path
     * 
     * @param homePath
     * @throws NotSupportedException
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     */
    public LeapServer(Path homePath) throws IOException, InterruptedException, URISyntaxException, NotSupportedException {
        this(homePath, 
             Context.get().hosts().getHost(Context.get().hosts().getDefaultHost().getHostId()), 
                                    new ThreadPoolExecutor(Context.get().server().getThreadPoolCoreSize(), 
                                                           Context.get().server().getThreadPoolMaxSize(), 
                                                           Context.get().server().getThreadPoolKeepAlive(), 
                                                           TimeUnit.SECONDS, 
                                                           new LinkedBlockingQueue<Runnable>())
        );
    }

    /**
     * Construct with home path, host object, thread pool
     * 
     * @param homePath
     * @param host
     * @param threadpool
     * @throws NotSupportedException
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     */
    public LeapServer(Path homePath, Host<?> host, ThreadPoolExecutor threadpool) throws IOException, InterruptedException, URISyntaxException, NotSupportedException {
        this(homePath, host, threadpool, ClassUtils.getClassLoader());
    }

    /**
     * Construct with Context object
     * 
     * @param homePath
     * @param host
     * @param threadpool 
     * @param classLoader
     * @throws NotSupportedException
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     */
    public LeapServer(Path homePath, Host<?> host, ThreadPoolExecutor threadpool, LeapURLClassLoader classLoader) throws IOException, InterruptedException, URISyntaxException, NotSupportedException {
        this(true, Context.get().getHomePath(), host.getDocroot(), PROTOCOL.valueOf(host.getProtocol()), new InetSocketAddress(InetAddress.getByName(host.getHost()), host.getPort()), Context.get().server().getBackLog(), threadpool, host, classLoader);
    }

    /**
     * Constructor with configuration file Path
     * 
     * @param isDefaultHost
     * @param homePath
     * @param docroot
     * @param protocol
     * @param inetSocketAddress
     * @param backlog
     * @param threadpool
     * @param host
     * @param classLoader
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IOException
     * @throws NotSupportedException
     */
    public LeapServer(boolean isDefaultHost, Path homePath, Path docroot, PROTOCOL protocol, InetSocketAddress inetSocketAddress, int backlog, ThreadPoolExecutor threadpool, Host<?> host, LeapURLClassLoader classLoader) throws IOException, InterruptedException, URISyntaxException, NotSupportedException {
        this.host = host;
        this.host.setHostStatus(STATUS.SETUP);
        this.isDefaultHost = true;
        this.homePath = homePath;
        this.protocol = protocol;
        this.backlog = backlog;
        this.docroot = docroot;
        this.threadpool = threadpool;
        this.inetSocketAddress = inetSocketAddress;
        this.ipAllowedFilters = host.getIpAllowedFiltering();
        this.ipForbiddenFilters = host.getIpForbiddenFiltering();
        this.redirectHostSelection = new RedirectHostSelection(Context.get().server().getLoadBalanceRedirects());
        this.sessionManager = new SessionManager(host);
        this.userManager = new SecurityManager(host.getHostId());
        this.resourcesModel = ResourceManager.get(host.getHostId());
        this.serviceManager = new ServiceManager(host, this.userManager, this.sessionManager, this.resourcesModel, classLoader);
    }

    /**
     * Get service host ID
     * @return
     */
    public String getHostId() {
        return this.host.getHostId();
    }

    /**
     * Get service port
     * @return
     */
    public int getPort() {
        return this.inetSocketAddress.getPort();
    }

    /**
     * Get servlet loader object
     * @return
     */
    protected ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    /**
     * Get session manager
     * @return
     */
    protected SessionManager getSessionManager() {
        return this.sessionManager;
    }

    /**
     * Get user manager object
     * @return
     */
    protected SecurityManager getSecurityManager() {
        return this.userManager;
    }

    /**
     * Get root directory
     * @return
     */
    public Path getDocroot() {
        return this.docroot;
    }

    @Override
    public void run() {
        try {
            this.host.setHostStatus(STATUS.STARTING);
            if(!this.protocol.isSecured()) {
                this.server = new ServerSocket();
                this.server.bind(this.inetSocketAddress, this.backlog);
                this.logger.info("[HTTP SERVER START] Address: " + this.inetSocketAddress.toString());
            } else {
                File keyStore = new File(Context.get().server().<String> getKeyStore());
                String passphrase = Context.get().server().getPassphrase();
                String sslProtocol = Context.get().server().getEncryptionMethod();
                this.server = HttpsServerSocketFactory.getSSLServerSocket(keyStore, passphrase, sslProtocol, this.inetSocketAddress, this.backlog);
                this.logger.info("[HTTPS SERVER START] Address: "+this.inetSocketAddress.toString()+"  Protocol: "+sslProtocol+"  KeyStore: "+keyStore.getName()+"  Supported Protocol: "+Arrays.toString(((SSLServerSocket)server).getSupportedProtocols())+"  KeyStore: "+keyStore.getName());
            }
            this.host.setHostStatus(STATUS.STARTED);
            while (this.host.getHostStatus() == STATUS.STARTED || this.host.getHostStatus() == STATUS.RUNNING) { 
                Socket connection = this.server.accept();
                connection.setSoTimeout(Context.get().server().getConnectionTimeout());
                //connection.setSoLinger(false, 1);
                int queueSize = this.threadpool.getQueue().size();
                String ipAddress = connection.getLocalAddress().toString();
                if(this.ipAllowedFilters.include(ipAddress) || this.ipForbiddenFilters.exclude(ipAddress)) {
                    if(queueSize < Context.get().server().<Integer> getThreadQueueSize()) {
                        this.logger.info("[CLIENT DETECTED] Client request accepted. Submitting thread. "+ipAddress+" - "+connection.getPort()+"  Thread queue size - "+queueSize);
                        this.threadpool.submit(new LeapHandler(this, this.docroot, connection, this.host));
                    } else {
                        String redirectHost = redirectHostSelection.getSelectedHost();
                        String redirectPage = Html.makeRedirect(this.protocol.protocol(), 0, redirectHost);
                        this.logger.info("[CLIENT DETECTED] Thread pool queue size limit reached: "+queueSize+".  Send redirect to Load-Balance URL: "+redirectHost+"\n"+redirectPage);
                        try(OutputStream out = connection.getOutputStream()) {
                            out.write(redirectPage.getBytes());
                        }
                        connection.close();
                    }
                } else {
                    this.logger.info("[CLIENT CANCELED] Rquested IP address is not in allowed filters or exist in forbidden filters: "+ipAddress);
                    Map<String, Object> params = new HashMap<>();
                    params.put("@code", HTTP.RES500);
                    params.put("@type", "Not in allowed IP or forbidden IP.");
                    params.put("@message", "Rquested IP address is not in allowed filters or exist in forbidden filters: "+ipAddress);
                    String resPage = this.host.getResource().getResourcePage(params);
                    try(OutputStream out = connection.getOutputStream()) {
                        out.write(resPage.getBytes());
                    }
                    connection.close();
                }
            } 
            //Close when breaking host server loop
            close();
        } catch(Exception e) {
            this.logger.error(e.getMessage(), e);
        }
    }
    /**
     * Stop server 
     * @throws InterruptedException
     * @throws IOException
     */
    public void close() throws InterruptedException, IOException {
        this.server.close();
        this.host.setHostStatus(STATUS.TERMINATED);
        this.host.getLogger().info("[SERVER TERMINATED] "+this.host.getHost()+" Server is terminated...");
    }

    /**
     * Whether server started
     * @return
     */
    public boolean isClosed() {
        return this.server == null ? true : this.server.isClosed();
    }
}
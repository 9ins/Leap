package org.chaostocosmos.leap.http.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.chaostocosmos.leap.http.Request;
import org.chaostocosmos.leap.http.Response;
import org.chaostocosmos.leap.http.annotation.MethodMappper;
import org.chaostocosmos.leap.http.annotation.ServiceMapper;
import org.chaostocosmos.leap.http.commons.UNIT;
import org.chaostocosmos.leap.http.enums.MIME_TYPE;
import org.chaostocosmos.leap.http.enums.REQUEST_TYPE;
import org.chaostocosmos.leap.http.resources.ResourceMonitor;
import org.chaostocosmos.leap.http.resources.SystemMonitor;

@ServiceMapper(path = "")
public class ResourceUsageService extends AbstractService {

    Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @MethodMappper(mappingMethod = REQUEST_TYPE.GET, path = "/resources")
    public void getResources(Request request, Response response) {
        String type = request.getContextParam().get("type");
        System.out.println(request.getContextParam().get("unit"));
        UNIT unit = UNIT.valueOf(request.getContextParam().get("unit"));
        Map<Object, Object> resMap = new LinkedHashMap<>();
        try {
            switch(type) {
                case "CPU":
                resMap = getCpuUsage(unit);
                break;
                case "MEMORY":
                resMap = getMemoryUsage(unit);
                break;
                case "THREAD":
                resMap = getThreadPoolUsage(unit);
                break;
                default:
                throw new RuntimeException("");
            }
        } catch (Exception e) {
            super.logger.error(e.getMessage(), e);
        }
        response.setResponseCode(200);
        response.addHeader("Content-Type", MIME_TYPE.APPLICATION_JSON.mimeType());
        response.setBody(this.gson.toJson(resMap));
    }

    @MethodMappper(mappingMethod = REQUEST_TYPE.GET, path = "/session")
    public void getSession(Request request, Response response) {
        String typeParam = request.getParameter("type");
        Object json = null;
        System.out.println(typeParam+" --------------------------");
        Map<String, Map<Object, Object>> sessions = new HashMap<>();
        String[] names = new String[] {"oracle", "kafka", "mysql"};
        for(String name : names) {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("allowedHosts", new ArrayList<>());
            map.put("forbiddenHosts", new ArrayList<>());
            map.put("keepAlive", true);
            map.put("tcpNoDelay", true);
            map.put("bindAddress", "localhost");
            map.put("remoteHosts", new ArrayList<>());
            map.put("sessionMode", "LOAD_BALANCE");
            map.put("loadBalanceRatio", "10:10:80");
            map.put("retry", 20);
            map.put("retryInterval", 30);
            map.put("bufferSize", 60);
            map.put("connectionTimeout", 60);
            map.put("soTimeout", 60);
            sessions.put(name, map);
            Map<Object, Object> map1 = new LinkedHashMap<>();
            map1.put("NAME", name);
            map1.put("SESSION_MODE", "LOAD_BALANCE");
            map1.put("SESSION_RECEIVE_FAIL_COUNT", 30);
            map1.put("SESSION_RECEIVE_SIZE", 20);
            map1.put("SESSION_RECEIVE_SIZE_TOTAL", 30);
            map1.put("SESSION_RECEIVE_SUCCESS_COUNT", 60);
            map1.put("SESSION_SEND_FAIL_COUNT", 90);
            map1.put("SESSION_SEND_SIZE", 30);
            map1.put("SESSION_SEND_SIZE_TOTAL", 20);
            map1.put("SESSION_SEND_SUCCESS_COUNT", 30);
            map1.put("SESSION_TOTAL_FAIL", 60);
            map1.put("SESSION_TOTAL_SUCCESS", 60);
            map.put("statisticsMap", map1);
        }    
        json = this.gson.toJson(sessions, sessions.getClass());
        response.setResponseCode(200);
        response.addHeader("Content-Type", MIME_TYPE.APPLICATION_JSON.mimeType());
        response.setBody(json);
    }

    @Override
    public Throwable errorHandling(Response response, Throwable throwable) throws Throwable {
        throwable.printStackTrace();
        return null;
    }

    public Map<Object, Object> getCpuUsage(UNIT unit) throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("type", "CPU");
        map.put("unit", unit);
        map.put("cpuLoad", ResourceMonitor.get().getProcessCpuLoad());
        map.put("cpuTime", ResourceMonitor.get().getProcessCpuTime());
        map.put("systemCpuLoad", ResourceMonitor.get().getSystemCpuLoad());    
        return map;
    }

    public Map<Object, Object> getMemoryUsage(UNIT unit) throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        double totalPhysicalMemory = ResourceMonitor.get().getTotalPhysicalMemory();
        double freePhysicalMemory = ResourceMonitor.get().getFreePhysicalMemory();
        double usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;
        double maxHeapMemory = ResourceMonitor.get().getProcessHeapMax(UNIT.MB);
        double usedHeapMemory = SystemMonitor.get().getProcessHeapUsed(UNIT.MB);
        double freeHeapMemory = maxHeapMemory - usedHeapMemory;
        double usedMemory = SystemMonitor.get().getProcessMemoryUsed(unit);
        map.put("systemTotal", totalPhysicalMemory);
        map.put("systemFree", freePhysicalMemory);
        map.put("systemUsed", usedPhysicalMemory);
        map.put("heapMax", maxHeapMemory);
        map.put("heapUsed", usedHeapMemory);
        map.put("heapFree", freeHeapMemory);
        map.put("memoryUsed", usedMemory);         
        return map;       
    }

    public Map<Object, Object> getThreadPoolUsage(UNIT unit) throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        int activeCount = SystemMonitor.get().getThreadPoolActiveCount();
        int corePoolSize = SystemMonitor.get().getThreadPoolCoreSize();
        int largestPoolSize = SystemMonitor.get().getThreadPoolLargestSize();
        int maxinumPoolSize = SystemMonitor.get().getThreadPoolMaxSize();
        int threadPoolLimitSize = 30;
        long completedTaskCount = SystemMonitor.get().getThreadPoolCompletedTask();
        long taskCount = 10;
        int queueSize = SystemMonitor.get().getThreadPoolQueuedTask();
        map.put("activeCount", activeCount);
        map.put("corePoolSize", corePoolSize);
        map.put("largestPoolSize", largestPoolSize);
        map.put("maxinumPoolSize", maxinumPoolSize);
        map.put("limitPoolSize", threadPoolLimitSize);
        map.put("completedTaskCount", completedTaskCount);
        map.put("taskCount", taskCount);
        map.put("queueSize", queueSize);
        return map;
    }

    public void setCorePoolSize(int size) throws Exception {
    }

    public void setMaximumPoolSize(int size) throws Exception {
    }
}

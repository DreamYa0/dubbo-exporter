package com.g7.framework.exporter.client.exporter;


import com.alibaba.dubbo.rpc.RpcContext;
import com.g7.framework.common.dto.BaseResult;
import com.g7.framework.exporter.client.conf.ProviderClientConfig;
import com.g7.framework.exporter.client.context.HttpProviderContext;
import com.g7.framework.exporter.client.monitor.HttpMonitor;
import com.g7.framework.exporter.client.util.HttpHeaderUtils;
import com.g7.framework.trace.SpanContext;
import com.g7.framework.trace.TraceContext;
import com.g7.framwork.common.util.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author dreamyao
 * @title dubbo接口暴露http调用方式
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
@RequestMapping({"api"})
public class ApiExporter implements ApplicationContextAware {

    private static final String TRACE_ID_EXT = "X-B3-Traceid";
    private static final String SPAN_ID_EXT = "X-B3-Spanid";
    private final ConcurrentMap<String, Class<?>> cacheMap = new ConcurrentHashMap<>(16);
    protected ApplicationContext applicationContext;
    private static final Logger logger = LoggerFactory.getLogger(ApiExporter.class);

    @Autowired
    private ProviderClientConfig providerClientConfig;

    @ResponseBody
    @RequestMapping({"/{service}/{method}"})
    public Object invoke(@ModelAttribute PostRequest postRequest, @PathVariable("service") String service,
                         @PathVariable("method") String method, HttpServletRequest request,
                         HttpServletResponse response) {

        //打印入参
        if (logger.isDebugEnabled()) {
            logger.debug("ip:{}-request:{}", getIpAddr(request), JsonUtils.toJson(postRequest));
        }
        //设置上下文
        HttpProviderContext.setContext(postRequest.getAppid(), getIpAddr(request));

        //设置跟踪上下文
        String traceId = HttpHeaderUtils.getHeader(request, TRACE_ID_EXT);
        if (StringUtils.isEmpty(traceId)) {
            traceId = TraceContext.getContext().genTraceIdAndSet();
        }

        TraceContext.getContext().setTraceId(traceId);
        MDC.put("__X-TraceID__", TraceContext.getContext().getTraceId());

        String spanId = HttpHeaderUtils.getHeader(request, SPAN_ID_EXT);
        if (StringUtils.isEmpty(spanId)) {
            spanId = SpanContext.getContext().genSpanIdAndSet();
        }

        MDC.put("SpanId", spanId);

        //设置调用链
        TraceContext.getContext().genChainRelationAndSet();
        SpanContext.getContext().genChainRelationAndSet();

        try {

            //初始化monitor
            if (Boolean.FALSE.equals(HttpMonitor.isInited())) {
                HttpMonitor.init(applicationContext, request.getLocalPort());
            }

            //按照某种类型统计
            String remoteValue;
            if (providerClientConfig.getConsumerSourceType().equals("ip")) {
                remoteValue = getIpAddr(request);
            } else {
                remoteValue = postRequest.getAppid() == null ? "unknow_appid" : postRequest.getAppid();
            }

            //有监控的调用
            String resultJson;
            if (HttpMonitor.getInstance() != null) {
                // 记录起始时间戮
                long start = System.currentTimeMillis();

                try {

                    // 执行调用
                    resultJson = doInvoke(postRequest, service, method);
                    HttpMonitor.getInstance().collect(remoteValue, service, method, start, false);

                } catch (Exception e) {

                    HttpMonitor.getInstance().collect(remoteValue, service, method, start, true);
                    logger.error("未知异常：", e);
                    BaseResult result = new BaseResult();
                    result.setSuccess(false);
                    result.setCode("1");
                    result.setDescription("服务内部错误!");
                    resultJson = JsonUtils.toJson(result);
                }
            } else {

                //无监控的调用
                resultJson = doInvoke(postRequest, service, method);
            }

            //出参日志
            if (logger.isDebugEnabled()) {
                logger.debug("http output:" + resultJson);
            }

            return resultJson;
        } finally {

            TraceContext.removeContext();
            MDC.remove("__X-TraceID__");

            SpanContext.removeContext();
            MDC.remove("SpanId");
        }
    }

    private String doInvoke(PostRequest postRequest, String service, String method) {

        BaseResult result = new BaseResult();
        postRequest.setService(service);
        postRequest.setMethod(method);

        //入参日志
        if (logger.isDebugEnabled()) {
            logger.debug("http input " + JsonUtils.toJson(postRequest));
        }

        String fullServiceName;
        //安全包检查
        if (providerClientConfig.getSafePackage() != null && providerClientConfig.getSafePackage().size() > 0) {

            boolean safe = false;
            for (String safePac : providerClientConfig.getSafePackage()) {
                if (postRequest.getService().startsWith(safePac)) {
                    safe = true;
                    break;
                }
            }

            //传入的是非安全的包接口
            if (!safe) {
                logger.warn("service is not in safe package, service=" + postRequest.getService());
                result.setSuccess(false);
                result.setCode("2");
                result.setDescription("service is not in safe package," + postRequest.getService());
                return JsonUtils.toJson(result);
            }
        }

        //接口包全路径
        fullServiceName = postRequest.getService();

        try {

            //先从缓存中取类
            Class<?> serviceClz = cacheMap.get(fullServiceName);
            if (Objects.isNull(serviceClz)) {

                try {

                    serviceClz = Class.forName(fullServiceName);

                } catch (ClassNotFoundException e) {

                    serviceClz = Thread.currentThread().getContextClassLoader().loadClass(fullServiceName);
                }

                // 如果没有加入缓存，现在加入缓存中
                cacheMap.putIfAbsent(fullServiceName, serviceClz);
            }

            Object serviceBean = applicationContext.getBean(serviceClz);
            Method[] methods = serviceClz.getMethods();
            Method methodReflect = null;
            for (Method m : methods) {
                if (m.getName().equals(postRequest.getMethod())) {
                    methodReflect = m;
                    break;
                }
            }

            if (methodReflect == null) {

                logger.warn("method not found " + postRequest.getMethod() + ",http input:" + JsonUtils.toJson(postRequest));
                result.setSuccess(false);
                result.setCode("3");
                result.setDescription("method not found " + postRequest.getMethod());
                return JsonUtils.toJson(result);
            }

            Object callResult;

            // 支持泛型
            Type[] paramTypes = methodReflect.getGenericParameterTypes();

            if (!StringUtils.isEmpty(postRequest.getToken())) {
                RpcContext.getContext().setAttachment("c_token", postRequest.getToken());
            }

            if (!StringUtils.isEmpty(postRequest.getGround())) {
                RpcContext.getContext().setAttachment("ground", postRequest.getGround());
            }

            if (!StringUtils.isEmpty(postRequest.getFlag())){
                RpcContext.getContext().setAttachment("flag", postRequest.getFlag());
            }

            if (paramTypes.length == 0) {

                // 没有入参直接调用
                callResult = methodReflect.invoke(serviceBean);

            } else if (paramTypes.length == 1) {

                Object inputParam = JsonUtils.fromJson(postRequest.getParam(), paramTypes[0]);

                callResult = methodReflect.invoke(serviceBean, inputParam);

            } else {

                logger.warn("parameter more than one," + postRequest.getMethod() + ",http input:" +
                        JsonUtils.toJson(postRequest));
                result.setSuccess(false);
                result.setCode("4");
                result.setDescription("parameter more than one," + postRequest.getMethod());
                return JsonUtils.toJson(result);
            }

            return JsonUtils.toJson(callResult);

        } catch (ClassNotFoundException | BeansException e) {
            logger.warn("bean not found!", e);
            result.setSuccess(false);
            result.setCode("2");
            result.setDescription("service not found," + postRequest.getService());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            logger.warn("call service error: ", e);
            result.setSuccess(false);
            result.setCode("5");
            result.setDescription("call service error," + postRequest.getService());
        } catch (Exception e) {
            logger.warn("call service error: \r\n http input: " + JsonUtils.toJson(postRequest), e);
            result.setSuccess(false);
            result.setCode("6");
            result.setDescription("input param to json error," + postRequest.getParam());
        }

        return JsonUtils.toJson(result);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private String getIpAddr(HttpServletRequest request) {

        String ipAddress = request.getHeader("x-forwarded-for");

        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }

        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();

            if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }
        }

        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > 15) {
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }
}
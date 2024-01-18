package com.g7.framework.exporter.client.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dreamyao
 * @title Http协议Header工具
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
public class HttpHeaderUtils {

    public static String getHeader(HttpServletRequest request, String name) {
        if (request == null) {
            throw new RuntimeException("request can't be null");
        }
        return request.getHeader(name);
    }

    public static void setHeader(HttpServletResponse response, String name, String value) {
        if (response == null) {
            throw new RuntimeException("response can't be null");
        }
        response.setHeader(name, value);
    }
}

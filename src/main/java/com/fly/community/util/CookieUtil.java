package com.fly.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/18
 */
public class CookieUtil {
    public static String getCookieInfo(String key, HttpServletRequest request) {
        if (request == null || key == null)
            throw new IllegalArgumentException("参数为空");
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie: cookies) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

package com.fly.community.controller.interceptor;

import com.fly.community.commons.Constants;
import com.fly.community.entity.LoginTicket;
import com.fly.community.entity.User;
import com.fly.community.service.UserService;
import com.fly.community.util.CookieUtil;
import com.fly.community.util.HostHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/18
 */
@Component
public class LoginTicketInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ticket = CookieUtil.getCookieInfo("ticket", request);
        if (ticket == null) return true;
        // check if the ticket is valid
        LoginTicket loginTicket = userService.findLoginTicket(ticket);
        if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
            // find the respective user by ticket
            User user = userService.findUserById(loginTicket.getUserId());
            if (user != null && user.getStatus() == Constants.ActivationState.ACTIVATION_REPEAT.getCode()) {
                // set user, similar to session
                hostHolder.setUser(user);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // add user to model, as this method executes before the template is rendered
        if (modelAndView != null && hostHolder.getUser() != null) {
            modelAndView.addObject("loginUser", hostHolder.getUser());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // remove user from hostHolder
        hostHolder.clear();
    }
}

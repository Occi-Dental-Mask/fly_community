package com.fly.community.service;

import com.fly.community.commons.Constants;
import com.fly.community.dao.LoginTicketMapper;
import com.fly.community.dao.UserMapper;
import com.fly.community.entity.LoginTicket;
import com.fly.community.entity.User;
import com.fly.community.util.CommunityUtil;
import com.fly.community.util.MailClient;
import com.fly.community.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private LoginTicketMapper loginTicketMapper;

    @Resource
    private MailClient mailClient;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private RedisTemplate redisTemplate;



    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    public User findUserById(int id) {
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 参数校验
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }
        // 查询已有账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }

        // 设置用户信息
        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(Constants.ActivationState.ACTIVATION_INITIAL.getCode());
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }


    public Integer activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == Constants.ActivationState.ACTIVATION_REPEAT.getCode()) {
            return Constants.ActivationState.ACTIVATION_REPEAT.getCode();
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, Constants.ActivationState.ACTIVATION_REPEAT.getCode());
            return Constants.ActivationState.ACTIVATION_SUCCESS.getCode();
        } else {
            return Constants.ActivationState.ACTIVATION_FAILURE.getCode();
        }
    }

    public Map<String, Object> login(String username, String password, boolean rememberMe) {
        Map<String, Object> map =  new HashMap<>();
        int expireTime = rememberMe? Constants.Time.LONG_EXPIRE_TIME: Constants.Time.SHORT_EXPIRE_TIME;
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        // validate username
        User user = userMapper.selectByName(username);

        if (user == null || user.getStatus() != Constants.ActivationState.ACTIVATION_REPEAT.getCode()) {
            map.put("usernameMsg", "用户不存在或尚未激活");
            return map;
        }
        // validate pwd, use the same method to encrypt the input pwd and compare with the stored pwd
        String pwd_encode = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(pwd_encode)) {
            map.put("passwordMsg", "密码不正确");
            return map;
        }
        // generate login ticket
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expireTime * 1000));
        // insert login ticket
//        loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        map.put("expireSeconds", expireTime);
        return map;
    }

    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public void logout(String ticket) {
//      loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public boolean checkPassword(int id, String oldPwd) {
        User user = userMapper.selectById(id);
        return user.getPassword().equals(CommunityUtil.md5(oldPwd + user.getSalt()));
    }

    public void updatePassword(int id, String newPwd) {
        User user = userMapper.selectById(id);
        userMapper.updatePassword(id, CommunityUtil.md5(newPwd + user.getSalt()));
    }

    public User findUserByName(String toName) {
        return userMapper.selectByName(toName);
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}

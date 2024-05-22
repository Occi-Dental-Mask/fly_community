package com.fly.community.controller;

import com.fly.community.annotation.LoginRequired;
import com.fly.community.entity.User;
import com.fly.community.service.FollowService;
import com.fly.community.service.LikeService;
import com.fly.community.service.UserService;
import com.fly.community.util.CommunityUtil;
import com.fly.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.fly.community.commons.Constants.EntityType.ENTITY_TYPE_USER;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/18
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;

    @Resource
    private HostHolder hostHolder;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Resource
    private FollowService followService;

    @Resource
    private LikeService likeService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile file, Model model) {
        if (file == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }
        String fileName = file.getOriginalFilename();
        // check if the file is an image
        // get the suffix of the file
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (suffix == null) {
            model.addAttribute("error", "文件格式不正确");
            return "/site/setting";
        }
        // generate a random file name
        fileName = CommunityUtil.generateUUID() + suffix;
        File fileNew = new File(uploadPath + "/" + fileName);
        try {
            // save the file
            file.transferTo(fileNew);
        } catch (Exception e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }
        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }


    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // storage path
        String filePath = uploadPath + "/" + fileName;
        // suffix
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // response image
        response.setContentType("image/" + suffix);
        try (FileInputStream fis = new FileInputStream(filePath);
             OutputStream os = response.getOutputStream())
        {
            byte[] buffer = new byte[1024];
            int b;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }


    @LoginRequired
    @RequestMapping(path = "/changepwd", method = RequestMethod.POST)
    public String changePwd(String oldPwd, String newPwd, String newPwdAgain, Model model) {
        User user = hostHolder.getUser();
        if (StringUtils.isBlank(oldPwd)) {
            model.addAttribute("oldPwdMsg", "原密码不能为空!");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPwd)) {
            model.addAttribute("newPwdMsg", "新密码不能为空!");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPwdAgain)) {
            model.addAttribute("newPwdAgainMsg", "确认密码不能为空!");
            return "/site/setting";
        }

        if (!userService.checkPassword(user.getId(), oldPwd)) {
            model.addAttribute("oldPwdMsg", "原密码不正确!");
            return "/site/setting";
        }
        if (!newPwd.equals(newPwdAgain)) {
            model.addAttribute("newPwdAgainMsg", "两次输入的密码不一致!");
            return "/site/setting";
        }
        userService.updatePassword(user.getId(), newPwd);
        return "redirect:/index";

    }


    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikes(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
}

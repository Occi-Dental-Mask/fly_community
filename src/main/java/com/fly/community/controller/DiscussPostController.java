package com.fly.community.controller;

import com.fly.community.commons.Constants;
import com.fly.community.entity.Comment;
import com.fly.community.entity.DiscussPost;
import com.fly.community.entity.Page;
import com.fly.community.entity.User;
import com.fly.community.service.CommentService;
import com.fly.community.service.DiscussPostService;
import com.fly.community.service.LikeService;
import com.fly.community.service.UserService;
import com.fly.community.util.CommunityUtil;
import com.fly.community.util.HostHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;

import static com.fly.community.commons.Constants.EntityType.ENTITY_TYPE_COMMENT;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/19
 */
@Controller
@RequestMapping("/discuss")
public class DiscussPostController {

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private UserService userService;

    @Resource
    private CommentService commentService;

    @Resource
    private HostHolder hostHolder;

    @Resource
    private LikeService likeService;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        if (hostHolder.getUser() == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(hostHolder.getUser().getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);
        return CommunityUtil.getJSONString(0, "发布成功");
    }

    @RequestMapping(value = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", discussPost);
        User user = userService.findUserById(discussPost.getUserId());
        model.addAttribute("user", user);

        // 点赞数量
        long likeCount = likeService.findEntityLikes(Constants.EntityType.ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.alreadyLiked(hostHolder.getUser().getId(), Constants.EntityType.ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // set page info
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(discussPost.getCommentCount());
        page.setLimit(5);

        List<Map<String, Object>> commentVOList = new ArrayList<>();
        List<Comment> commentList = commentService.findCommentsByEntity(Constants.CommentEntityType.COMMENT_TYPE, discussPost.getId(), page.getOffset(), page.getLimit());
        for (Comment comment: commentList) {
            Map<String, Object> commentVO = new HashMap<>();
            commentVO.put("comment", comment);
            commentVO.put("user", userService.findUserById(comment.getUserId()));
            // 点赞数量
            likeCount = likeService.findEntityLikes(ENTITY_TYPE_COMMENT, comment.getId());
            commentVO.put("likeCount", likeCount);
            // 点赞状态
            likeStatus = hostHolder.getUser() == null ? 0 :
                    likeService.alreadyLiked(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
            commentVO.put("likeStatus", likeStatus);
            commentVOList.add(commentVO);
            // for every comment there are its replies
            List<Comment> replyList = commentService.findCommentsByEntity(Constants.CommentEntityType.REPLY_TYPE, comment.getId(), 0, Integer.MAX_VALUE);
            List<Map<String, Object>> replyVOList = new ArrayList<>();
            for (Comment reply: replyList) {
                Map<String, Object> replyVO = new HashMap<>();
                replyVO.put("reply", reply);
                // the user that is replying
                replyVO.put("user", userService.findUserById(reply.getUserId()));
                User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                // to whom reply
                replyVO.put("target", target);
                // 点赞数量
                likeCount = likeService.findEntityLikes(ENTITY_TYPE_COMMENT, reply.getId());
                replyVO.put("likeCount", likeCount);
                // 点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.alreadyLiked(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                replyVO.put("likeStatus", likeStatus);
                replyVOList.add(replyVO);
            }
            commentVO.put("replies", replyVOList);
            commentVO.put("replyCount", commentService.findCommentCount(Constants.CommentEntityType.REPLY_TYPE, comment.getId()));
        }
        model.addAttribute("comments", commentVOList);

        return "/site/discuss-detail";
    }



}

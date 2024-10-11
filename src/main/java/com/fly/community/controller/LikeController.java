package com.fly.community.controller;

import com.fly.community.entity.Event;
import com.fly.community.event.EventProducer;
import com.fly.community.event.NewsConstants;
import com.fly.community.service.LikeService;
import com.fly.community.util.CommunityUtil;
import com.fly.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/21
 */

@Controller
public class LikeController implements NewsConstants {

    @Resource
    private LikeService likeService;

    @Resource
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        likeService.like(hostHolder.getUser().getId(), entityType, entityId, entityUserId);
        long likes = likeService.findEntityLikes(entityType, entityId);

        int status = likeService.alreadyLiked(hostHolder.getUser().getId(), entityType, entityId);

        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likes);
        map.put("likeStatus", status);
        // 触发点赞事件
        if (status == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }
        return CommunityUtil.getJSONString(0, null, map);
    }
}

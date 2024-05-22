package com.fly.community.service;

import com.fly.community.entity.User;
import com.fly.community.util.RedisUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.fly.community.commons.Constants.EntityType.ENTITY_TYPE_USER;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/21
 */

@Service
public class FollowService {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserService userService;

    /**
    /**
     * user follow an entity
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisUtil.getFollowerKey(entityType, entityId);

                redisOperations.multi();
                redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return redisOperations.exec();
            }
        });
    }

    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }

    /**
     * find the number of how many entities that a user followed
     * @param userId
     * @param entityType
     * @return
     */
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * find the number of how many followers that an entity has
     * @param entityType
     * @param entityId
     * @return
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**
     * check if the user has followed the entity
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * find the entity(like KOLs) that a user follows
     * use pagination
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        List<Map<String, Object>> res = new ArrayList<>();
        if (targetIds != null) {
            for (Integer targetId: targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                res.add(map);
            }
        }
        return res;
    }

    /**
     * find the followers of an entity
     */
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        List<Map<String, Object>> res = new ArrayList<>();
        if (targetIds != null) {
            for (Integer targetId: targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                res.add(map);
            }
        }
        return res;
    }
}

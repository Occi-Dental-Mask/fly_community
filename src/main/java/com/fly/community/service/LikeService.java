package com.fly.community.service;

import com.fly.community.util.RedisUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/21
 */
@Service
public class LikeService {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * someone click on the like, and both the likes that one user received and
     * that the likes that one entity received should be recorded.
     * this is a converting action.
     * if the entity is liked by this user before, executing this means unlike
     * vice versa
     *
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        // setup the redis key.
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisUtil.getUserLikeKey(entityUserId);
                // update the like the entity received
                boolean alreadyLiked = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                redisOperations.multi();
                if (alreadyLiked) {
                    // cancel the like
                    redisOperations.opsForSet().remove(entityLikeKey, userId);
                    redisOperations.opsForValue().decrement(userLikeKey);
                } else {
                    redisOperations.opsForSet().add(entityLikeKey, userId);
                    redisOperations.opsForValue().increment(userLikeKey);
                }
                return redisOperations.exec();
            }
        });
    }

    /**
     * find the number of likes that an entity received
     * @param entityType
     * @param entityId
     * @return
     */
    public long findEntityLikes(int entityType, int entityId) {
        String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /**
     * find the number of likes that one user received
     * @param userId
     * @return
     */
    public int findUserLikes(int userId) {
        String userLikeKey = RedisUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

    /**
     * find the status of the like of one user to one entity
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public int alreadyLiked(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1: 0;
    }
}

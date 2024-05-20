package com.fly.community.service;

import com.fly.community.commons.Constants;
import com.fly.community.dao.CommentMapper;
import com.fly.community.entity.Comment;
import com.fly.community.util.SensitiveFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/19
 */
@Service
public class CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private SensitiveFilter sensitiveFilter;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        // HTML filter
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        // sensitive words filter
        comment.setContent(sensitiveFilter.filterSensitive(comment.getContent()));

        // insert comment in to table
        int rows = commentMapper.insertComment(comment);

        // know the newest count of comments of one post(only the TYPE1 counts)
        int count = commentMapper.selectCountByEntity(Constants.CommentEntityType.COMMENT_TYPE, comment.getEntityId());

        // update the number of comments of one post
        discussPostService.updateCommentCount(comment.getEntityId(), count);

        return rows;

    }
}

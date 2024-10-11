package com.fly.community.dao;

import com.fly.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/19
 */
@Mapper
public interface CommentMapper {
    int selectCountByEntity(int entityType, int entityId);

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);

}

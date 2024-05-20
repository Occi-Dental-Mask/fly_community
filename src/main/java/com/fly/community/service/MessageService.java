package com.fly.community.service;

import com.fly.community.dao.MessageMapper;
import com.fly.community.entity.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/19
 */
@Service
public class MessageService {

    @Resource
    private MessageMapper messageMapper;

    public int addMessage(Message message) {
        return messageMapper.insertMessage(message);
    }

    /**
     * 查询当前用户的会话列表（实现分页查询）,针对每个会话只返回一条最新的私信
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findConversation(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    /**
     * 查询当前用户的会话数量
     * @param userId
     * @return
     */
    public int countConversation(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    /**
     * 查询某个会话所包含的私信列表（实现分页查询）
     * @param conversationId
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findMails(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    /**
     * 查询某个会话所包含的私信数量
     * @param conversationId
     * @return
     */
    public int countMails(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public void readMessage(List<Integer> ids) {
        messageMapper.updateStatus(ids, 1);
    }


}

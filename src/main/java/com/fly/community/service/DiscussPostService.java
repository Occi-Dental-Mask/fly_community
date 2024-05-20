package com.fly.community.service;

import com.fly.community.dao.DiscussPostMapper;
import com.fly.community.entity.DiscussPost;
import com.fly.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import javax.swing.plaf.PanelUI;
import java.util.List;

@Service
public class DiscussPostService {

    @Resource
    private DiscussPostMapper discussPostMapper;

    @Resource
    private SensitiveFilter sensitiveFilter;
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public int findDiscussPostRows(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public void addDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        String originalContent = discussPost.getContent();
        String originalTitle = discussPost.getTitle();
        // TODO 转义HTML标记
        discussPost.setContent(HtmlUtils.htmlEscape(originalContent));
        discussPost.setTitle(HtmlUtils.htmlEscape(originalTitle));

        // TODO 过滤标题和内容
        discussPost.setContent(sensitiveFilter.filterSensitive(discussPost.getContent()));
        discussPost.setTitle(sensitiveFilter.filterSensitive(discussPost.getTitle()));

        // 插入数据
        discussPostMapper.insertDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public void updateCommentCount(int id, int count) {
        discussPostMapper.updateCommentCount(id, count);
    }
}

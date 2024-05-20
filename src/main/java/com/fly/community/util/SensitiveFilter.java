package com.fly.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: occi
 * @date: 2024/5/19
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    // create a Trie class
    public class TrieNode {
        // is it the end of a word
        private boolean isEnd = false;
        // children nodes
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        public void addNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        public TrieNode getSubNode(Character c) {
            return subNodes.getOrDefault(c, null);
        }
    }

    // create a Trie root node
    private TrieNode rootNode = new TrieNode();

    private void addKeyword(String word) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            TrieNode node = tempNode.getSubNode(c);
            if (node == null) {
                node = new TrieNode();
                tempNode.addNode(c, node);
            }
            tempNode = node;
            if (i == word.length() - 1) {
                tempNode.setEnd(true);
            }
        }
    }


    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * @description: filter sensitive words
     * @param word
     * @return
     */
    public String filterSensitive(String word) {
        if (StringUtils.isBlank(word)) {
            return null;
        }
        // pointer1: point to the TrieNode that is currently handled
        TrieNode tempNode = rootNode;
        // pointer2: point to the beginning of the word that is currently handled
        int begin = 0;
        // pointer3: point to the end of the word that is currently handled
        int end = 0;
        StringBuilder sb = new StringBuilder();

        while (end < word.length()) {
            char c = word.charAt(end);
            // skip symbols
            if (isSymbol(c)) {
                // if the TrieNode is the root node, add the symbol to the result
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                end++;
                continue;
            }
            // check if the word is sensitive
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // the word is not sensitive
                sb.append(word.charAt(begin));
                begin++;
                end = begin;
                // reset the pointer to the root node
                tempNode = rootNode;
            } else if (tempNode.isEnd()) {
                // the word is sensitive
                sb.append("***");
                end++;
                begin = end;
                tempNode = rootNode;
            } else {
                end++;
            }
        }

        sb.append(word.substring(begin));
        return sb.toString();
    }

    @PostConstruct
    public void buildInitialTrie() {
        // read sensitive words from a file
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive_words.txt");

             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             ) {
                // read file
                String keyword;
                while ((keyword = reader.readLine()) != null) {
                    // 添加到前缀树
                    this.addKeyword(keyword);
                }
            } catch (IOException e) {
                logger.error("加载敏感词文件失败: " + e.getMessage());
            }
    }
}

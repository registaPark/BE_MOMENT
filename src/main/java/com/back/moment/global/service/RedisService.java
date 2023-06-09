package com.back.moment.global.service;

import com.back.moment.chat.entity.Chat;
import com.back.moment.chat.repository.ChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper;

    public void setRefreshValues(String userId, String key) {
        redisTemplate.opsForHash().put("Refresh",userId,key);
        redisTemplate.expire("Refresh",Duration.ofMinutes(3L));
    }

    public void setCodeValues(String userId, String key) {
        redisTemplate.opsForHash().put("Code", userId, key);
        redisTemplate.expire("Code",Duration.ofMinutes(10L));
    }

    public void setChatValues(Chat chat,Long chatRoomId,String chatId){
        String chatAsJson = null;
        try {
            chatAsJson = objectMapper.writeValueAsString(chat);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisTemplate.opsForHash().put("Chat"+chatRoomId,chatId,chatAsJson);
        redisTemplate.expire("Chat"+chatRoomId,Duration.ofMinutes(24 * 60L));
    }

    public String getRefreshToken(String userId){
        return (String) redisTemplate.opsForHash().get("Refresh",userId);
    }

    public String getCode(String userId){
        return (String) redisTemplate.opsForHash().get("Code",userId);
    }

    public Chat getChat(Long chatRoomId,String chatId){
        String chatJson = (String) redisTemplate.opsForHash().get("Chat" + chatRoomId, chatId);
        try {
            return objectMapper.readValue(chatJson, Chat.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public void saveChatsToDB(Long chatRoomId){
        List<Chat> chats = getChats(chatRoomId);
        chatRepository.saveAll(chats);
        redisTemplate.delete("Chat"+chatRoomId);
    }
    public void deleteValues(String key){
        redisTemplate.delete(key);
    }

    private List<Chat> getChats(Long chatRoomId){
        List<Object> chats = redisTemplate.opsForHash().values("Chat" + chatRoomId);
        ArrayList<Chat> chatList = new ArrayList<>();
        for (Object c : chats) {
            try {
                Chat chat = objectMapper.readValue((String) c, Chat.class);
                chatList.add(chat);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return chatList;
    }
}

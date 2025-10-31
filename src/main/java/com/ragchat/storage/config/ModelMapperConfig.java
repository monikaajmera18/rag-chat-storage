package com.ragchat.storage.config;

import com.ragchat.storage.dto.MessageResponse;
import com.ragchat.storage.dto.SessionResponse;
import com.ragchat.storage.model.ChatMessage;
import com.ragchat.storage.model.ChatSession;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // Set matching strategy to strict for better accuracy
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true)
                .setAmbiguityIgnored(true);

        // Custom mapping for ChatMessage to MessageResponse
        modelMapper.addMappings(new PropertyMap<ChatMessage, MessageResponse>() {
            @Override
            protected void configure() {
                map().setSessionId(source.getSession().getId());
            }
        });

        // ChatSession to SessionResponse mapping (basic fields only)
        // messageCount will be set manually in the service
        modelMapper.createTypeMap(ChatSession.class, SessionResponse.class);

        return modelMapper;
    }
}
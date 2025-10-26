package com.ragchat.storage.dto;

import com.ragchat.storage.model.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long sessionId;
    private SenderType sender;
    private String content;
    private String context;
    private LocalDateTime timestamp;
}
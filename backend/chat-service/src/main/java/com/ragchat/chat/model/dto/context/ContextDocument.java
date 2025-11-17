package com.ragchat.chat.model.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextDocument {

    private String sessionId;
    private String documentId;
    private String filename;
    private Integer chunkIndex;
    private String snippet;
    private Double score;
}

package com.ragchat.chat.model.dto.context;

import java.util.List;
import java.util.Map;
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
public class ChatMessageContext {

    /**
     * Source of the context, e.g. "session-documents" for RAG, or "client" for custom metadata.
     */
    private String source;

    /**
     * Documents used as RAG context (if any).
     */
    private List<ContextDocument> documents;

    /**
     * Arbitrary additional context supplied by the client or internal callers.
     */
    private Map<String, Object> extra;
}

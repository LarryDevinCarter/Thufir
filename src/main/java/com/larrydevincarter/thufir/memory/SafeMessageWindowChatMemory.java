package com.larrydevincarter.thufir.memory;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class SafeMessageWindowChatMemory implements ChatMemory {

    private final MessageWindowChatMemory delegate;

    public SafeMessageWindowChatMemory(int maxMessages) {
        this.delegate = MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    @Override
    public Object id() {
        return null;
    }

    @Override
    public void add(ChatMessage message) {
        if (message == null) {
            log.warn("Blocked: null message");
            return;
        }

        if (isEmptyOrInvalid(message)) {
            log.warn("Blocked empty/invalid message - type: {}, preview: '{}'",
                    message.type(),
                    extractTextPreview(message));
            return;
        }

        delegate.add(message);
        log.debug("Added valid message - type: {}", message.type());
    }

    public void add(ChatMessage... messages) {
        if (messages == null) {
            return;
        }
        Arrays.stream(messages).forEach(this::add);
    }

    public void add(Iterable<ChatMessage> messages) {
        if (messages == null) {
            return;
        }
        messages.forEach(this::add);
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private boolean isEmptyOrInvalid(ChatMessage message) {
        String text = extractText(message);
        boolean hasText = text != null && !text.trim().isEmpty();

        if (message instanceof AiMessage ai) {
            if (ai.hasToolExecutionRequests()) {
                return false;
            }
            return !hasText;
        }

        return !hasText;
    }

    private String extractText(ChatMessage message) {
        return switch (message) {
            case UserMessage user -> user.hasSingleText() ? user.singleText() : concatTextContents(user.contents());
            case AiMessage ai -> ai.text();
            case SystemMessage system -> system.text();
            case ToolExecutionResultMessage tool -> tool.text();
            default -> null;
        };
    }

    private String concatTextContents(List<Content> contents) {
        StringBuilder sb = new StringBuilder();
        contents.stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .filter(t -> t != null && !t.trim().isEmpty())
                .forEach(t -> sb.append(t).append(" "));
        return sb.toString().trim();
    }

    private String extractTextPreview(ChatMessage message) {
        String text = extractText(message);
        return text == null ? "NULL" : text.substring(0, Math.min(30, text.length()));
    }

    public int size() {
        return delegate.messages().size();
    }
}
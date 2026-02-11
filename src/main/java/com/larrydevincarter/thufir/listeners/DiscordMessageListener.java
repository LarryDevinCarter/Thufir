package com.larrydevincarter.thufir.listeners;

import com.larrydevincarter.thufir.configs.AiServiceConfig;
import com.larrydevincarter.thufir.services.Assistant;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Slf4j
public class DiscordMessageListener extends ListenerAdapter {

    private final Assistant chattingAssistant;
    private final Assistant workingAssistant;

    @Value("${discord.bot.channel-id}")
    private String monitoredChannelId;
    @Value("${discord.bot.chat-prefix}")
    private String chatPrefix;
    @Value("${discord.bot.command-prefix}")
    private String commandPrefix;

    public DiscordMessageListener(@Qualifier("chattingAssistant") Assistant chattingAssistant,
                                  @Qualifier("workingAssistant") Assistant workingAssistant) {
        this.chattingAssistant = chattingAssistant;
        this.workingAssistant = workingAssistant;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        TextChannel channel = event.getChannel().asTextChannel();
        if (!channel.getId().equals(monitoredChannelId)) return;

        String content = event.getMessage().getContentRaw().trim();

        if (content.startsWith(commandPrefix)) {
            String command = content.substring(commandPrefix.length()).trim();
            processTradingReply(command, event);
            return;
        }

        if (content.toLowerCase().startsWith(chatPrefix)) {
            String message = content.substring(chatPrefix.length()).trim();
            processChatReply(message, event);
            return;
        }

        if (event.getMessage().getMentions().getMembers().stream()
                .anyMatch(m -> m.getId().equals(event.getJDA().getSelfUser().getId()))) {
            processTradingReply(content, event);
        }
    }

    private void processTradingReply(String content, MessageReceivedEvent event) {
        String messageBlock = String.format(
                "Time: %s CST\nContent: %s\nContext: DISCORD_REPLY\nUrgent: false\n\n",
                LocalDateTime.now(ZoneId.of("America/Chicago")), content
        );

        String prompt = """
            Larry just replied via Discord. Here are all unread messages:
            
            %s
            
            Current time: %s CST
            
            Review immediately:
            - If command/override → acknowledge & adjust behavior
            - If clarification needed → ask via sendMessageToLarry
            - Send confirmation back to Discord
        """.formatted(messageBlock, LocalDateTime.now(ZoneId.of("America/Chicago")));

        String assistantResponse = workingAssistant.chat(prompt);

        log.info("Immediate Discord reply processed. Assistant response: {}", assistantResponse);
    }

    private void processChatReply(String content, MessageReceivedEvent event) {
        String messageBlock = String.format(
                "Time: %s CST\nContent: %s\nContext: DISCORD_CHAT\nUrgent: false\n\n",
                LocalDateTime.now(ZoneId.of("America/Chicago")), content
        );

        String prompt = """
            Larry just replied via Discord. Here are all unread messages:
            
            %s
            
            Current time: %s CST
            
            Review immediately:
            - If command/override → acknowledge & adjust behavior
            - If clarification needed → ask via sendMessageToLarry
            - Send confirmation back to Discord
        """.formatted(messageBlock, LocalDateTime.now(ZoneId.of("America/Chicago")));

        String assistantResponse = chattingAssistant.chat(prompt);

        log.info("Immediate Discord reply processed. Assistant response: {}", assistantResponse);
    }
}
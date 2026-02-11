package com.larrydevincarter.thufir.configs;

import com.larrydevincarter.thufir.services.listeners.DiscordMessageListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Value("${discord.bot.token}")
    private String token;

    @Value("${discord.bot.channel-id}")
    private String channelId;

    private JDA jda;

    @Bean
    public JDA jda(DiscordMessageListener messageListener) throws Exception {
        jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .setChunkingFilter(ChunkingFilter.NONE)
                .addEventListeners(messageListener)
                .setActivity(Activity.watching("Watching the market"))
                .build();

        jda.awaitReady();
        return jda;
    }

    @PostConstruct
    public void onStart() {
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}
package com.discordBot.demo.discord.listener;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InteractionDebugListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(InteractionDebugListener.class);

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        log.info("🟡 [ButtonEvent] {} from {} | acknowledged={}",
                event.getComponentId(), event.getUser().getName(), event.isAcknowledged());
    }
    @Override
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
        log.info("🧩 [DEBUG] Interaction created: type={}, user={}, id={}, acknowledged={}",
                event.getClass().getSimpleName(),
                event.getUser() != null ? event.getUser().getName() : "unknown",
                event.getId(),
                event.isAcknowledged());
    }
}

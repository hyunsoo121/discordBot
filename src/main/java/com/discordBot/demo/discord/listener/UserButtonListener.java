package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.UserSearchHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserButtonListener extends ListenerAdapter {

    private final UserSearchHandler userSearchHandler;
    private static final String ID_PREFIX = "userstats_";

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(ID_PREFIX)) {
            userSearchHandler.handlePagination(event);
        }
    }
}
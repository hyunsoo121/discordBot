package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchButtonListener extends ListenerAdapter {

    private final MatchImageHandler matchImageHandler;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(MatchImageHandler.BUTTON_ID_CONFIRM) ||
                componentId.startsWith(MatchImageHandler.BUTTON_ID_CANCEL)) {

            event.deferEdit().queue();
            matchImageHandler.handleFinalConfirmation(event);
        }
    }
}

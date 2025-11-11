package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.RankingHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingButtonListener extends ListenerAdapter {

    private final RankingHandler rankingHandler;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(RankingHandler.SORT_BUTTON_ID_PREFIX) ||
                componentId.startsWith(RankingHandler.PAGINATION_BUTTON_ID_PREFIX)) {

            event.deferEdit().queue();
            rankingHandler.handleRankingButtonInteraction(event);
        }
    }
}
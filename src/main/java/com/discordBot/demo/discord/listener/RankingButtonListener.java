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

        boolean isGeneralRankingButton = componentId.startsWith(RankingHandler.SORT_BUTTON_ID_PREFIX) ||
                componentId.startsWith(RankingHandler.PAGINATION_BUTTON_ID_PREFIX);

        boolean isLineRankingButton = componentId.startsWith(RankingHandler.SORT_LINE_BUTTON_ID_PREFIX) ||
                componentId.startsWith(RankingHandler.PAGINATION_LINE_BUTTON_ID_PREFIX);

        if (isGeneralRankingButton || isLineRankingButton) {

            event.deferEdit().queue();

            rankingHandler.handleRankingButtonInteraction(event);
        }
    }
}
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

        // ⭐ 수정: 모든 매치 관련 버튼 ID를 확인합니다.
        // ID 포맷은 'ACTION:...' 이므로, 모든 매치 관련 액션을 포함합니다.
        if (componentId.startsWith(MatchImageHandler.BUTTON_ID_CONFIRM) ||
                componentId.startsWith(MatchImageHandler.BUTTON_ID_CANCEL) ||
                componentId.startsWith(MatchImageHandler.BUTTON_ID_EDIT)) {

            // ⭐ 수정: 모든 버튼 상호작용은 통합 핸들러로 위임합니다.
            matchImageHandler.handleButtonInteraction(event);
        }
    }
}
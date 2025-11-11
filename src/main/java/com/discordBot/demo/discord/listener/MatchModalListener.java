package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.MatchImageHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchModalListener extends ListenerAdapter {

    private final MatchImageHandler matchImageHandler;

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        // ⭐ 수정: 새로운 모달 ID 접두사 (MODAL_ID_BASE)를 확인합니다.
        // ID 포맷은 "MODAL_ID_BASE:TEMP_MATCH_ID:TEAM:CATEGORY" 이므로 startsWith로 확인해야 합니다.
        if (modalId.startsWith(MatchImageHandler.MODAL_ID_BASE)) {

            // ⭐ 수정: 핸들러 메서드 이름을 handleModalInteraction으로 변경합니다.
            matchImageHandler.handleModalInteraction(event);
        }
    }
}
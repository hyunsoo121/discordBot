package com.discordBot.demo.discord.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface MatchImageHandler {

    String BUTTON_ID_CONFIRM = "MATCH_CONFIRM";
    String BUTTON_ID_CANCEL = "MATCH_CANCEL";
    String BUTTON_ID_EDIT = "MATCH_EDIT"; // 통합된 수정 버튼 ID
    String MODAL_ID_BASE = "MATCH_MODAL_SUBMIT";

    void handleMatchUploadCommand(SlashCommandInteractionEvent event);

    /** 버튼 인터랙션 (확인, 취소, 수정 버튼) 처리 */
    void handleButtonInteraction(ButtonInteractionEvent event);

    /** 모달 제출 인터랙션 (수정 후 제출) 처리 */
    void handleModalInteraction(ModalInteractionEvent event);
}
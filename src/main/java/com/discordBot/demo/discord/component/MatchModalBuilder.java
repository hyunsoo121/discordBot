package com.discordBot.demo.discord.component; // discord 패키지로 분리

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

@Component
public class MatchModalBuilder {

    // ⭐ 모달/인풋 컴포넌트 ID 상수 정의 (다른 클래스들과 공유)
    public static final String MODAL_ID_PREFIX = "match-register-modal-";
    public static final String INPUT_ID_DISCORD_USER = "discord-user-id";
    public static final String INPUT_ID_LOL_NICKNAME = "lol-nickname";
    public static final String INPUT_ID_KILLS = "kills";
    public static final String INPUT_ID_DEATHS = "deaths";
    public static final String INPUT_ID_ASSISTS = "assists";
    public static final String BUTTON_ID_NEXT_PLAYER = "next-player-modal-trigger";


    /**
     * 선수 정보 입력을 위한 모달을 생성합니다.
     */
    public Modal buildPlayerStatsModal(String initiatorId, int playerNumber) {

        // Modal ID에 세션 정보를 포함하여 어떤 모달인지 식별합니다.
        String modalId = MODAL_ID_PREFIX + playerNumber + "-" + initiatorId;

        TextInput discordUser = TextInput.create(INPUT_ID_DISCORD_USER, "선수 Discord ID", TextInputStyle.SHORT)
                .setPlaceholder("디스코드 ID를 숫자만 입력해주세요.")
                .setMinLength(1)
                .setRequired(true)
                .build();

        TextInput lolNickname = TextInput.create(INPUT_ID_LOL_NICKNAME, "롤 닉네임 (GameName#TagLine)", TextInputStyle.SHORT)
                .setPlaceholder("예: Hide On Bush#KR1")
                .setMinLength(3)
                .setRequired(true)
                .build();

        TextInput kills = TextInput.create(INPUT_ID_KILLS, "Kills", TextInputStyle.SHORT)
                .setPlaceholder("0~99 사이의 숫자")
                .setMinLength(1)
                .setRequired(true)
                .build();

        TextInput deaths = TextInput.create(INPUT_ID_DEATHS, "Deaths", TextInputStyle.SHORT)
                .setPlaceholder("0~99 사이의 숫자")
                .setMinLength(1)
                .setRequired(true)
                .build();

        TextInput assists = TextInput.create(INPUT_ID_ASSISTS, "Assists", TextInputStyle.SHORT)
                .setPlaceholder("0~99 사이의 숫자")
                .setMinLength(1)
                .setRequired(true)
                .build();

        return Modal.create(modalId, "경기 기록 입력 (선수 " + playerNumber + "/10)")
                .addActionRow(discordUser)
                .addActionRow(lolNickname)
                .addActionRow(kills)
                .addActionRow(deaths)
                .addActionRow(assists)
                .build();
    }
}
package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingButtonListener extends ListenerAdapter {

    private final RankingService rankingService;
    private final RankingHandler rankingHandler;

    private static final String SHOW_BUTTON_ID = "show_rank_details";
    private static final String HIDE_BUTTON_ID = "hide_rank_details";
    private static final int MIN_GAMES_THRESHOLD = 1;


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(SHOW_BUTTON_ID) || componentId.startsWith(HIDE_BUTTON_ID)) {

            event.deferEdit().queue(); // 메시지를 수정할 것임을 승인

            Long discordServerId;
            try {
                // 버튼 ID에서 서버 ID 추출 (ID는 마지막 '_' 뒤에 위치)
                discordServerId = Long.parseLong(componentId.substring(componentId.lastIndexOf('_') + 1));
            } catch (Exception e) {
                event.getHook().sendMessage("❌ 서버 정보를 찾을 수 없습니다.").setEphemeral(true).queue();
                return;
            }

            String serverName = event.getGuild().getName();
            List<UserRankDto> rankedList = rankingService.getRankingByKDA(discordServerId, MIN_GAMES_THRESHOLD);

            if (rankedList.isEmpty()) {
                event.getHook().sendMessage("❌ 랭킹 데이터가 없습니다.").setEphemeral(true).queue();
                return;
            }

            MessageEmbed newEmbed;
            Button newButton;

            if (componentId.startsWith(SHOW_BUTTON_ID)) {
                // '상세 지표 보기' 클릭 시: 상세 Embed로 수정
                newEmbed = rankingHandler.createDetailedRankingEmbed(discordServerId, serverName, rankedList);
                // 버튼을 '숨기기'로 변경
                newButton = Button.secondary(HIDE_BUTTON_ID + "_" + discordServerId, "▲ 상세 지표 숨기기");

            } else { // '숨기기' 버튼 클릭 시: 초기 요약 Embed로 수정
                // 초기 요약 Embed 생성
                newEmbed = rankingHandler.createSummaryRankingEmbed(discordServerId, serverName, rankedList);
                // 버튼을 '상세 보기'로 변경
                newButton = Button.primary(SHOW_BUTTON_ID + "_" + discordServerId, "🔍 상세 지표 보기");
            }

            // 3. 기존 메시지를 새로운 Embed와 새로운 버튼으로 수정 (토글 완료)
            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(ActionRow.of(newButton))
                    .queue();
        }
    }
}
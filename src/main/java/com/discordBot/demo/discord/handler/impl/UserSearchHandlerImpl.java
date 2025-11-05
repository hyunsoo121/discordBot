package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.UserSearchHandler;
import com.discordBot.demo.discord.presenter.UserSearchPresenter;
import com.discordBot.demo.domain.dto.UserSearchDto;
import com.discordBot.demo.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSearchHandlerImpl implements UserSearchHandler {

    private final UserSearchService userSearchService;
    private final UserSearchPresenter userSearchPresenter;

    private static final String COMMAND_NAME = "유저검색"; // SlashCommandListener에서 정의된 이름으로 가정
    private static final int CHAMPIONS_PER_PAGE = 5;
    private static final String ID_PREFIX = "userstats_"; // Presenter에서 사용된 접두사

    @Override
    public void handleUserStatsCommand(SlashCommandInteractionEvent event) {

        if (!event.getName().equals(COMMAND_NAME)) return;

        event.deferReply(true).queue();

        OptionMapping discordUserOption = event.getOption("discord-user");
        OptionMapping nicknameOption = event.getOption("riot-nickname");
        OptionMapping tagOption = event.getOption("riot-tag");

        Long serverId = event.getGuild().getIdLong();
        Long targetDiscordUserId = null;
        Optional<UserSearchDto> statsDtoOpt = Optional.empty();
        String discordMention = null;

        try {
            // ... (검색 및 데이터 로딩 로직 유지) ...
            if (discordUserOption != null) {
                User discordUser = discordUserOption.getAsUser();
                targetDiscordUserId = discordUser.getIdLong();
                discordMention = discordUser.getAsMention();

                statsDtoOpt = userSearchService.searchUserInternalStatsByDiscordId(targetDiscordUserId, serverId);

            } else if (nicknameOption != null && tagOption != null) {
                String summonerName = nicknameOption.getAsString();
                String tagLine = tagOption.getAsString();

                statsDtoOpt = userSearchService.searchUserInternalStatsByRiotId(summonerName, tagLine, serverId);

                if (statsDtoOpt.isPresent()) {
                    UserSearchDto stats = statsDtoOpt.get();
                    targetDiscordUserId = stats.getDiscordUserId();
                    // 동기 호출 방지: DB에서 ID를 가져왔으므로 수동 멘션 생성
                    discordMention = "<@" + targetDiscordUserId + ">";
                }

            } else {
                event.getHook().sendMessage("❌ 오류: 검색할 디스코드 유저를 멘션하거나, Riot ID (닉네임과 태그)를 입력해야 합니다.").setEphemeral(true).queue();
                return;
            }

            if (statsDtoOpt.isEmpty()) {
                String failMessage = (targetDiscordUserId != null)
                        ? discordMention + "님은 해당 서버에 내전 기록이 없습니다."
                        : "❌ 검색 실패: Riot ID가 서버에 등록되지 않았거나 유저를 찾을 수 없습니다.";

                event.getHook().sendMessage(failMessage).setEphemeral(true).queue();
                return;
            }

            UserSearchDto stats = statsDtoOpt.get();
            int totalPages = (int) Math.ceil((double) stats.getChampionStatsList().size() / CHAMPIONS_PER_PAGE);

            // 5. Embed와 버튼 생성
            MessageEmbed embed = userSearchPresenter.buildStatsEmbed(stats, 0); // 첫 페이지 Embed
            ActionRow paginationRow = userSearchPresenter.createPaginationActionRow(0, totalPages, targetDiscordUserId);

            // 6. 최종 응답: 멘션, Embed, 버튼, Ephemeral 전송
            event.getHook().sendMessage(discordMention + "님의 내전 지표입니다.")
                    .addEmbeds(embed)
                    .addComponents(paginationRow)
                    .setEphemeral(true)
                    .queue();

        } catch (Exception e) {
            log.error("유저 지표 검색 중 오류 발생: {}", event.getName(), e);
            event.getHook().sendMessage("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.").setEphemeral(true).queue();
        }
    }


    @Override
    public void handlePagination(ButtonInteractionEvent event) {

        // Listener에서 event.deferEdit().queue()를 처리했으므로, 바로 Hook을 사용합니다.

        String componentId = event.getComponentId();
        String[] parts = componentId.split("_");

        // ID 구조: userstats_DISCORDUSERID_ACTION_DESTINATION_INDEX (총 4 파트)

        try {
            // parts[1]: DISCORDUSERID
            // parts[2]: ACTION (prev/next)
            // parts[3]: DESTINATION_INDEX (새 페이지 인덱스)

            Long discordUserId = Long.parseLong(parts[1]);

            // ⭐ 핵심 수정: 버튼 ID에서 이동할 목적지 인덱스(새 페이지 인덱스)를 바로 추출
            int newPageIndex = Integer.parseInt(parts[3]);

            Long serverId = event.getGuild().getIdLong();

            // 1. 서비스에서 데이터 다시 조회
            Optional<UserSearchDto> statsOpt = userSearchService.searchUserInternalStatsByDiscordId(discordUserId, serverId);

            if (statsOpt.isEmpty()) {
                event.getHook().editOriginal("❌ 오류: 통계 데이터 조회에 실패했습니다. (유저 통계 부재)").setComponents().queue();
                return;
            }

            UserSearchDto stats = statsOpt.get();
            int totalChampions = stats.getChampionStatsList().size();
            int totalPages = (int) Math.ceil((double) totalChampions / CHAMPIONS_PER_PAGE);

            // 2. 최종 검사 (버튼 비활성화가 풀린 경우 대비)
            if (newPageIndex < 0 || newPageIndex >= totalPages) {
                log.warn("사용자 ID {}가 유효하지 않은 페이지 인덱스({})를 요청했습니다.", discordUserId, newPageIndex);
                return; // 유효하지 않은 요청 무시
            }

            // 3. 새로운 Embed와 버튼 구성
            MessageEmbed newEmbed = userSearchPresenter.buildStatsEmbed(stats, newPageIndex);

            // 4. 버튼 생성 시, 새롭게 획득한 newPageIndex를 사용
            ActionRow newPaginationRow = userSearchPresenter.createPaginationActionRow(newPageIndex, totalPages, discordUserId);

            // 5. 메시지 업데이트
            event.getHook().editOriginalEmbeds(newEmbed)
                    .setComponents(newPaginationRow)
                    .queue();

        } catch (NumberFormatException e) {
            log.error("페이지네이션 ID 파싱 오류 (숫자 변환 실패): {}", componentId, e);
            event.getHook().editOriginal("❌ 오류: 버튼 ID 파싱 중 오류가 발생했습니다.").setComponents().queue();
        } catch (Exception e) {
            log.error("페이지네이션 처리 중 예기치 않은 오류 발생: {}", componentId, e);
            event.getHook().editOriginal("❌ 페이지 업데이트 중 서버 오류가 발생했습니다.").setComponents().queue();
        }
    }
}
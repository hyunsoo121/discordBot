package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.UserSearchHandler;
import com.discordBot.demo.discord.presenter.UserSearchPresenter;
import com.discordBot.demo.domain.dto.UserSearchDto;
import com.discordBot.demo.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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

    private static final String COMMAND_NAME = "유저검색";
    private static final int CHAMPIONS_PER_PAGE = 5;

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
            if (discordUserOption != null) {
                // CASE 1: Discord User 멘션 검색
                User discordUser = discordUserOption.getAsUser();
                targetDiscordUserId = discordUser.getIdLong();
                discordMention = discordUser.getAsMention();

                statsDtoOpt = userSearchService.searchUserInternalStatsByDiscordId(targetDiscordUserId, serverId);

            } else if (nicknameOption != null && tagOption != null) {
                // CASE 2: Riot ID 검색
                String summonerName = nicknameOption.getAsString();
                String tagLine = tagOption.getAsString();

                statsDtoOpt = userSearchService.searchUserInternalStatsByRiotId(summonerName, tagLine, serverId);

                if (statsDtoOpt.isPresent()) {
                    UserSearchDto stats = statsDtoOpt.get();
                    targetDiscordUserId = stats.getDiscordUserId();
                    // JDA를 사용해 User 객체를 가져와 멘션 메시지 생성
                    discordMention = event.getJDA().retrieveUserById(targetDiscordUserId).complete().getAsMention();
                }

            } else {
                // 옵션 부족
                event.getHook().sendMessage("❌ 오류: 검색할 디스코드 유저를 멘션하거나, Riot ID (닉네임과 태그)를 입력해야 합니다.").setEphemeral(true).queue();
                return;
            }

            // 4. 결과 응답 처리
            if (statsDtoOpt.isEmpty()) {
                String failMessage = (targetDiscordUserId != null)
                        ? discordMention + "님은 해당 서버에 내전 기록이 없습니다."
                        : "❌ 검색 실패: Riot ID가 서버에 등록되지 않았거나 유저를 찾을 수 없습니다.";

                event.getHook().sendMessage(failMessage).setEphemeral(true).queue();
                return;
            }

            UserSearchDto stats = statsDtoOpt.get();

            // 챔피언 목록 페이지네이션 설정
            int totalPages = (int) Math.ceil((double) stats.getChampionStatsList().size() / CHAMPIONS_PER_PAGE);

            // ⭐ 5. Embed와 버튼 생성
            MessageEmbed embed = userSearchPresenter.buildStatsEmbed(stats, 0); // 첫 페이지 Embed
            ActionRow paginationRow = userSearchPresenter.createPaginationActionRow(0, totalPages, targetDiscordUserId);

            // ⭐ 6. 최종 응답: 멘션, Embed, 버튼, Ephemeral 전송
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
}
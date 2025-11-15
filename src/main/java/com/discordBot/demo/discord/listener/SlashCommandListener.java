package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.AdminCommandHandler;
import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.discord.handler.RegistrationHandler;
import com.discordBot.demo.discord.handler.UserSearchHandler; // UserSearchHandler 주입
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SlashCommandListener extends ListenerAdapter {

    private final MatchImageHandler matchImageHandler;
    private final RankingHandler rankingHandler;
    private final AdminCommandHandler adminCommandHandler;
    private final RegistrationHandler registrationHandler;
    private final UserSearchHandler userSearchHandler; // UserSearchHandler 주입

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        try {
            switch (event.getName()){
                case "롤계정등록":
                    registrationHandler.handleRegisterCommand(event);
                    break;

                case "내전경기등록": // match-upload
                    matchImageHandler.handleMatchUploadCommand(event);
                    break;

                case "내전통합랭킹": // rank-check
                    rankingHandler.handleRankingCommand(event);
                    break;

                case "라인별순위":
                    rankingHandler.handleLineRankingCommand(event);
                    break;

                case "유저검색": // user-stats
                    userSearchHandler.handleUserStatsCommand(event);
                    break;

                case "데이터초기화": // init-data
                    adminCommandHandler.handleInitDataCommand(event);
                    break;

                default:
                        // deferReply가 되지 않은 경우 event.reply() 사용
                        event.reply("알 수 없는 커맨드입니다.").setEphemeral(true).queue();
                        break;
            }
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 비즈니스 로직 오류가 발생했습니다.";
            // Hook은 try 블록 초기에 deferReply가 되었다는 가정 하에 사용
            event.getHook().sendMessage(message).setEphemeral(true).queue();

        } catch (Exception e) {
            log.error("슬래시 커맨드 처리 중 예기치 않은 오류 발생: {}", event.getName(), e);
            event.getHook().sendMessage("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.").setEphemeral(true).queue();
        }
    }

    private List<CommandData> buildCommandDataList() {
        List<CommandData> commandDataList = new ArrayList<>();

        commandDataList.add(
                Commands.slash("롤계정등록", "관리자 전용: 특정 유저의 롤 계정과 선호 라인을 연결합니다.")
                        .addOption(OptionType.USER, "target-user", "롤 계정을 연결할 디스코드 유저를 @멘션하세요.", true)
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
                        .addOption(OptionType.STRING, "preferred-lines", "선호 라인을 1개 이상 입력하세요 (콤마로 구분, 예: 탑, 정글, 원딜, 미드, 서폿)", true)
        );

        commandDataList.add(
                Commands.slash("내전경기등록", "경기 결과 이미지로 기록을 등록합니다.")
                        .addOption(OptionType.ATTACHMENT, "input-image", "경기 결과 스크린샷 이미지", true)
        );

        commandDataList.add(
                Commands.slash("내전통합랭킹", "내전 랭킹을 확인합니다")
        );

        commandDataList.add(
                Commands.slash("라인별순위", "특정 라인의 랭킹을 확인합니다.")
                        .addOption(OptionType.STRING, "라인이름", "확인할 라인을 입력하세요 (TOP, JUNGLE, MID, ADC, SUPPORT)", true)
        );

        commandDataList.add(
                Commands.slash("유저검색", "특정 유저의 내전 지표를 검색합니다.")
                        .addOption(OptionType.USER, "discord-user", "검색할 디스코드 유저를 @멘션하세요.", false)
                        .addOption(OptionType.STRING, "lol-nickname", "검색할 롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", false)
        );

        commandDataList.add(
                Commands.slash("데이터초기화", "관리자 전용: 현재 서버에 테스트용 5경기 기록을 주입합니다.")
        );

        return commandDataList;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        log.info("기존 서버 명령어 등록 시작: {}", event.getGuild().getName());
        event.getGuild().updateCommands().addCommands(buildCommandDataList()).queue();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        log.info("🎉 새로운 서버에 참가하여 명령어 등록: {} (ID: {})", event.getGuild().getName(), event.getGuild().getId());

            event.getGuild().updateCommands().addCommands(buildCommandDataList()).queue(
                success -> log.info("✅ 새로운 서버에 명령어 등록 성공: {}", event.getGuild().getName()),
                failure -> log.error("❌ 새로운 서버에 명령어 등록 실패: {}", event.getGuild().getName(), failure)
        );
    }
}
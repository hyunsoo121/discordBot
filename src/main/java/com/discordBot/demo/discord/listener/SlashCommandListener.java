package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.AdminCommandHandler;
import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.discord.handler.RankingHandler;
import com.discordBot.demo.discord.handler.RegistrationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        try {
            switch (event.getName()){
                case "register":
                    registrationHandler.handleRegisterCommand(event);
                    break;

                case "match-upload":
                    matchImageHandler.handleMatchUploadCommand(event);
                    break;

                case "rank-check":
                    rankingHandler.handleRankingCommand(event);
                    break;

                case "init-data":
                    adminCommandHandler.handleInitDataCommand(event);
                    break;

                default:
                    event.getHook().sendMessage("알 수 없는 커맨드입니다.").queue();
                    break;
            }
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 비즈니스 로직 오류가 발생했습니다.";
            event.getHook().sendMessage(message).queue();
        } catch (Exception e) {
            log.error("슬래시 커맨드 처리 중 예기치 않은 오류 발생: {}", event.getName(), e);
            event.getHook().sendMessage("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.").queue();
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();

        commandDataList.add(
                Commands.slash("register", "관리자 전용: 특정 유저의 롤 닉네임(Riot ID)을 연결합니다.")
                        .addOption(OptionType.USER, "target-user", "롤 계정을 연결할 디스코드 유저를 @멘션하세요.", true)
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
        );

        commandDataList.add(
                Commands.slash("match-upload", "경기 결과 이미지로 기록을 등록합니다.")
                        .addOption(OptionType.ATTACHMENT, "result-image", "경기 결과 스크린샷 이미지", true)
        );

        commandDataList.add(
                Commands.slash("rank-check", "내전 랭킹을 확인합니다")
        );

        commandDataList.add(
                Commands.slash("init-data", "관리자 전용: 현재 서버에 테스트용 5경기 기록을 주입합니다.")
        );
        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}
package com.discordBot.demo.discord.listener;

import com.discordBot.demo.discord.handler.AdminCommandHandler;
import com.discordBot.demo.service.UserService;
import com.discordBot.demo.discord.handler.MatchImageHandler;
import com.discordBot.demo.discord.handler.RankingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@RequiredArgsConstructor
public class SlashCommandListener extends ListenerAdapter {

    private final UserService userService;
    private final MatchImageHandler imageHandler;
    private final RankingHandler rankingHandler;
    private final AdminCommandHandler adminCommandHandler;

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

    // --- 1. 슬래시 명령어 라우팅 ---
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        // 1. 응답 지연 (Deferred Reply)
        // /rank-check는 본인에게만 보이도록 true로 설정
        // 나머지 명령어도 대부분 개인 응답이므로 true로 설정

        // ⭐ 수정: 모든 명령에 대해 Ephemeral(true)로 deferReply 호출
        event.deferReply(true).queue();

        try {
            switch (event.getName()){
                case "register":
                    handleRegisterCommand(event);
                    break;

                case "match-upload":
                    imageHandler.handleMatchUploadCommand(event);
                    break;

                case "rank-check":
                    // ⭐ RankingHandler 내에서 응답을 Ephemeral로 처리하도록 위임
                    rankingHandler.handleRankingCommand(event);
                    break;

                case "my-info":
                    event.getHook().sendMessage("**[내 정보] 기능은 아직 구현되지 않았습니다.**").queue();
                    break;

                case "init-data":
                    handleInitData(event);
                    break;

                default:
                    // getHook()은 이미 deferReply(true)를 따릅니다.
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

    // --- 2. 버튼 상호작용 라우팅 ---
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(MatchImageHandler.BUTTON_ID_CONFIRM) ||
                componentId.startsWith(MatchImageHandler.BUTTON_ID_CANCEL)) {

            event.deferEdit().queue();
            imageHandler.handleFinalConfirmation(event);
        }
    }

    private void handleInitData(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            // deferReply(true) 상태이므로 getHook 사용
            event.getHook().sendMessage("❌ 오류: **데이터 초기화** 명령어는 서버 관리자만 사용할 수 있습니다.").queue();
            return;
        }
        adminCommandHandler.handleInitDataCommand(event);
    }

    // 3. handleRegisterCommand: 관리자 권한 체크 및 대리 등록 로직 수행
    private void handleRegisterCommand(SlashCommandInteractionEvent event) {
        // deferReply(true) 상태이므로 getHook 사용

        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessage("❌ 오류: 이 명령어는 **서버 관리자**만 사용할 수 있습니다.").queue();
            return;
        }

        OptionMapping targetUserOption = event.getOption("target-user");
        OptionMapping nicknameOption = event.getOption("lol-nickname");

        if (targetUserOption == null || nicknameOption == null) {
            event.getHook().sendMessage("❌ 오류: 대상 유저와 롤 닉네임 옵션을 모두 입력해 주세요.").queue();
            return;
        }

        String fullNickname = nicknameOption.getAsString();
        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);

        if (!matcher.matches()) {
            event.getHook().sendMessage("❌ 오류: 롤 닉네임을 **'게임이름#태그'** 형식으로 정확히 입력해 주세요. (예: Faker#KR1)").queue();
            return;
        }

        String gameName = matcher.group(1);
        String tagLine = matcher.group(2);

        Long targetDiscordUserId = targetUserOption.getAsUser().getIdLong();
        Long discordServerId = event.getGuild().getIdLong();

        try {
            String resultMessage = userService.registerLolNickname(targetDiscordUserId, gameName, tagLine, discordServerId);
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("관리자 롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage(e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }


    // --- 4. onGuildReady (명령어 등록) ---
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();

        commandDataList.add(
                Commands.slash("register", "관리자 전용: 특정 유저의 롤 닉네임(Riot ID)을 연결합니다.")
                        .addOption(OptionType.USER, "target-user", "롤 계정을 연결할 디스코드 유저를 @멘션하세요.", true)
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
        );

        OptionData winnerTeamOption = new OptionData(
                OptionType.STRING,
                "winner-team",
                "승리팀을 선택하세요.",
                true
        )
                .addChoice("BLUE 팀 승리", "BLUE")
                .addChoice("RED 팀 승리", "RED");

        commandDataList.add(
                Commands.slash("match-upload", "경기 결과 이미지로 기록을 등록합니다.")
                        .addOptions(winnerTeamOption)
                        .addOption(OptionType.ATTACHMENT, "result-image", "경기 결과 스크린샷 이미지", true)
        );

        commandDataList.add(
                Commands.slash("my-info", "내 정보를 보여줍니다")
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
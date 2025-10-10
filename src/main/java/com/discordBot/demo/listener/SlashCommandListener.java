package com.discordBot.demo.listener;

import com.discordBot.demo.service.UserService;
import com.discordBot.demo.discord.handler.MatchImageHandler;
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
import net.dv8tion.jda.api.interactions.commands.build.OptionData; // ⭐ OptionData 임포트 추가
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

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

    // --- 1. 슬래시 명령어 라우팅 ---
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        switch (event.getName()){
            case "register": // '/register'로 라우팅
                handleRegisterCommand(event);
                break;

            case "match-upload":
                imageHandler.handleMatchUploadCommand(event);
                break;

            case "my-info":
                event.reply("**[내 정보] 기능은 아직 구현되지 않았습니다.**").setEphemeral(true).queue();
                break;

            case "rank-check":
                event.reply("**[랭킹 확인] 기능은 아직 구현되지 않았습니다.**").setEphemeral(true).queue();
                break;

            default:
                event.reply("알 수 없는 커맨드입니다.").setEphemeral(true).queue();
                break;
        }
    }

    // --- 2. 버튼 상호작용 라우팅 ---
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        // MatchImageHandler의 버튼 ID 접두사를 확인하여 위임
        if (componentId.startsWith(MatchImageHandler.BUTTON_ID_CONFIRM) ||
                componentId.startsWith(MatchImageHandler.BUTTON_ID_CANCEL)) {

            // 버튼 클릭 시 로딩 상태를 표시
            event.deferEdit().queue();
            imageHandler.handleFinalConfirmation(event);
        }
    }

    // 3. handleRegisterCommand: 관리자 권한 체크 및 대리 등록 로직 수행
    private void handleRegisterCommand(SlashCommandInteractionEvent event) {

        // 1. 관리자 권한 확인 (가장 중요)
        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ 오류: 이 명령어는 **서버 관리자**만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        // 2. 옵션 추출
        OptionMapping targetUserOption = event.getOption("target-user");
        OptionMapping nicknameOption = event.getOption("lol-nickname");

        if (targetUserOption == null || nicknameOption == null) {
            event.reply("❌ 오류: 대상 유저와 롤 닉네임 옵션을 모두 입력해 주세요.").setEphemeral(true).queue();
            return;
        }

        // 3. Riot ID 형식 검증
        String fullNickname = nicknameOption.getAsString();
        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);

        if (!matcher.matches()) {
            event.reply("❌ 오류: 롤 닉네임을 **'게임이름#태그'** 형식으로 정확히 입력해 주세요. (예: Faker#KR1)")
                    .setEphemeral(true).queue();
            return;
        }

        String gameName = matcher.group(1);
        String tagLine = matcher.group(2);
        // 대상 유저의 ID를 추출 (Discord User 객체에서 ID를 가져옴)
        Long targetDiscordUserId = targetUserOption.getAsUser().getIdLong();

        event.deferReply(true).queue();

        // 4. 서비스 호출
        try {
            // UserService의 관리자 전용 메서드 호출
            String resultMessage = userService.registerLolNickname(targetDiscordUserId, gameName, tagLine);
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("관리자 롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            // 사용자 정의 예외 메시지(UserService에서 던진)를 그대로 전달
            event.getHook().sendMessage(e.getMessage().startsWith("❌ 오류:") ? e.getMessage() : "❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }


    // --- 4. onGuildReady (명령어 등록) ---
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();

        // 1. '/register' 명령 이름만 사용하며 관리자 전용 옵션을 유지
        commandDataList.add(
                Commands.slash("register", "관리자 전용: 특정 유저의 롤 닉네임(Riot ID)을 연결합니다.")
                        .addOption(OptionType.USER, "target-user", "롤 계정을 연결할 디스코드 유저를 @멘션하세요.", true)
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
        );

        // 2. /match-upload 명령어 등록 (승리팀 옵션 수정)

        // ⭐ OptionData를 사용하여 Choice를 옵션에 직접 추가합니다.
        OptionData winnerTeamOption = new OptionData(
                OptionType.STRING,
                "winner-team",
                "승리팀을 선택하세요.",
                true
        )
                .addChoice("RED 팀 승리", "RED")
                .addChoice("BLUE 팀 승리", "BLUE");

        commandDataList.add(
                Commands.slash("match-upload", "경기 결과 이미지로 기록을 등록합니다.")
                        // OptionData를 addOptions로 추가합니다.
                        .addOptions(winnerTeamOption)
                        .addOption(OptionType.ATTACHMENT, "result-image", "경기 결과 스크린샷 이미지", true)
        );

        // 3. 나머지 명령어 등록
        commandDataList.add(
                Commands.slash("my-info", "내 정보를 보여줍니다")
        );
        commandDataList.add(
                Commands.slash("rank-check", "내전 랭킹을 확인합니다")
        );

        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}

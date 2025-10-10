package com.discordBot.demo.listener;

import com.discordBot.demo.service.UserService;
import com.discordBot.demo.discord.handler.MatchImageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
    private final MatchImageHandler imageHandler; // ⭐ MatchImageHandler 주입

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

    // --- 1. 슬래시 명령어 라우팅 ---
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        switch (event.getName()){
            case "register":
                handleRegisterCommand(event);
                break;

            case "match-upload": // ⭐ /match-upload 명령어 라우팅
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

            // 버튼 클릭 시 로딩 상태를 표시 (첫 번째 응답)
            event.deferEdit().queue();
            imageHandler.handleFinalConfirmation(event);
        }
        // ... (다른 버튼 이벤트 처리 로직은 여기에 추가) ...
    }

    // --- 3. handleRegisterCommand (기존 유지) ---
    private void handleRegisterCommand(SlashCommandInteractionEvent event) {

        OptionMapping nicknameOption = event.getOption("lol-nickname");
        if (nicknameOption == null) {
            event.reply("❌ 오류: 롤 닉네임 옵션을 입력해 주세요.").setEphemeral(true).queue();
            return;
        }

        String fullNickname = nicknameOption.getAsString();

        Matcher matcher = RIOT_ID_PATTERN.matcher(fullNickname);

        if (!matcher.matches()) {
            event.reply("❌ 오류: 롤 닉네임을 **'게임이름#태그'** 형식으로 정확히 입력해 주세요. (예: Faker#KR1)")
                    .setEphemeral(true).queue();
            return;
        }

        String gameName = matcher.group(1);
        String tagLine = matcher.group(2);
        Long discordUserId = event.getUser().getIdLong();

        event.deferReply(true).queue();

        try {
            String resultMessage = userService.registerLolNickname(discordUserId, gameName, tagLine);
            event.getHook().sendMessage(resultMessage).queue();

        } catch (Exception e) {
            log.error("롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }

    // --- 4. onGuildReady (명령어 등록) ---
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();

        commandDataList.add(
                Commands.slash("register", "롤 닉네임(Riot ID)을 디스코드 계정에 연결합니다.")
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
        );

        // ⭐ /match-upload 명령어 등록 (STRING + ATTACHMENT)
        commandDataList.add(
                Commands.slash("match-upload", "경기 결과 이미지로 기록을 등록합니다.")
                        .addOption(OptionType.STRING, "winner-team", "승리팀을 입력하세요 (RED/BLUE)", true)
                        .addOption(OptionType.ATTACHMENT, "result-image", "경기 결과 스크린샷 이미지", true)
        );

        commandDataList.add(
                Commands.slash("my-info", "내 정보를 보여줍니다")
        );
        commandDataList.add(
                Commands.slash("rank-check", "내전 랭킹을 확인합니다")
        );

        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}
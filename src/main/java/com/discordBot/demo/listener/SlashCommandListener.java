package com.discordBot.demo.listener;

import com.discordBot.demo.discord.handler.MatchInteractionHandler;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
    private final MatchInteractionHandler interactionHandler;

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        switch (event.getName()){
            case "register":
                handleRegisterCommand(event);
                break;

            case "match-register":
                interactionHandler.handleMatchRegisterCommand(event);
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

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        // 모달 이벤트가 발생하면 핸들러에게 위임하여 처리
        interactionHandler.handleModalSubmission(event);
    }

    private void handleMatchRegisterCommand(SlashCommandInteractionEvent event) {

        // 1. 초기 옵션 (승리팀) 가져오기
        OptionMapping winnerTeamOption = event.getOption("winner-team");
        if (winnerTeamOption == null) {
            event.reply("❌ 오류: 승리팀 옵션을 찾을 수 없습니다. (내부 오류)").setEphemeral(true).queue();
            return;
        }
        String winnerTeam = winnerTeamOption.getAsString();

        // 2. 기본 유효성 검사
        if (!winnerTeam.equalsIgnoreCase("RED") && !winnerTeam.equalsIgnoreCase("BLUE")) {
            event.reply("❌ 오류: 승리팀은 RED 또는 BLUE로 정확히 입력해 주세요.").setEphemeral(true).queue();
            return;
        }

        // 3. 인터랙티브 등록 시작 (첫 번째 Modal 띄우기)

        // 이 단계는 복잡한 Modal 로직을 필요로 하므로, 여기서는 시작 메시지만 출력합니다.
        // 실제 구현에서는 이 시점에서 Modal을 빌드하고 event.replyModal(modal).queue()를 호출해야 합니다.

        event.reply("✅ [" + winnerTeam.toUpperCase() + " 팀 승리] 경기 기록 등록 절차를 시작합니다. 첫 번째 선수 정보를 입력해주세요.")
                .setEphemeral(true)
                .queue();

        // TODO: 첫 번째 PlayerStats Modal을 띄우는 로직 구현
    }

    private void handleRegisterCommand(SlashCommandInteractionEvent event) {

        String fullNickname = event.getOption("lol-nickname").getAsString();

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

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();

        commandDataList.add(
                Commands.slash("register", "롤 닉네임(Riot ID)을 디스코드 계정에 연결합니다.")
                        .addOption(OptionType.STRING, "lol-nickname", "롤 닉네임과 태그를 '이름#태그' 형식으로 입력하세요 (예: Hide On Bush#KR1)", true)
        );

        commandDataList.add(
                Commands.slash("my-info", "내 정보를 보여줍니다")
        );
        commandDataList.add(
                Commands.slash("rank-check", "내전 랭킹을 확인합니다")
        );
        commandDataList.add(
                Commands.slash("match-register", "내전 경기 기록 등록을 시작하고 승리팀을 결정합니다.")
                        .addOption(OptionType.STRING, "winner-team", "승리팀을 입력하세요 (RED/BLUE)", true)
        );

        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}
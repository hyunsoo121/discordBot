package com.discordBot.demo.listener;

import com.discordBot.demo.service.UserService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@RequiredArgsConstructor
public class SlashCommandListener extends ListenerAdapter {

    private final UserService userService;

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        switch (event.getName()){
            case "register":
                handleRegisterCommand(event);
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

        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}
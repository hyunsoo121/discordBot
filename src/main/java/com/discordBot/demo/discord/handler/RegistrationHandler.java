package com.discordBot.demo.discord.handler;

import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationHandler {

    private final UserService userService;
    private final LolAccountRepository lolAccountRepository;

    // 버튼 ID Prefix
    public static final String BUTTON_ID_REGISTER_SELECT = "reg-select-";

    private static final Pattern RIOT_ID_PATTERN = Pattern.compile("^(.+)#(.+)$");


    /**
     * /register 명령어를 처리하여 계정 중복을 검사하고 필요한 경우 선택 버튼을 제시합니다.
     */
    public void handleRegisterCommand(SlashCommandInteractionEvent event) {

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

        // 1. GameName이 일치하는 모든 계정 조회
        List<LolAccount> existingAccounts = lolAccountRepository.findByGameName(gameName);

        if (existingAccounts.isEmpty()) {
            // 2-A. 중복 없음: 표준 등록 로직 실행
            performStandardRegistration(event, discordUserId, gameName, tagLine);
        } else {
            // 2-B. 중복 있음: 선택 로직 실행
            sendAccountSelection(event, existingAccounts, discordUserId, fullNickname);
        }
    }

    /**
     * 중복 계정 목록을 사용자에게 버튼으로 제시합니다.
     */
    private void sendAccountSelection(SlashCommandInteractionEvent event, List<LolAccount> accounts, Long discordUserId, String fullNickname) {

        // 5개를 초과하지 않도록 버튼 생성
        List<Button> buttons = accounts.stream()
                .limit(5)
                .map(account -> {
                    // 버튼 ID: reg-select-{discordUserId}-{accountId}
                    String buttonId = BUTTON_ID_REGISTER_SELECT + discordUserId + "-" + account.getId();
                    String buttonLabel = account.getGameName() + "#" + account.getTagLine();
                    return Button.primary(buttonId, buttonLabel);
                })
                .collect(Collectors.toList());

        // 새로운 계정으로 등록하는 옵션 추가
        buttons.add(Button.secondary(BUTTON_ID_REGISTER_SELECT + discordUserId + "-NEW", "새로운 계정으로 등록"));

        event.reply("⚠️ **중복 롤 계정 발견!** '" + fullNickname + "'님, 이미 등록된 동일한 이름을 가진 계정들이 있습니다. 아래 목록 중 **당신의 태그**를 선택하거나, **새로운 계정**으로 등록해주세요.")
                .setEphemeral(true)
                .addActionRow(buttons)
                .queue();
    }

    /**
     * 표준 등록 로직 (중복 없는 경우 또는 신규 등록 선택 시)
     */
    private void performStandardRegistration(SlashCommandInteractionEvent event, Long discordUserId, String gameName, String tagLine) {
        event.deferReply(true).queue();
        try {
            // UserService의 기본 등록 메서드 사용
            String resultMessage = userService.registerLolNickname(discordUserId, gameName, tagLine);
            event.getHook().sendMessage(resultMessage).queue();
        } catch (Exception e) {
            log.error("롤 닉네임 등록 중 에러 발생: {}", e.getMessage(), e);
            event.getHook().sendMessage("❌ 서버 처리 중 예기치 않은 오류가 발생했습니다.").queue();
        }
    }


    /**
     * 사용자가 버튼으로 계정을 선택했을 때 최종 연결 로직을 처리합니다.
     */
    public void handleAccountSelection(ButtonInteractionEvent event) {

        event.deferEdit().queue(); // 상호작용 로딩 상태 표시

        String[] parts = event.getComponentId().split("-"); // reg-select-{discordUserId}-{accountId}

        Long discordUserId = Long.parseLong(parts[2]);
        String accountAction = parts[3];

        // 1. 현재 버튼을 누른 사용자가 명령을 시작한 사람인지 확인
        // ⭐ 수정: primitive long 값으로 비교
        if (event.getUser().getIdLong() != discordUserId.longValue()) {
            event.getHook().sendMessage("❌ 오류: 본인이 시작한 등록 세션만 완료할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        // 2. 선택된 계정 ID 또는 신규 등록 처리
        try {
            String resultMessage;
            if ("NEW".equals(accountAction)) {
                // 신규 등록 선택 시, 사용자에게 태그를 다시 입력하도록 안내 (원래의 fullNickname을 알 수 없음)
                event.getHook().editOriginal("✅ **[새로운 계정으로 등록]**을 선택했습니다. \n다시 `/register` 명령어를 사용하시고, **반드시 정확한 태그**를 포함하여 입력해주세요.")
                        .setComponents()
                        .queue();
                return;
            } else {
                // 기존 계정 선택 시, 해당 계정 ID와 연결
                Long lolAccountId = Long.parseLong(accountAction);

                // UserService에 기존 계정과 Discord 계정을 연결하는 새로운 메서드 추가 필요
                resultMessage = userService.linkExistingAccount(discordUserId, lolAccountId);
            }

            event.getHook().editOriginal(resultMessage).setComponents().queue();

        } catch (Exception e) {
            log.error("기존 롤 계정 연결 중 오류 발생: {}", e.getMessage(), e);
            event.getHook().editOriginal("❌ 서버 처리 중 오류가 발생했습니다.").setComponents().queue();
        }
    }

    // Note: linkExistingAccount 메서드를 UserService에 추가해야 함
}

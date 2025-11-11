package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.GuildServer; // GuildServer 엔티티 임포트 필요
import com.discordBot.demo.domain.entity.Line;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LineRepository;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.RiotApiService;
import com.discordBot.demo.service.ServerManagementService;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LolAccountRepository lolAccountRepository;
    private final RiotApiService riotApiService;
    private final ServerManagementService serverManagementService;
    private final LineRepository lineRepository; // ⭐ LineRepository 주입

    // UserService 인터페이스 메서드 서명도 아래와 같이 변경되어야 합니다.
    @Override
    @Transactional
    public String registerLolNickname(Long targetDiscordUserId, String gameName, String tagLine, Long discordServerId, String preferredLineNamesCsv) {

        // 1. Riot API를 통한 계정 정보 확인 및 Puuid 가져오기 (기존 로직 유지)
        RiotAccountDto riotAccount = riotApiService.verifyNickname(gameName, tagLine)
                .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 해당 롤 계정(Riot ID)을 찾을 수 없습니다. 이름과 태그라인을 정확히 입력해 주세요."));

        // ⭐ 2. Line 엔티티 조회 및 복수 라인 처리
        Set<Line> preferredLines = new HashSet<>();
        String displayLines = "없음";

        if (StringUtils.hasText(preferredLineNamesCsv)) {
            // 쉼표로 분리, 공백 제거, 대문자로 변환 후 List<String>으로 변환
            List<String> lineNames = Arrays.stream(preferredLineNamesCsv.toUpperCase().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // 각 라인 이름으로 Line 엔티티 조회 및 Set에 추가
            for (String lineName : lineNames) {
                Line line = lineRepository.findByName(lineName)
                        .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 선호 라인 [" + lineName + "] 정보를 찾을 수 없습니다. (유효 라인: TOP, JUNGLE, MID, ADC, UTILITY)"));
                preferredLines.add(line);
            }

            // 등록 완료 메시지에 표시할 라인 이름 목록 생성 (Line 엔티티의 DisplayName 필드가 있다고 가정)
            displayLines = preferredLines.stream()
                    .map(Line::getName) // Line 엔티티의 이름 필드를 사용
                    .collect(Collectors.joining(", "));
        }


        // 3. User, GuildServer 엔티티 조회 및 중복 확인 (기존 로직 유지)
        User targetUser = userRepository.findByDiscordUserId(targetDiscordUserId)
                .orElseGet(() -> {
                    log.info("새로운 대상 디스코드 유저 등록: ID={}", targetDiscordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(targetDiscordUserId);
                    return userRepository.save(newUser);
                });

        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(discordServerId);

        // 해당 서버에 이미 등록된 계정인지 확인 (PK는 아니지만 비즈니스 유효성 검증)
        Optional<LolAccount> existingAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                gameName, tagLine, discordServerId
        );

        if (existingAccountOpt.isPresent()) {
            throw new IllegalArgumentException("❌ 오류: 롤 계정 **" + gameName + "#" + tagLine + "**는 이미 이 서버에 등록되어 있습니다.");
        }


        // 4. 신규 롤 계정 등록 및 업데이트
        LolAccount accountToSave = new LolAccount();

        accountToSave.setUser(targetUser);
        accountToSave.setGuildServer(guildServer);
        accountToSave.setGameName(riotAccount.getGameName()); // 대소문자 구분을 위해 Riot API 결과 사용
        accountToSave.setTagLine(riotAccount.getTagLine());
        accountToSave.setPuuid(riotAccount.getPuuid());

        accountToSave.setPreferredLines(preferredLines); // ⭐ 복수 선호 라인 설정

        lolAccountRepository.save(accountToSave);

        return "🎉 관리자 등록 완료: 롤 계정 **" + accountToSave.getFullAccountName() +
                "**가 연결되었습니다! (선호 라인: " + displayLines + ")";
    }

    // --- 유틸리티 메서드: 기존 LolAccount에 Discord User 연결 ---
    @Override
    @Transactional
    public String linkExistingAccount(Long targetDiscordUserId, Long lolAccountId) {

        // 1. 대상 LolAccount 조회
        LolAccount lolAccount = lolAccountRepository.findById(lolAccountId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 선택한 롤 계정 ID(" + lolAccountId + ")를 찾을 수 없습니다."));

        // 2. 대상 User 조회 또는 생성
        User targetUser = userRepository.findByDiscordUserId(targetDiscordUserId)
                .orElseGet(() -> {
                    log.info("새로운 대상 디스코드 유저 등록: ID={}", targetDiscordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(targetDiscordUserId);
                    return userRepository.save(newUser);
                });

        // 3. 소유권 확인 및 업데이트
        User existingOwner = lolAccount.getUser();

        if (existingOwner != null && !existingOwner.equals(targetUser)) {
            // 이미 다른 유저가 소유한 경우
            throw new IllegalArgumentException("❌ 오류: 롤 계정 **" + lolAccount.getFullAccountName() +
                    "**는 이미 다른 사용자에게 연결되어 있어 소유권을 변경할 수 없습니다.");
        }

        // 연결 업데이트 (Puuid는 이미 해당 계정에 있을 것으로 가정)
        lolAccount.setUser(targetUser);
        lolAccountRepository.save(lolAccount);

        return "✅ 롤 계정 **" + lolAccount.getFullAccountName() +
                "**가 대상 유저(" + targetDiscordUserId + ")에게 성공적으로 연결되었습니다!";
    }
}

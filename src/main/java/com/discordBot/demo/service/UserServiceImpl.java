package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LolAccountRepository lolAccountRepository;

    @Override
    @Transactional
    public String registerLolNickname(Long discordUserId, String gameName, String tagLine) {
        // 1. 디스코드 사용자 찾기 또는 생성
        User user = userRepository.findByDiscordUserId(discordUserId)
                .orElseGet(() -> {
                    log.info("새로운 디스코드 유저 등록: ID={}", discordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(discordUserId);
                    return userRepository.save(newUser);
                });

        // 2. 롤 계정 중복 확인 (이미 해당 태그로 등록된 계정이 있는지 확인)
        Optional<LolAccount> existingAccountOpt = lolAccountRepository.findByGameNameAndTagLine(gameName, tagLine);

        if (existingAccountOpt.isPresent()) {
            LolAccount existingAccount = existingAccountOpt.get();
            // 2-1. 이미 다른 유저에게 연결된 경우
            if (existingAccount.getUser() != null && !existingAccount.getUser().equals(user)) {
                throw new IllegalArgumentException("❌ 오류: 해당 롤 계정(" + existingAccount.getFullAccountName() + ")은 이미 다른 디스코드 사용자에게 등록되어 있습니다.");
            }
            // 2-2. 이미 나에게 연결된 경우
            if (existingAccount.getUser() != null && existingAccount.getUser().equals(user)) {
                return "✅ 알림: 롤 계정 **" + existingAccount.getFullAccountName() + "**는 이미 당신에게 연결되어 있습니다.";
            }

            // 2-3. DB에 계정은 있으나 연결된 유저가 없는 경우 -> 연결 시도
            existingAccount.setUser(user);
            lolAccountRepository.save(existingAccount);
            return "✅ 롤 계정 **" + existingAccount.getFullAccountName() + "**가 당신의 디스코드 계정에 연결되었습니다! (기존 DB 기록 활용)";
        }

        // 3. 신규 롤 계정 등록
        LolAccount newAccount = new LolAccount();
        newAccount.setGameName(gameName);
        newAccount.setTagLine(tagLine);
        newAccount.setUser(user);

        lolAccountRepository.save(newAccount);

        return "🎉 롤 계정 **" + newAccount.getFullAccountName() + "**가 성공적으로 등록되었습니다!";
    }

    @Override
    public List<LolAccount> findAccountsByGameName(String gameName) {
        return lolAccountRepository.findByGameName(gameName);
    }

    // ⭐ 신규 메서드 구현: 기존 LolAccount에 Discord User 연결
    @Override
    @Transactional
    public String linkExistingAccount(Long discordUserId, Long lolAccountId) {
        // 1. 대상 LolAccount 조회
        LolAccount lolAccount = lolAccountRepository.findById(lolAccountId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 선택한 롤 계정 ID(" + lolAccountId + ")를 찾을 수 없습니다."));

        // 2. 대상 User 조회 또는 생성
        User user = userRepository.findByDiscordUserId(discordUserId)
                .orElseGet(() -> {
                    log.info("새로운 디스코드 유저 등록: ID={}", discordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(discordUserId);
                    return userRepository.save(newUser);
                });

        // 3. 소유권 확인 및 업데이트
        if (lolAccount.getUser() != null && !lolAccount.getUser().equals(user)) {
            throw new IllegalArgumentException("❌ 오류: 롤 계정 **" + lolAccount.getFullAccountName() + "**는 이미 다른 사용자에게 연결되어 있습니다.");
        }

        // 연결 업데이트
        lolAccount.setUser(user);
        lolAccountRepository.save(lolAccount);

        return "✅ 롤 계정 **" + lolAccount.getFullAccountName() + "**가 당신의 디스코드 계정에 성공적으로 연결되었습니다!";
    }
}

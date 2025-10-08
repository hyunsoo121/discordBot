package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.LolNickname;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.dto.RiotAccountDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service // 🚩 Spring Bean으로 등록
@RequiredArgsConstructor
@Transactional(readOnly = true) // 🚩 기본 트랜잭션 설정을 읽기 전용으로 설정
public class UserServiceImpl implements UserService { // 🚩 인터페이스 구현

    private final UserRepository userRepository;
    private final LolNicknameRepository lolNicknameRepository;

    // 💡 인터페이스에 의존 (DIP)
    private final RiotApiService riotApiService;

    @Override
    @Transactional // 🚩 쓰기 작업이므로 트랜잭션 활성화
    public String registerLolNickname(Long discordUserId, String gameName, String tagLine) {

        String fullNickname = gameName + "#" + tagLine;

        // 1. Riot API 검증 (riotAccountService 사용)
        Optional<RiotAccountDto> riotAccountOpt = riotAccountService.verifyNickname(gameName, tagLine);

        if (riotAccountOpt.isEmpty()) {
            return "❌ 오류: 해당 롤 닉네임(" + fullNickname + ")은 존재하지 않습니다.";
        }

        // 2. Discord User 조회 또는 생성
        User user = userRepository.findByDiscordUserId(discordUserId)
                .orElseGet(() -> {
                    // (Optional) 디스코드 봇에서 유저 정보를 가져와서 User 엔티티 생성
                    User newUser = new User();
                    newUser.setDiscordUserId(discordUserId);
                    // Riot API 응답에서 받은 gameName 사용을 고려할 수 있습니다.
                    newUser.setUsername("DiscordUser#" + discordUserId);
                    return userRepository.save(newUser);
                });

        // 3. 닉네임 등록 (중복 방지 로직)
        if (lolNicknameRepository.existsByNickname(fullNickname)) {
            return "⚠️ 경고: 해당 롤 닉네임은 이미 다른 디스코드 계정에 등록되어 있습니다.";
        }

        LolNickname newNickname = new LolNickname();
        newNickname.setUser(user);
        newNickname.setNickname(fullNickname);

        // 첫 닉네임이면 대표 닉네임으로 설정 (user.getNicknames().isEmpty()는
        // User 엔티티의 @OneToMany FetchType에 따라 지연 로딩 문제가 있을 수 있으므로
        // 쿼리로 대체하거나, isMain 필드를 업데이트하는 로직을 추가하는 것이 더 안정적입니다.)
        newNickname.setIsMain(lolNicknameRepository.countByUser(user) == 0);

        lolNicknameRepository.save(newNickname);

        return "✅ 성공: 롤 닉네임 '" + newNickname.getNickname() + "'이(가) 성공적으로 등록되었습니다.";
    }
}
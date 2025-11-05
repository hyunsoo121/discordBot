package com.discordBot.demo.config;

import com.discordBot.demo.service.ChampionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

/**
 * 챔피언 데이터의 초기 로딩 및 관리(버전 업데이트) 설정을 담당하는 구성 클래스.
 */
@Configuration
@RequiredArgsConstructor
public class ChampionConfig {

    private final ChampionService championService;

    @Bean
    public ApplicationRunner runChampionUpdateOnStartup() {
        return args -> {
            // ApplicationContext 로드 완료 후 챔피언 업데이트 서비스 호출
            championService.updateChampionDataIfNecessary();
        };
    }
}

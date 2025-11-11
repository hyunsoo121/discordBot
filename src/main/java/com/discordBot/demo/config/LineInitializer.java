package com.discordBot.demo.config;

import com.discordBot.demo.domain.entity.Line;
import com.discordBot.demo.domain.repository.LineRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class LineInitializer {

    @Bean
    public ApplicationRunner initLines(LineRepository lineRepository) {
        return args -> {
            if (lineRepository.count() == 0) {

                List<Line> fixedLines = Arrays.asList(
                        createLine("TOP", "탑"),
                        createLine("JUNGLE", "정글"),
                        createLine("MID", "미드"),
                        createLine("ADC", "원딜"),
                        createLine("SUPPORT", "서포터")
                );

                lineRepository.saveAll(fixedLines);
                System.out.println("✅ [INIT] 5개 고정 포지션 데이터가 성공적으로 삽입되었습니다.");
            }
        };
    }

    private Line createLine(String name, String displayName) {
        Line line = new Line();
        line.setName(name);
        line.setDisplayName(displayName);
        return line;
    }
}
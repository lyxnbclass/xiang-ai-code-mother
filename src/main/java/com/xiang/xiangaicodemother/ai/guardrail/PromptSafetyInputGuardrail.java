package com.xiang.xiangaicodemother.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** 在 Prompt 进入模型前阻止超长内容和常见指令注入。 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    public static final int MAX_PROMPT_LENGTH = 1000;

    private static final List<String> BLOCKED_PHRASES = List.of(
            "忽略之前的指令", "忽略以上指令", "绕过系统", "越狱",
            "ignore previous instructions", "ignore above", "jailbreak", "bypass safeguards"
    );

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        Optional<String> violation = findViolation(input);
        return violation.map(this::fatal).orElseGet(this::success);
    }

    public static Optional<String> findViolation(String input) {
        if (input == null || input.isBlank()) {
            return Optional.of("输入内容不能为空");
        }
        if (input.length() > MAX_PROMPT_LENGTH) {
            return Optional.of("输入内容过长，不能超过 1000 字");
        }
        String normalized = input.toLowerCase(Locale.ROOT);
        if (BLOCKED_PHRASES.stream().anyMatch(normalized::contains)) {
            return Optional.of("输入包含不安全的指令，请修改后重试");
        }
        if (INJECTION_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).find())) {
            return Optional.of("检测到指令注入内容，请修改后重试");
        }
        return Optional.empty();
    }
}

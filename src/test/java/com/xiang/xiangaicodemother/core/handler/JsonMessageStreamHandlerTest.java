package com.xiang.xiangaicodemother.core.handler;

import cn.hutool.json.JSONUtil;
import com.xiang.xiangaicodemother.ai.model.message.AiResponseMessage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMessageStreamHandlerTest {

    private final JsonMessageStreamHandler handler = new JsonMessageStreamHandler();

    @Test
    void collectsAiTextAndCompletedFileToolOutput() {
        StringBuilder history = new StringBuilder();
        HashSet<String> toolIds = new HashSet<>();

        String text = handler.handleChunk(
                JSONUtil.toJsonStr(new AiResponseMessage("生成计划")), history, toolIds);
        String request = handler.handleChunk(
                "{\"type\":\"tool_request\",\"id\":\"call-1\",\"name\":\"writeFile\"}",
                history, toolIds);
        String executed = handler.handleChunk(
                "{\"type\":\"tool_executed\",\"id\":\"call-1\",\"name\":\"writeFile\","
                        + "\"arguments\":\"{\\\"relativeFilePath\\\":\\\"src/App.vue\\\","
                        + "\\\"content\\\":\\\"<template>ok</template>\\\"}\","
                        + "\"result\":\"文件写入成功\"}", history, toolIds);

        assertEquals("生成计划", text);
        assertTrue(request.contains("选择工具"));
        assertTrue(executed.contains("src/App.vue"));
        assertTrue(executed.contains("<template>ok</template>"));
        assertTrue(history.toString().contains("生成计划"));
        assertTrue(history.toString().contains("工具调用"));
    }

    @Test
    void ignoresDuplicateRequestsAndUnknownMessages() {
        StringBuilder history = new StringBuilder();
        HashSet<String> toolIds = new HashSet<>();
        String request = "{\"type\":\"tool_request\",\"id\":\"call-1\"}";

        assertTrue(handler.handleChunk(request, history, toolIds).contains("选择工具"));
        assertEquals("", handler.handleChunk(request, history, toolIds));
        assertEquals("", handler.handleChunk("{\"type\":\"unknown\"}", history, toolIds));
    }
}

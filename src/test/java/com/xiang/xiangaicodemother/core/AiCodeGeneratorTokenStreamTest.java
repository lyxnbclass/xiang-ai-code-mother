package com.xiang.xiangaicodemother.core;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import com.xiang.xiangaicodemother.core.builder.VueProjectBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCodeGeneratorTokenStreamTest {

    @Test
    void adaptsAiAndToolCallbacksToStructuredFluxMessages() {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        FakeTokenStream tokenStream = new FakeTokenStream();

        List<String> chunks = facade.processTokenStream(tokenStream).collectList().block();

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).contains("ai_response"));
        assertTrue(chunks.get(1).contains("tool_request"));
        assertTrue(chunks.get(2).contains("tool_executed"));
    }

    @Test
    void buildsVueProjectBeforeCompletingStream() {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        VueProjectBuilder builder = mock(VueProjectBuilder.class);
        when(builder.buildProject(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(facade, "vueProjectBuilder", builder);

        facade.processTokenStream(new FakeTokenStream(), 42L).collectList().block();

        verify(builder).buildProject(endsWith("vue_project_42"));
    }

    private static class FakeTokenStream implements TokenStream {
        private Consumer<String> partialResponseHandler;
        private Consumer<ToolExecution> toolExecutionHandler;
        private Consumer<ChatResponse> completeHandler;

        @Override
        public TokenStream onPartialResponse(Consumer<String> handler) {
            partialResponseHandler = handler;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> handler) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<ToolExecution> handler) {
            toolExecutionHandler = handler;
            return this;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
            completeHandler = handler;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> handler) {
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            partialResponseHandler.accept("开始生成");
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .id("call-1")
                    .name("writeFile")
                    .arguments("{\"relativeFilePath\":\"index.html\",\"content\":\"ok\"}")
                    .build();
            toolExecutionHandler.accept(ToolExecution.builder()
                    .request(request)
                    .result("文件写入成功")
                    .build());
            completeHandler.accept(null);
        }
    }
}

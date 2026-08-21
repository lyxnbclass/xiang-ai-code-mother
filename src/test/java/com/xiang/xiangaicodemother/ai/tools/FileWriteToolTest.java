package com.xiang.xiangaicodemother.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteToolTest {

    @TempDir
    Path tempDir;

    @Test
    void writesUtf8FileInsideApplicationProject() throws Exception {
        FileWriteTool tool = new FileWriteTool(tempDir);

        String result = tool.writeFile("src/pages/Home.vue", "<template>你好</template>", 7L);

        Path target = tempDir.resolve("vue_project_7/src/pages/Home.vue");
        assertTrue(result.startsWith("文件写入成功"));
        assertEquals("<template>你好</template>", Files.readString(target));
    }

    @Test
    void rejectsTraversalAndAbsolutePaths() {
        FileWriteTool tool = new FileWriteTool(tempDir);

        assertTrue(tool.writeFile("../escape.txt", "bad", 7L).startsWith("文件写入失败"));
        assertTrue(tool.writeFile(tempDir.resolve("absolute.txt").toString(), "bad", 7L)
                .startsWith("文件写入失败"));
        assertFalse(Files.exists(tempDir.resolve("escape.txt")));
        assertFalse(Files.exists(tempDir.resolve("absolute.txt")));
    }

    @Test
    void rejectsInvalidApplicationAndOversizedContent() {
        FileWriteTool tool = new FileWriteTool(tempDir);

        assertTrue(tool.writeFile("index.html", "ok", 0L).startsWith("文件写入失败"));
        assertTrue(tool.writeFile("large.txt", "x".repeat(1_048_577), 7L)
                .startsWith("文件写入失败"));
        assertFalse(Files.exists(tempDir.resolve("vue_project_7/large.txt")));
    }
}

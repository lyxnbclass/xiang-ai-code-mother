package com.xiang.xiangaicodemother.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectFileToolsTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAndUniquelyModifiesProjectFiles() throws Exception {
        FileWriteTool writeTool = new FileWriteTool(tempDir);
        FileReadTool readTool = new FileReadTool(tempDir);
        FileModifyTool modifyTool = new FileModifyTool(tempDir);

        assertTrue(writeTool.writeFile("src/App.vue", "<h1>旧标题</h1>", 9L)
                .startsWith("文件写入成功"));
        assertTrue(readTool.readFile("src/App.vue", 9L).contains("旧标题"));
        assertTrue(modifyTool.modifyFile("src/App.vue", "旧标题", "新标题", 9L)
                .startsWith("文件修改成功"));
        assertTrue(Files.readString(tempDir.resolve("vue_project_9/src/App.vue")).contains("新标题"));

        Files.writeString(tempDir.resolve("vue_project_9/src/Repeated.vue"), "相同 相同");
        assertTrue(modifyTool.modifyFile("src/Repeated.vue", "相同", "不同", 9L)
                .contains("匹配到多处"));
        assertTrue(Files.readString(tempDir.resolve("vue_project_9/src/Repeated.vue")).equals("相同 相同"));
    }

    @Test
    void listsUsefulEntriesAndFiltersGeneratedOrSecretFiles() throws Exception {
        Path projectRoot = tempDir.resolve("vue_project_12");
        Files.createDirectories(projectRoot.resolve("src/pages"));
        Files.createDirectories(projectRoot.resolve("node_modules/pkg"));
        Files.writeString(projectRoot.resolve("src/pages/Home.vue"), "home");
        Files.writeString(projectRoot.resolve("node_modules/pkg/index.js"), "ignored");
        Files.writeString(projectRoot.resolve(".env.local"), "SECRET=value");

        String result = new FileDirReadTool(tempDir).readDir("", 12L);

        assertTrue(result.contains("src/pages/"));
        assertTrue(result.contains("src/pages/Home.vue"));
        assertFalse(result.contains("node_modules"));
        assertFalse(result.contains(".env.local"));
    }

    @Test
    void deletesOnlyNonCriticalFilesInsideProject() throws Exception {
        Path projectRoot = tempDir.resolve("vue_project_15");
        Files.createDirectories(projectRoot.resolve("src/components"));
        Files.writeString(projectRoot.resolve("package.json"), "{}");
        Files.writeString(projectRoot.resolve("src/components/Unused.vue"), "unused");
        FileDeleteTool deleteTool = new FileDeleteTool(tempDir);

        assertTrue(deleteTool.deleteFile("package.json", 15L).startsWith("文件删除失败"));
        assertTrue(deleteTool.deleteFile("../outside.txt", 15L).startsWith("文件删除失败"));
        assertTrue(deleteTool.deleteFile("src/components/Unused.vue", 15L)
                .startsWith("文件删除成功"));
        assertTrue(Files.exists(projectRoot.resolve("package.json")));
        assertFalse(Files.exists(projectRoot.resolve("src/components/Unused.vue")));
    }

    @Test
    void rejectsAbsoluteAndTraversalPathsAcrossReadAndModifyTools() throws Exception {
        Path projectRoot = tempDir.resolve("vue_project_18");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("safe.txt"), "safe");

        FileReadTool readTool = new FileReadTool(tempDir);
        FileModifyTool modifyTool = new FileModifyTool(tempDir);
        String absolutePath = tempDir.resolve("outside.txt").toString();

        assertTrue(readTool.readFile("../outside.txt", 18L).startsWith("文件读取失败"));
        assertTrue(readTool.readFile(absolutePath, 18L).startsWith("文件读取失败"));
        assertTrue(modifyTool.modifyFile("../outside.txt", "x", "y", 18L)
                .startsWith("文件修改失败"));
        assertTrue(Files.readString(projectRoot.resolve("safe.txt")).equals("safe"));
    }
}

package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import com.xiang.xiangaicodemother.constant.AppConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectDownloadServiceImplTest {

    private Path projectRoot;

    private final ProjectDownloadServiceImpl service = new ProjectDownloadServiceImpl();

    @AfterEach
    void cleanup() {
        if (projectRoot != null) {
            FileUtil.del(projectRoot.toFile());
        }
    }

    @Test
    void shouldOnlyIncludeSourceFiles() throws Exception {
        projectRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR,
                "download-test-" + UUID.randomUUID()).toAbsolutePath();
        Files.createDirectories(projectRoot.resolve("src/components"));
        Files.createDirectories(projectRoot.resolve("node_modules/pkg"));
        Files.createDirectories(projectRoot.resolve("dist"));
        Files.writeString(projectRoot.resolve("src/main.js"), "console.log('ok')");
        Files.writeString(projectRoot.resolve("src/components/App.vue"), "<template />");
        Files.writeString(projectRoot.resolve("node_modules/pkg/index.js"), "ignored");
        Files.writeString(projectRoot.resolve("dist/index.html"), "ignored");
        Files.writeString(projectRoot.resolve("debug.log"), "ignored");
        Files.writeString(projectRoot.resolve(".env.local"), "SECRET=ignored");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.writeProjectZip(projectRoot, outputStream);

        Set<String> entries = readEntryNames(outputStream.toByteArray());
        assertEquals(Set.of("src/main.js", "src/components/App.vue"), entries);
    }

    private Set<String> readEntryNames(byte[] zipBytes) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}

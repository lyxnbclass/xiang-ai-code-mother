package com.xiang.xiangaicodemother.core.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VueProjectBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsProjectOutsideAllowedRootAndMissingPackageJson() throws Exception {
        VueProjectBuilder builder = new VueProjectBuilder(tempDir.resolve("allowed"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path missingPackage = Files.createDirectories(tempDir.resolve("allowed/vue_project_1"));

        assertFalse(builder.buildProject(outside.toString()));
        assertFalse(builder.buildProject(missingPackage.toString()));
    }

    @Test
    void validatesBuildOutputAndSerializesBuildsForSameProject() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("output"));
        Path project = Files.createDirectories(root.resolve("vue_project_1"));
        Files.writeString(project.resolve("package.json"), "{}");
        FakeVueProjectBuilder builder = new FakeVueProjectBuilder(root);

        CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(
                () -> builder.buildProject(project.toString()));
        CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(
                () -> builder.buildProject(project.toString()));

        assertTrue(first.join());
        assertTrue(second.join());
        assertTrue(Files.isRegularFile(project.resolve("dist/index.html")));
        assertTrue(builder.maxConcurrentBuilds.get() == 1);
    }

    private static class FakeVueProjectBuilder extends VueProjectBuilder {
        private final AtomicInteger activeBuilds = new AtomicInteger();
        private final AtomicInteger maxConcurrentBuilds = new AtomicInteger();

        FakeVueProjectBuilder(Path root) {
            super(root);
        }

        @Override
        protected boolean executeNpmInstall(Path projectDir) {
            int active = activeBuilds.incrementAndGet();
            maxConcurrentBuilds.accumulateAndGet(active, Math::max);
            try {
                Thread.sleep(50);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected boolean executeNpmBuild(Path projectDir) {
            try {
                Path dist = Files.createDirectories(projectDir.resolve("dist"));
                Files.writeString(dist.resolve("index.html"), "ok");
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                activeBuilds.decrementAndGet();
            }
        }
    }
}

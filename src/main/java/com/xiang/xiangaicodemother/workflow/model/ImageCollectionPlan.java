package com.xiang.xiangaicodemother.workflow.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** AI 生成的图片收集计划。 */
@Data
public class ImageCollectionPlan implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<ImageSearchTask> contentImageTasks;
    private List<IllustrationTask> illustrationTasks;
    private List<DiagramTask> diagramTasks;
    private List<LogoTask> logoTasks;

    public record ImageSearchTask(String query) implements Serializable {}
    public record IllustrationTask(String query) implements Serializable {}
    public record DiagramTask(String mermaidCode, String description) implements Serializable {}
    public record LogoTask(String description) implements Serializable {}
}

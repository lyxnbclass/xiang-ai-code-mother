package com.xiang.xiangaicodemother.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** AI 代码质量检查结果。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean valid;
    private List<String> errors;
    private List<String> suggestions;

    public boolean passed() {
        return Boolean.TRUE.equals(valid);
    }
}

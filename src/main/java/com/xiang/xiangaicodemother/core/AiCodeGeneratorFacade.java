package com.xiang.xiangaicodemother.core;


import com.xiang.xiangaicodemother.ai.AiCodeGeneratorService;
import com.xiang.xiangaicodemother.ai.model.HtmlCodeResult;
import com.xiang.xiangaicodemother.ai.model.MultiFileCodeResult;
import com.xiang.xiangaicodemother.core.parser.CodeParserExecutor;
import com.xiang.xiangaicodemother.core.saver.CodeFileSaverExecutor;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口，根据类型生成并保存代码
     * @param userMessage 用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        validateAppId(appId);
        return switch (codeGenTypeEnum){
            case HTML -> {
                HtmlCodeResult result=aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result=aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }



    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage    用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型为空");
        }
        validateAppId(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream 代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .concatWith(Flux.defer(() -> {
                    String completeCode = codeBuilder.toString();
                    Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                    File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                    log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
                    return Flux.empty();
                }));
    }

    private void validateAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
    }

}


package com.xiang.xiangaicodemother.core.parser;

import com.xiang.xiangaicodemother.ai.model.HtmlCodeResult;
import com.xiang.xiangaicodemother.ai.model.MultiFileCodeResult;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 * 根据代码生成类型执行对应的解析器执行解析逻辑
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser=new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser=new MultiFileCodeParser();
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType){
        return switch (codeGenType){
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default->
                throw new RuntimeException("不支持的生成类型：" + codeGenType.getValue());
        };
    }
}

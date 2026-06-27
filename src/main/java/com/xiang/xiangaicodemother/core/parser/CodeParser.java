package com.xiang.xiangaicodemother.core.parser;


/**
 * 代码解析器策略接口
 */
public interface CodeParser<T> {

    /**
     * 解析代码内容
     *
     * @param codeContent 代码内容
     * @return 解析结果对象
     */
    T parseCode(String codeContent);
}

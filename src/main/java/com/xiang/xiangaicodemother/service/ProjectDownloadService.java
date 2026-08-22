package com.xiang.xiangaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 生成项目源码 ZIP 下载流。
 */
public interface ProjectDownloadService {

    void downloadProjectAsZip(Path projectRoot, String downloadFileName, HttpServletResponse response);

    void writeProjectZip(Path projectRoot, OutputStream outputStream);
}

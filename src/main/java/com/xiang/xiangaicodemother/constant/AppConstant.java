package com.xiang.xiangaicodemother.constant;

/**
 * 应用常量。
 */
public interface AppConstant {

    int GOOD_APP_PRIORITY = 99;

    int DEFAULT_APP_PRIORITY = 0;

    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    String CODE_DEPLOY_HOST = "http://localhost";
}

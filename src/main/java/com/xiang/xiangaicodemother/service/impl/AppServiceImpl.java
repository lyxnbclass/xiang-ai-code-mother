package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiang.xiangaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.xiang.xiangaicodemother.ai.AiCodeGenTypeRoutingService;
import com.xiang.xiangaicodemother.ai.model.CodeGenTypeRoutingResult;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.core.AiCodeGeneratorFacade;
import com.xiang.xiangaicodemother.core.builder.VueProjectBuilder;
import com.xiang.xiangaicodemother.core.handler.StreamHandlerExecutor;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.exception.ThrowUtils;
import com.xiang.xiangaicodemother.mapper.AppMapper;
import com.xiang.xiangaicodemother.model.dto.app.AppQueryRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppAddRequest;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.model.vo.AppVO;
import com.xiang.xiangaicodemother.model.vo.UserVO;
import com.xiang.xiangaicodemother.service.AppService;
import com.xiang.xiangaicodemother.service.AppCoverService;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import com.xiang.xiangaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用服务实现。
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "appName", "codeGenType", "deployKey", "priority", "userId",
            "editTime", "createTime", "updateTime", "deployedTime"
    );

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Resource
    private AppCoverService appCoverService;

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        ThrowUtils.throwIf(appAddRequest == null || StrUtil.isBlank(appAddRequest.getInitPrompt()),
                ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String initPrompt = appAddRequest.getInitPrompt().trim();
        ThrowUtils.throwIf(initPrompt.length() > 1000,
                ErrorCode.PARAMS_ERROR, "初始化 prompt 不能超过 1000 字");

        CodeGenTypeEnum selectedType = selectCodeGenType(initPrompt);
        App app = new App();
        app.setInitPrompt(initPrompt);
        app.setAppName(StrUtil.sub(initPrompt, 0, 12));
        app.setCodeGenType(selectedType.getValue());
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        app.setUserId(loginUser.getId());
        boolean saved = this.save(app);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建应用失败");
        log.info("应用创建成功，appId={}，codeGenType={}", app.getId(), selectedType.getValue());
        return app.getId();
    }

    private CodeGenTypeEnum selectCodeGenType(String initPrompt) {
        try {
            CodeGenTypeRoutingResult result = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
            if (result != null && result.getCodeGenType() != null) {
                return result.getCodeGenType();
            }
            log.warn("AI 生成类型路由返回空结果，使用本地规则降级");
        } catch (Exception e) {
            log.warn("AI 生成类型路由失败，使用本地规则降级");
            log.debug("AI 生成类型路由异常详情", e);
        }
        return fallbackCodeGenType(initPrompt);
    }

    private CodeGenTypeEnum fallbackCodeGenType(String prompt) {
        String normalized = prompt.toLowerCase();
        if (StrUtil.containsAny(normalized, "后台", "管理系统", "登录", "注册", "表单", "商城",
                "复杂交互", "数据管理", "dashboard", "admin", "vue", "react")) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        if (StrUtil.containsAny(normalized, "多页面", "多个页面", "官网", "网站导航", "产品页",
                "联系我们", "multi-page")) {
            return CodeGenTypeEnum.MULTI_FILE;
        }
        return CodeGenTypeEnum.HTML;
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }

        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");

        boolean userMessageSaved = chatHistoryService.addChatMessage(
                appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        ThrowUtils.throwIf(!userMessageSaved, ErrorCode.OPERATION_ERROR, "保存用户消息失败");

        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenType, appId);
        return streamHandlerExecutor.doExecute(
                contentFlux, chatHistoryService, appId, loginUser, codeGenType);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }

        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = generateUniqueDeployKey();
        }

        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        String sourceDirName = app.getCodeGenType() + "_" + appId;
        File sourceDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR, sourceDirName);
        if (!sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用代码不存在，请先生成应用");
        }

        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            boolean built = vueProjectBuilder.buildProject(sourceDir.getAbsolutePath());
            ThrowUtils.throwIf(!built, ErrorCode.OPERATION_ERROR, "Vue 项目构建失败，请检查生成代码");
            File distDir = new File(sourceDir, "dist");
            ThrowUtils.throwIf(!new File(distDir, "index.html").isFile(),
                    ErrorCode.OPERATION_ERROR, "Vue 项目未生成 dist/index.html");
            sourceDir = distDir;
        }

        File deployDir = new File(AppConstant.CODE_DEPLOY_ROOT_DIR, deployKey);
        try {
            if (deployDir.exists()) {
                FileUtil.del(deployDir);
            }
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败");
        }

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updated = this.updateById(updateApp);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        appCoverService.generateAppCoverAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    private String generateUniqueDeployKey() {
        for (int i = 0; i < 10; i++) {
            String candidate = RandomUtil.randomString(6);
            long count = this.count(QueryWrapper.create().eq("deployKey", candidate));
            if (count == 0) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成部署标识失败，请重试");
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        if (app.getUserId() != null) {
            User user = userService.getById(app.getUserId());
            appVO.setUser(userService.getUserVO(user));
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }

        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", request.getId())
                .like("appName", request.getAppName())
                .like("cover", request.getCover())
                .like("initPrompt", request.getInitPrompt())
                .eq("codeGenType", request.getCodeGenType())
                .eq("deployKey", request.getDeployKey())
                .eq("priority", request.getPriority())
                .eq("userId", request.getUserId());

        String sortField = request.getSortField();
        if (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(request.getSortOrder()));
        }
        return queryWrapper;
    }

    /**
     * 删除应用时同步逻辑删除其对话历史；任一步失败都会回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId;
        try {
            appId = Long.parseLong(id.toString());
        } catch (NumberFormatException e) {
            return false;
        }
        if (appId <= 0) {
            return false;
        }
        chatHistoryService.deleteByAppId(appId);
        boolean removed = super.removeById(id);
        if (removed) {
            aiCodeGeneratorServiceFactory.clearAppChatMemory(appId);
        }
        return removed;
    }
}

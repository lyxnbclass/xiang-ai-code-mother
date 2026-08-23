package com.xiang.xiangaicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.xiang.xiangaicodemother.annotation.AuthCheck;
import com.xiang.xiangaicodemother.common.BaseResponse;
import com.xiang.xiangaicodemother.common.DeleteRequest;
import com.xiang.xiangaicodemother.common.ResultUtils;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.constant.UserConstant;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.exception.ThrowUtils;
import com.xiang.xiangaicodemother.model.dto.app.AppAddRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppAdminUpdateRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppDeployRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppQueryRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppUpdateRequest;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.vo.AppVO;
import com.xiang.xiangaicodemother.service.AppService;
import com.xiang.xiangaicodemother.service.ProjectDownloadService;
import com.xiang.xiangaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 应用接口。
 */
@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(defaultValue = "false") boolean agent,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        ThrowUtils.throwIf(message.length() > 1000, ErrorCode.PARAMS_ERROR, "提示词不能超过 1000 字");
        User loginUser = userService.getLoginUser(request);

        Flux<ServerSentEvent<String>> content = appService.chatToGenCode(appId, message, loginUser, agent)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(JSONUtil.toJsonStr(Map.of("d", chunk)))
                        .build());
        Mono<ServerSentEvent<String>> done = Mono.just(ServerSentEvent.<String>builder()
                .event("done")
                .data("")
                .build());

        return content.concatWith(done)
                .onErrorResume(error -> {
                    log.error("应用代码生成失败，appId={}", appId, error);
                    String errorMessage = error instanceof BusinessException
                            ? error.getMessage()
                            : "代码生成失败，请稍后重试";
                    String data = JSONUtil.toJsonStr(Map.of("message", errorMessage));
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("business-error")
                            .data(data)
                            .build());
                });
    }

    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest requestBody,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(requestBody == null || requestBody.getAppId() == null
                || requestBody.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.deployApp(requestBody.getAppId(), loginUser));
    }

    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        validateId(appId);
        App app = getExistingApp(appId);
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        Path sourceDir = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, app.getCodeGenType() + "_" + appId);
        projectDownloadService.downloadProjectAsZip(sourceDir, "app-" + appId + ".zip", response);
    }

    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest requestBody,
                                     HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.createApp(requestBody, loginUser));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest requestBody,
                                           HttpServletRequest request) {
        validateId(requestBody == null ? null : requestBody.getId());
        ThrowUtils.throwIf(StrUtil.isBlank(requestBody.getAppName()),
                ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        ThrowUtils.throwIf(requestBody.getAppName().length() > 256,
                ErrorCode.PARAMS_ERROR, "应用名称不能超过 256 字");
        User loginUser = userService.getLoginUser(request);
        App oldApp = getExistingApp(requestBody.getId());
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        App app = new App();
        app.setId(requestBody.getId());
        app.setAppName(requestBody.getAppName().trim());
        app.setEditTime(LocalDateTime.now());
        boolean updated = appService.updateById(app);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用失败");
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest requestBody,
                                           HttpServletRequest request) {
        validateId(requestBody == null ? null : requestBody.getId());
        User loginUser = userService.getLoginUser(request);
        App oldApp = getExistingApp(requestBody.getId());
        boolean owner = oldApp.getUserId().equals(loginUser.getId());
        boolean admin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!owner && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(appService.removeById(requestBody.getId()));
    }

    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam long id) {
        validateId(id);
        return ResultUtils.success(appService.getAppVO(getExistingApp(id)));
    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest requestBody,
                                                       HttpServletRequest request) {
        validateUserPageRequest(requestBody);
        User loginUser = userService.getLoginUser(request);
        requestBody.setUserId(loginUser.getId());
        return ResultUtils.success(queryAppVOPage(requestBody));
    }

    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest requestBody) {
        validateUserPageRequest(requestBody);
        requestBody.setPriority(AppConstant.GOOD_APP_PRIORITY);
        return ResultUtils.success(queryAppVOPage(requestBody));
    }

    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest requestBody) {
        validateId(requestBody == null ? null : requestBody.getId());
        getExistingApp(requestBody.getId());
        return ResultUtils.success(appService.removeById(requestBody.getId()));
    }

    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest requestBody) {
        validateId(requestBody == null ? null : requestBody.getId());
        getExistingApp(requestBody.getId());
        if (requestBody.getAppName() != null) {
            ThrowUtils.throwIf(StrUtil.isBlank(requestBody.getAppName())
                            || requestBody.getAppName().length() > 256,
                    ErrorCode.PARAMS_ERROR, "应用名称长度应为 1 到 256 字");
        }
        if (requestBody.getCover() != null) {
            ThrowUtils.throwIf(requestBody.getCover().length() > 512,
                    ErrorCode.PARAMS_ERROR, "应用封面地址不能超过 512 字");
        }
        if (requestBody.getPriority() != null) {
            ThrowUtils.throwIf(requestBody.getPriority() < 0, ErrorCode.PARAMS_ERROR, "优先级不能小于 0");
        }

        App app = new App();
        BeanUtil.copyProperties(requestBody, app);
        app.setEditTime(LocalDateTime.now());
        boolean updated = appService.updateById(app);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用失败");
        return ResultUtils.success(true);
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest requestBody) {
        ThrowUtils.throwIf(requestBody == null || requestBody.getPageNum() <= 0
                || requestBody.getPageSize() <= 0, ErrorCode.PARAMS_ERROR, "分页参数错误");
        return ResultUtils.success(queryAppVOPage(requestBody));
    }

    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(@RequestParam long id) {
        validateId(id);
        return ResultUtils.success(appService.getAppVO(getExistingApp(id)));
    }

    private Page<AppVO> queryAppVOPage(AppQueryRequest requestBody) {
        long pageNum = requestBody.getPageNum();
        long pageSize = requestBody.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(requestBody);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> records = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(records);
        return appVOPage;
    }

    private void validateUserPageRequest(AppQueryRequest requestBody) {
        ThrowUtils.throwIf(requestBody == null || requestBody.getPageNum() <= 0
                || requestBody.getPageSize() <= 0 || requestBody.getPageSize() > 20,
                ErrorCode.PARAMS_ERROR, "分页参数错误，每页最多查询 20 个应用");
    }

    private void validateId(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
    }

    private App getExistingApp(long id) {
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        return app;
    }
}

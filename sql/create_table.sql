-- 应用表（教程第 5 阶段）
create table if not exists app
(
    id           bigint                             not null comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户 id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    unique key uk_deployKey (deployKey),
    index idx_appName (appName),
    index idx_userId (userId)
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表（教程第 6 阶段）
create table if not exists chat_history
(
    id          bigint                                not null comment 'id' primary key,
    message     text                                  not null comment '消息',
    messageType varchar(32)                           not null comment '消息类型（user/ai）',
    appId       bigint                                not null comment '应用 id',
    userId      bigint                                not null comment '创建用户 id',
    createTime  datetime(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    updateTime  datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '更新时间',
    isDelete    tinyint     default 0                 not null comment '是否删除',
    index idx_appId (appId),
    index idx_createTime (createTime),
    index idx_appId_createTime (appId, createTime)
) comment '对话历史' collate = utf8mb4_unicode_ci;

package com.xiang.xiangaicodemother.service.impl;

import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.xiang.xiangaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.xiang.xiangaicodemother.service.AppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatHistoryServiceImplTest {

    private ChatHistoryServiceImpl chatHistoryService;
    private AppService appService;

    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryServiceImpl();
        appService = mock(AppService.class);
        ReflectionTestUtils.setField(chatHistoryService, "appService", appService);
    }

    @Test
    void rejectsUsersWhoAreNeitherCreatorNorAdmin() {
        App app = App.builder().id(100L).userId(1L).build();
        User visitor = User.builder().id(2L).userRole("user").build();
        when(appService.getById(100L)).thenReturn(app);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(100L, 10, null, visitor));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void rejectsOversizedHistoryPage() {
        User user = User.builder().id(1L).userRole("user").build();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(100L, 51, null, user));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void resolvesOnlySupportedMessageTypes() {
        assertEquals(ChatHistoryMessageTypeEnum.USER,
                ChatHistoryMessageTypeEnum.getEnumByValue("user"));
        assertEquals(ChatHistoryMessageTypeEnum.AI,
                ChatHistoryMessageTypeEnum.getEnumByValue("ai"));
        assertNull(ChatHistoryMessageTypeEnum.getEnumByValue("assistant"));
    }

    @Test
    void ignoresBlankFiltersAndRejectsUnsafeSortFields() {
        ChatHistoryQueryRequest request = new ChatHistoryQueryRequest();
        request.setMessage("");
        request.setMessageType("");
        request.setSortField("id desc; drop table chat_history");

        String sql = chatHistoryService.getQueryWrapper(request).toSQL().toLowerCase();

        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("messagetype ="));
    }
}

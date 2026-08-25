package com.xiang.xiangaicodemother.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void writesBusinessErrorAsSseForPreControllerFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/app/chat/gen/code");
        request.addHeader("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = new GlobalExceptionHandler().businessExceptionHandler(
                new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求太频繁"), request, response);

        assertNull(result);
        assertTrue(response.getContentAsString().contains("event: business-error"));
        assertTrue(response.getContentAsString().contains("请求太频繁"));
    }
}

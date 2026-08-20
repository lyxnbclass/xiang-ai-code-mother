package com.xiang.xiangaicodemother.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticResourceControllerTest {

    private final StaticResourceController controller = new StaticResourceController();

    @Test
    void rejectsPathTraversal() {
        MockHttpServletRequest request = requestFor("/static/multi_file_1/../secret.txt");

        assertEquals(HttpStatus.FORBIDDEN,
                controller.serveStaticResource("multi_file_1", request).getStatusCode());
    }

    @Test
    void rejectsInvalidDirectoryName() {
        MockHttpServletRequest request = requestFor("/static/../index.html");

        assertEquals(HttpStatus.BAD_REQUEST,
                controller.serveStaticResource("..", request).getStatusCode());
    }

    private MockHttpServletRequest requestFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
        return request;
    }
}

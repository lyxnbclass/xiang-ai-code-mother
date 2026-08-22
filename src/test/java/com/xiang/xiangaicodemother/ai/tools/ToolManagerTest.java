package com.xiang.xiangaicodemother.ai.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ToolManagerTest {

    @Test
    void registersToolsByMethodNameAndReturnsDefensiveArray() {
        ToolManager manager = new ToolManager(new BaseTool[]{
                new FileWriteTool(), new FileReadTool(), new FileModifyTool(),
                new FileDeleteTool(), new FileDirReadTool()
        });

        assertNotNull(manager.getTool("writeFile"));
        assertNotNull(manager.getTool("readDir"));
        assertNull(manager.getTool("missing"));
        BaseTool[] tools = manager.getAllTools();
        assertEquals(5, tools.length);
        tools[0] = null;
        assertNotNull(manager.getAllTools()[0]);
    }
}

package com.example.code_versioning_plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class CodeHistoryToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        CodeHistoryPanel codeHistoryPanel = null;
        try {
            codeHistoryPanel = new CodeHistoryPanel(project,toolWindow);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(codeHistoryPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

package com.example.code_versioning_plugin;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class myAction extends AnAction{
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 从事件对象中获取Project实例
        Project project = e.getProject();
        // 检查project是否为null
        if (project == null) {
            System.err.println("Project is null, cannot proceed.");
            return;
        }
        try {
            if (VersionControl.getInstance().push()) {
                // 使用NotificationGroupManager进行成功通知
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("listener")
                        .createNotification("推送成功", "推送操作已完成", NotificationType.INFORMATION)
                        .notify(project);
            } else {
                // 使用NotificationGroupManager进行失败通知
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("listener")
                        .createNotification("推送失败", "当前无修改，无需推送", NotificationType.ERROR)
                        .notify(project);
            };
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

package com.example.code_versioning_plugin;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class ProListener implements com.intellij.openapi.project.ProjectManagerListener{

    //项目打开时初始化
    public void projectOpened(@NotNull Project pro) {
        //初始化插件
        Path proDir = Path.of(pro.getBasePath());
        try {
            VersionControl.init(proDir);
            //添加文件事件监听器
            VirtualFileManager.getInstance().addVirtualFileListener(new FileListener());
        } catch (IOException e) {
            System.out.println("Error initializing version control : " +e.getMessage());
        }
    }

    //项目关闭时的操作
    public void projectClosing(@NotNull Project pro) {
        VersionControl instance=VersionControl.getInstance();
        instance.settle();
        System.out.println("closed");
    }
}
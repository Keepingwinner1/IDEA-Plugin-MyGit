package com.example.code_versioning_plugin;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FileListener implements com.intellij.openapi.vfs.VirtualFileListener {
    //监测文件创建
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        storeChanged(event,"CREATE");
    }


    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
        storeChanged(event,"DELETE");
    }

    @Override
    public void beforeContentsChange(@NotNull VirtualFileEvent event) {
        storeChanged(event,"CHANGE");
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        VirtualFile virtualFile = event.getFile();
        if(!virtualFile.isDirectory()&&Tools.isUnderDir(virtualFile.toNioPath())) {
            Path filePath = Paths.get(virtualFile.getPath());
            String filename = VersionControl.getInstance().getChangeMap().get(Paths.get(event.getFile().getPath())).getFilenum();
            Path codeVersion =  VersionControl.getInstance().getProPath().resolve(VersionControl.fileName);
            Path temp = codeVersion.resolve(VersionControl.copyFile);
            Path newpath=temp.resolve(filename);
            try {
                if(Files.mismatch(filePath,newpath)==-1L){
                    VersionControl.getInstance().getChangeMap().remove(Paths.get(event.getFile().getPath()));
                    Files.delete(newpath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //用于文件保存
    private void copyFile(@NotNull VirtualFileEvent event) {
        VersionControl instance = VersionControl.getInstance();
        HashMap<Path,FileStatus> map = instance.getChangeMap();
        String filename = map.get(Paths.get(event.getFile().getPath())).getFilenum();

        Path codeVersion = instance.getProPath().resolve(VersionControl.fileName);
        Path temp = codeVersion.resolve(VersionControl.copyFile);
        Path newpath=temp.resolve(filename);
        //若不存在文件，创建；存在则不操作
        if(!Files.exists(newpath)){
            try {
                Files.createFile(newpath);
                Files.write(newpath,event.getFile().contentsToByteArray(),StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void storeChanged(@NotNull VirtualFileEvent event,String type){
        VirtualFile virtualFile = event.getFile();
        //判断是否为文件，且判断是否是插件产生的文件，若是文件且不是插件产生的文件，继续
        if(!virtualFile.isDirectory()&&Tools.isUnderDir(virtualFile.toNioPath())) {
            Path filePath = Paths.get(virtualFile.getPath()); // 获取文件的路径

            System.out.println("A file has been " + type + ": " + filePath);

            VersionControl instance = VersionControl.getInstance();
            HashMap<Path,FileStatus> map=instance.getChangeMap();

            //获取时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = currentDateTime.format(formatter);

            if(!map.containsKey(filePath)){
                FileStatus status=new FileStatus(type,formattedDateTime,String.valueOf(filePath.hashCode()));
                map.put(filePath,status);
            }
            else{
                String lastStatus = map.get(filePath).getStatus();
                if(lastStatus.equals("CHANGE")){
                    if(type.equals("CREATE")){
                        throw new RuntimeException("记录错误");
                    }
                }
                else if(lastStatus.equals("CREATE")){
                    if(type.equals("CREATE")){
                        throw new RuntimeException("记录错误");
                    }
                    else if(type.equals("DELETE")){
                        map.remove(filePath);
                        return;
                    }
                    else {
                        type="CREATE";
                    }
                }
                else{
                    if(type.equals("CREATE")){
                        type="CHANGE";
                    }
                    else {
                        throw new RuntimeException("记录错误");
                    }
                }
                map.get(filePath).setStatus(type);
                map.get(filePath).setTimestamp(formattedDateTime);
            }
            if(!type.equals("CREATE")){
                copyFile(event);
            }
        }
    }


}
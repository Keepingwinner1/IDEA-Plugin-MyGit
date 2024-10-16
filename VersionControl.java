package com.example.code_versioning_plugin;

import com.jetbrains.rd.generator.nova.PredefinedType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VersionControl {
    public static final String fileName = "codeVersion";
    public static final String copyFile = "temp";
    private static VersionControl instance;
    private Path proPath;
    private int version;
    private HashMap<Path,FileStatus>  changeMap;

    //获得当前的类
    public static VersionControl getInstance() {
        if (instance == null) {
            instance = new VersionControl();
            instance.version=0;
        }
        return instance;
    }

    public Path getProPath() {
        return proPath;
    }

    public int getVersion() {
        return version;
    }

    public HashMap<Path, FileStatus> getChangeMap() {
        return changeMap;
    }

    //检测项目目录下是否有插件所需的文件夹，没有则创建
    public static void init(Path pro)throws IOException{
        VersionControl instance = getInstance();
        if(instance.proPath!=pro){
            instance.proPath = pro;
            instance.version = 0;
        }
        if(!Files.exists(pro)){
            throw new IOException("项目文件不存在");
        }
        //创建codeVersion文件夹
        Path pluginFile = pro.resolve(fileName);
        if(!Files.exists(pluginFile)){
            Files.createDirectory(pluginFile);
        }
        //创建temp文件夹
        Path copyCode =pluginFile.resolve(copyFile);
        if(!Files.exists(copyCode)){
            Files.createDirectory(copyCode);
        }
        //创建版本信息
        Path versionInfo = pluginFile.resolve("verInfo.txt");
        if(!Files.exists(versionInfo)){
            Files.createFile(versionInfo);
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = currentDateTime.format(formatter);
            Files.writeString(versionInfo, instance.version+" "+formattedDateTime+"\n", StandardOpenOption.APPEND);
            //创建V1.0文件夹
            Path ver = pluginFile.resolve("V1."+instance.version);
            if(!Files.exists(ver)) {
                Files.createDirectory(ver);
                //获取当前版本目录
                Tools.readPaths(instance.proPath.resolve("src"),ver.resolve("ProjectDir.txt"));
            }
        }
        else {
            getCurrentVersion(versionInfo, instance);
        }
        //读取修改缓存
        Path MapTemp = copyCode.resolve("MapTemp.txt");
        if(!Files.exists(MapTemp)){
            instance.changeMap = new HashMap<>();
        }
        else{
            instance.changeMap=Tools.readHashMapFromFile(MapTemp);
        }
    }

    //获取最新版本信息
    private static void getCurrentVersion(Path versionInfo, VersionControl instance) {
        String filePath = versionInfo.toString(); // 输入文件的路径

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String Line;
            String LastLine=null;
            while ((Line = br.readLine()) != null) {
                LastLine=Line;
            }
            // 处理最后一行
            if (LastLine != null) {
                String result = Tools.getLastPartBeforeSpace(LastLine);
                System.out.println("当前版本: " + result);
                instance.version=Integer.parseInt(result);
            } else {
                System.out.println("文件为空");
            }
        } catch (FileNotFoundException e) {
            System.err.println("文件未找到: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + e.getMessage());
        }
    }

    public boolean push() throws IOException {
        if(changeMap.isEmpty()){
            return false;
        }
        //获取路径
        Path codeVersion = proPath.resolve(fileName);
        Path temp = codeVersion.resolve(copyFile);
        String currentVersion = "V1." + (++version);
        Path currentVersionPath = codeVersion.resolve(currentVersion);
        if (!Files.exists(currentVersionPath)) {
            Files.createDirectories(currentVersionPath);
        }
        //获取最新版本目录
        Tools.readPaths(instance.proPath.resolve("src"),currentVersionPath.resolve("ProjectDir.txt"));


        //记录改动
        for(HashMap.Entry<Path,FileStatus> entry : instance.changeMap.entrySet()){
            Path after = entry.getKey();
            FileStatus status = entry.getValue();
            Path changeFile=currentVersionPath.resolve(status.getFilenum());
            Changes changes=null;
            if(status.getStatus().equals("CREATE")){
                if(!Files.exists(changeFile)){
                    Files.copy(after,changeFile);
                }
            }
            else if(status.getStatus().equals("DELETE")){
                Path before = temp.resolve(status.getFilenum());
                if(!Files.exists(changeFile)){
                    Files.copy(before,changeFile);
                }
            }
            else{
                Path before = temp.resolve(status.getFilenum());
                changes = new Changes(before,after);
                if(changes.status!= Changes.Status.NONE){
                    changes.saveToFile(changeFile.toString());
                }
                else{
                    instance.changeMap.remove(after);
                }
            }
        }

        //将历史记录留档备份
        Path change = currentVersionPath.resolve("change.txt");
        if(!Files.exists(change)){
            Files.createFile(change);
        }
        Tools.writeHashMapToFile(instance.changeMap,change);

        if(changeMap.isEmpty()){
            return false;
        }
        instance.changeMap.clear();

        //更新当前版本
        Path versionInfo = codeVersion.resolve("verInfo.txt");
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        Files.writeString(versionInfo, instance.version+" "+formattedDateTime+"\n", StandardOpenOption.APPEND);

        //重置temp
        Tools.deleteDirectory(temp);
        Files.createDirectory(temp);

        return true;
    }

    public class FileCompare{
        public List<String> before;
        public List<String> after;
        public FileCompare(List<String> before, List<String> after) {
            this.before = before;
            this.after = after;
        }
    }

    //获取当前代码的版本信息
    public List<Map<String, String>> getProjectVersionInfo() throws IOException {
        Path codeVersion = proPath.resolve(fileName);
        //Path currentVer=codeVersion.resolve("V1."+desVersion);
        Path currentVerDir=codeVersion.resolve("verInfo.txt");
        List<String> lines = Files.readAllLines(currentVerDir);
        lines.remove(0);//去除版本0
        List<Map<String, String>> resultList = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            Map<String, String> map = new HashMap<>();
            map.put("version", parts[0]);
            map.put("time", parts[1]+" "+parts[2]);
            resultList.add(map);
        }
        return resultList;
    }

    //获取某一版本相较于上一版本的修改列表，即desVersion-1到desVersion的变化列表
    public HashMap<Path, FileStatus> getChangeDirOfDesVersion(String desVersion) {
        Path codeVersion = proPath.resolve(fileName);
        Path version=codeVersion.resolve("V1."+desVersion);
        Path changeList=version.resolve("change.txt");
        return Tools.readHashMapFromFile(changeList);
    }

    //获取当前缓存中的文件修改列表：当次打开项目进行的修改无法记录？
    public HashMap<Path, FileStatus> getChangeDirOfCurrentVersion() {
        return changeMap;
    }

    //获取某个文件某个版本的内容以及上一个版本的内容，desVersion-1和desVersion
    public FileCompare getFileOfCertainVersion(Path filepath,int desVersion){
        try {
            Path codeVersion = proPath.resolve(fileName);
            Path temp = codeVersion.resolve(copyFile);
            //首先获取desVersion的文件变化
            Path desVer=codeVersion.resolve("V1."+desVersion);
            HashMap<Path, FileStatus> map = Tools.readHashMapFromFile(desVer.resolve("change.txt"));
            List<String> before = null;
            List<String> after = null;
            //如果本次是创建,直接获取创建的内容
            if(map.get(filepath).getStatus().equals("CREATE")){
                before=new ArrayList<String>();
                Path changeDetails=desVer.resolve(map.get(filepath).getFilenum());
                after=Files.readAllLines(changeDetails);
                return new FileCompare(before,after);
            }//若本次是删除，直接得到删除前的内容
            else if(map.get(filepath).getStatus().equals("DELETE")){
                after=new ArrayList<String>();
                Path changeDetails=desVer.resolve(map.get(filepath).getFilenum());
                before= Files.readAllLines(changeDetails);
                return new FileCompare(before,after);
            }
            //若是修改，首先获得该文件的最新版本
            map = instance.changeMap;
            FileStatus status = map.get(filepath);
            Path file = null;
            if (status == null) {
                file = filepath;
            } else {
                file = temp.resolve(status.getFilenum());
            }
            int currentVer = version;
            //若当前不存在该文件，说明该文件已被删除，找到该文件的最后一个版本;
            List<String> fileContent = null;
            if (Files.exists(file)) {
                fileContent = Files.readAllLines(file);
            }
            if(currentVer==desVersion){
                if(fileContent==null){
                    after=new ArrayList<String>();
                }
                else{
                    after=new ArrayList<String>(fileContent);
                }
            }
            //开始回溯
            while (currentVer >= desVersion) {
                Path version = codeVersion.resolve("V1." + currentVer);
                Path changeList = version.resolve("change.txt");
                map = Tools.readHashMapFromFile(changeList);
                status = map.get(filepath);
                if (status == null) {
                    currentVer--;
                    continue;
                }
                if (status.getStatus().equals("DELETE")) {
                    fileContent=Files.readAllLines(version.resolve(status.getFilenum()));
                } else if (status.getStatus().equals("CREATE")) {
                    if (fileContent != null) {
                        fileContent.clear();
                    }
                    else{
                        fileContent=new ArrayList<String>();
                    }
                } else {
                    Changes changes = Changes.loadFromFile(version.resolve(status.getFilenum()).toString());
                    Changes.rollBack(changes, fileContent);
                }
                currentVer--;
                if(fileContent!=null){
                    if (currentVer == desVersion) {
                            after=new ArrayList<String>(fileContent);
                    } else if (currentVer == desVersion-1) {
                        before = new ArrayList<String>(fileContent);
                    }
                }
            }
            return new FileCompare(before, after);
        }
        catch (IOException | ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    //获取某个文件当前未push内容与最新版本的内容，获取V1.version和Current版本的文件内容
    public FileCompare getFileOfCurrentVersion(Path filepath){
        try {
            List<String> after = null;
            List<String>before = null;
            Path codeVersion = proPath.resolve(fileName);
            Path temp=codeVersion.resolve(copyFile);
            FileStatus status = changeMap.get(filepath);
            if(status.getStatus().equals("CREATE")){
                after=Files.readAllLines(filepath);
                return new FileCompare(new ArrayList<String>(),after);
            }
            else if(status.getStatus().equals("DELETE")){
                Path file=temp.resolve(status.getFilenum());
                before=Files.readAllLines(file);
                return new FileCompare(before,new ArrayList<String>());
            }
            else{
                after=Files.readAllLines(filepath);
                Path file=temp.resolve(status.getFilenum());
                before=Files.readAllLines(file);
                return new FileCompare(before,after);
            }
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    //获取某一版本相较于上一版本的修改列表，即desVersion-1到desVersion的变化列表
    public HashMap<Path,FileStatus> getChangeMapOfCertainVersion(int desVersion){
        Path codeVersion = proPath.resolve(fileName);
        Path currentVer=codeVersion.resolve("V1."+desVersion);
        Path currentVerChange=currentVer.resolve("change.txt");
        return Tools.readHashMapFromFile(currentVerChange);
    }

    //获取某个版本的变化细节，即desVersion-1到desVersion的变化
    public Changes getChangeOfCertainVersion(Path filepath,int desVersion){
        try{
            Path codeVersion = proPath.resolve(fileName);
            Path version=codeVersion.resolve("V1."+desVersion);
            Path changeList=version.resolve("change.txt");
            HashMap<Path,FileStatus> map=Tools.readHashMapFromFile(changeList);
            Path changeDetails = version.resolve(map.get(filepath).getFilenum());
            return Changes.loadFromFile(changeDetails.toString());
        }
        catch (IOException | ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    //获取当前缓存中的变化细节
    public Changes getChangeOfCurrentVersion(Path filepath){
        try {
            FileStatus status = changeMap.get(filepath);
            Changes changes = null;
            Path codeVersion = proPath.resolve(fileName);
            Path temp = codeVersion.resolve(copyFile);
            if (status.getStatus().equals("CREATE")) {
                changes = new Changes(null, filepath);
            } else if (status.getStatus().equals("DELETE")) {
                Path before = temp.resolve(status.getFilenum());
                changes = new Changes(before, null);
            } else {
                Path before = temp.resolve(status.getFilenum());
                changes = new Changes(before, filepath);
            }
            return changes;
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    //项目关闭时保存map缓存到文件
    public void settle(){
        try {
            //保存修改缓存
            Path codeVersion = proPath.resolve(fileName);
            Path temp = codeVersion.resolve(copyFile);
            Path MapTemp = temp.resolve("MapTemp.txt");
            if (!changeMap.isEmpty()) {
                if (!Files.exists(MapTemp)) {
                    Files.createFile(MapTemp);
                }
                Tools.writeHashMapToFile(changeMap, MapTemp);
            }
            else {
                Files.deleteIfExists(MapTemp);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
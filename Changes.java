package com.example.code_versioning_plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Changes implements Serializable {
    public enum Status {
        CHANGE, CREATE,DELETE,NONE
    }
    public Status status;
    public class Change implements Serializable {
        public int line;
        public String content;
        public Change( int line, String content) {
            this.line = line;
            this.content = content;
        }
    }
    public List<Change> deleteLines = new ArrayList<>();
    public List<Change> addLines =  new ArrayList<>();
    public String filePath;
    public String fileName;


    //生成变更记录
    public Changes(Path oldFile, Path newFile) throws IOException {
        if(oldFile == null) {
            if(Files.notExists(newFile))
                throw  new IOException("文件路径有误");
            status = Status.CREATE;
            filePath = newFile.toString();
            fileName = newFile.getFileName().toString();
            deleteLines =null;
            List<String> newlines = Files.readAllLines(newFile);
            for(int i=0;i<newlines.size();i++) {
                addLines.add(new Change(i,newlines.get(i)));
            }
        }
        else if(newFile==null||Files.notExists(newFile)) {
            status = Status.DELETE;
            filePath = oldFile.toString();
            fileName = oldFile.getFileName().toString();
            addLines =null;
            List<String> newlines = Files.readAllLines(oldFile);
            for(int i=0;i<newlines.size();i++) {
                deleteLines.add(new Change(i,newlines.get(i)));
            }
        }
        else {
            status = Status.CHANGE;
            filePath = newFile.toString();
            fileName = newFile.getFileName().toString();
            List<String> oldlines = Files.readAllLines(oldFile);
            List<String> newlines = Files.readAllLines(newFile);
            int p = 0;
            for (int i = 0; i < newlines.size(); i++) {
                int j = p;
                for (; j < oldlines.size(); j++) {
                    if (newlines.get(i).equals(oldlines.get(j))) {
                        for (; p < j; p++) {
                            deleteLines.add(new Change(p, oldlines.get(p)));
                        }
                        p = j + 1;
                        break;
                    }
                }
                if (j == oldlines.size()) {
                    addLines.add(new Change(i, newlines.get(i)));
                }
            }
            for(;p<oldlines.size();p++) {
                deleteLines.add(new Change(p, oldlines.get(p)));
            }
        }
        if(addLines.size()==0 && deleteLines.size()==0) {
            status = Status.NONE;
        }
    }

    //变更记录保存到指定路径
    public void saveToFile(String filePath) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
        }
    }

    // 从文件读取 Changes 对象
    public static Changes loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            return (Changes) in.readObject();
        }
    }


    //根据记录将文件回退到变更前
    public static List<String> rollBack(Changes changes,List<String> f) throws IOException {
        for (int i = changes.addLines.size() - 1; i >= 0; i--) {
            f.remove(changes.addLines.get(i).line);
        }
        for (Change i : changes.deleteLines) {
            f.add(i.line, i.content);
        }
        return f;
    }
}
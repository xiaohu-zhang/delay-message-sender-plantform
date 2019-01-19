package com.cmcc.timer.mgr.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public abstract class FileUtils {
    
    public static File createNewFile(String newFileName) {
        File f = new File(newFileName);
        if(!f.exists()){
            try {
                f.getParentFile().mkdirs();
                f.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("error in create File at " + f.getAbsolutePath(),e);
            }
        }
        return f;
    }
    
    public static File createNewDir(String dirPath){
        File f = new File(dirPath);
        if(!f.exists()){
            f.mkdirs();
        }
        return f;
    }
    
    public static String stractMaxSuffixFileName(Path dir,String yyyyMMddhh) throws IOException{
        try(Stream<Path> p1 = TimerUtils.getPathStream(dir).get()){
            Path rp = p1.filter(p->{
                return p.toFile().getName().contains(yyyyMMddhh);
            }).max((f1,f2)->{
                String f1Name = f1.toFile().getName();
                String f2Name = f2.toFile().getName();
                return Integer.valueOf(f1Name.substring(f1Name.lastIndexOf("."))).compareTo(
                        Integer.valueOf(f2Name.substring(f2Name.lastIndexOf("."))));
            }).orElse(Paths.get("redo."+yyyyMMddhh+".log.0"));
            return rp != null ? rp.toString() :null;
        }
    }
}

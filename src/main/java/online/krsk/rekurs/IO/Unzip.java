/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package online.krsk.rekurs.IO;

//import dorkbox.cabParser.CabException;
//import dorkbox.cabParser.CabParser;
import dorkbox.cabParser.CabException;
import dorkbox.cabParser.CabParser;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author admin2
 */
public class Unzip {
 File path;
    public Unzip(File path)
    {
        this.path = path;
    }

    /**
     *
     * @param source
     * @param out
     * @throws IOException
     */
    
    public String unzip(File source) throws FileNotFoundException, IOException {
        
        try(ZipInputStream zin = new ZipInputStream(new FileInputStream(source)))
        {
            ZipEntry entry;
            String name;
            long size;
            File path2 = new File(path.getAbsolutePath()+"\\"+source.getName());
            path2.mkdir();
            
            while((entry=zin.getNextEntry())!=null){
                  
                name = entry.getName(); // получим название файла
                Boolean isdir = entry.isDirectory();
                size=entry.getSize();  // получим его размер в байтах
                System.out.printf("File name: %s \t File size: %d \n", name, size);
                 
                // распаковка
                
                if(isdir)
                {
                    File f = new File(path2+"\\"+entry.getName()); f.mkdir(); 
                    System.out.println("!!! "+entry.getName()+"  "+entry.isDirectory());
                    System.out.println("!!!2 "+f.getAbsolutePath()+"  "+f.getName()+"  "+entry.isDirectory());
                }
                else
                {
                FileOutputStream fout = new FileOutputStream(new File(path2.getAbsoluteFile()+"\\"+name));    
                for (int c = zin.read(); c != -1; c = zin.read()) {
                    fout.write(c);
                    }
                
                fout.flush();
                zin.closeEntry();
                fout.close();
                }
            }
        }
        catch(Exception ex){
              
            System.out.println(ex.getMessage());
        } return source.getName();
    } 
   
    public String unzipCAB(File source) throws IOException, CabException{
    
        final InputStream is = new FileInputStream(source.getAbsoluteFile());
        CabParser cabParser = new CabParser(is, new File(path.getAbsolutePath()+"\\"+source.getName()));
        cabParser.extractStream();
        
        
        return source.getName();
    
    }
    
    public String unzip7z(File source) throws Exception{
    
        ExtractorUtil.extract(source, path.getAbsolutePath()+"\\"+source.getName());
        
        
        
    return source.getName();
    }
    
    public String unzip7zSHELL(File source) throws IOException{
    
    Process process = Runtime.getRuntime()
      .exec(String.format("C:\\Program Files\\7-Zip\\7z.exe  e "+source.getAbsoluteFile()+
              " -o\""+path.getAbsolutePath()+"\\"+source.getName().substring(0,source.getName().length()-4)+"\"", source.getAbsolutePath()));
    System.out.println(String.format("C:\\Program Files\\7-Zip\\7z.exe  e "+source.getAbsoluteFile()+
              " -o\""+path.getAbsolutePath()+"\\"+source.getName().substring(0,source.getName().length()-4)+"\"", source.getAbsolutePath()));
    
    return source.getName();
    }
}

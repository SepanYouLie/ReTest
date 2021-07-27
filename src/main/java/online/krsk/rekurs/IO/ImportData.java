/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package online.krsk.rekurs.IO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Integer.parseInt;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 *
 * @author admin
 */
public class ImportData implements Runnable {

    Thread t;
    File source;
    File errFile;
    File workDir;
    private String msg=null;
    static ArrayList<Test> listTests = new ArrayList<Test>();
    static ArrayList<TestOut> listTestsOut = new ArrayList<TestOut>();
    
    
    public String getMsg(){
    return this.msg;}
    
    public ImportData(File source, File parentPath, File workDir) throws IOException
        {
            t = new Thread(this, workDir.toString());
            this.source = source;
            this.errFile = new File(parentPath.toString()+"\\errFile.txt");
            this.errFile.createNewFile();
            this.errFile.canWrite();
            this.workDir = workDir;
            //t.setPriority(9);
            t.start();
        }
    
    public void run(){
        try {
            
            msg = createTestList(workDir);
            
        } catch (IOException ex) {
            Logger.getLogger(ImportData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    synchronized String createTestList(File src) throws IOException{
        
        String msgExpExcel;
        
        listTests.add(new Test(src.getName()));
        int numTestIn = listTests.size()-1;
    parseQuestions(src.listFiles(new FilterName("question"))[0], numTestIn);
    parseAnswers(src.listFiles(new FilterName("answers."))[0], numTestIn);
    parseTestName(src.listFiles(new FilterName("test"))[0], numTestIn);
    parseSections(src.listFiles(new FilterName("section"))[0], numTestIn);
    
        
        listTestsOut.add(new TestOut());
        int numTestOut = listTestsOut.size()-1;
        createTestOut(listTests.get(numTestIn), listTestsOut.get(numTestOut));
        verifyTestOut(listTestsOut.get(numTestOut));
        
        msgExpExcel = exportToXLS(listTestsOut.get(numTestOut));
        
        return "Обработан тест "+listTests.get(numTestIn).nameArr[1]+". Создан файл: "+msgExpExcel; 
    
    }
    
    HSSFWorkbook readWorkbook (String filename) {
        try {
               POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(filename));
               HSSFWorkbook wb = new HSSFWorkbook(fs);
               return wb;
        }
        catch (Exception e) {
               return null;
        }
        }
    
    void writeWorkbook(HSSFWorkbook wb, String fileName) {
       try {
               FileOutputStream fileOut = new FileOutputStream(fileName);
               wb.write(fileOut);
               fileOut.close();
       }
       catch (Exception e) {
               //Обработка ошибки
       }
}    
    
    String exportToXLS (TestOut testOut){
      
        Path path = source.toPath();
        String strpathOut = ""+path.getParent().toString()+"\\XLSout\\";
        File pathOut = new File(strpathOut);
        pathOut.mkdir();
                
        HSSFWorkbook exc = readWorkbook(path.getParent().toString()+"\\шаблон.xls");
        HSSFSheet sheet= exc.getSheetAt(0);
        for(int i=0; i<testOut.outString.size(); i++){
            String[] str = testOut.outString.get(i);
            HSSFRow row = sheet.createRow(i+1);
            for(int j=0; j<15; j++){
                HSSFCell cell = row.createCell(j);
                cell.setCellValue(str[j]);
            }
        }
        String xlsName = testOut.name.replaceAll("\"", "");
        xlsName = xlsName.replaceAll("\\?", "");
        xlsName = xlsName.replaceAll("\\/", "");
        xlsName = xlsName.replaceAll("\\:", "");
        
        if (new File(pathOut.getAbsolutePath()+"\\"+xlsName+".xls").exists()) {
            int j=1;
            while(new File(pathOut.getAbsolutePath()+"\\"+xlsName+j+".xls").exists()){
                j++;
            }
            xlsName = xlsName+"_"+j;
        }
        
        writeWorkbook(exc,pathOut.getAbsolutePath()+"\\"+xlsName+".xls");
        if (new File(pathOut.getAbsolutePath()+"\\"+xlsName+".xls").exists()) 
      return pathOut.getAbsolutePath()+"\\"+xlsName+".xls"; 
        else return "!____________________________________________________________________________________________________________! Файл не создан!";
    }
    
    
    
    void verifyTestOut(TestOut testOut){
    String str;
    for(int i=0; i<testOut.outString.size(); i++){
    for(int j=1; j<6; j+=2){
        
        str = testOut.outString.get(i)[j];
        //Сюда добавляем нужные замены
        str = str.replaceAll("<.*?>", "");
        
        testOut.outString.get(i)[j] = str;
        
        }
    
    }
    }
    
    void createTestOut(Test testIn, TestOut testOut) throws IOException{
        
        
        testOut.name = testIn.nameArr[1];
        testIn.questions2.forEach(questions -> {
        
            
            
            if(!"1".equals(questions[3])&&!"2".equals(questions[3])&&!"5".equals(questions[3]))return; //Выкидываем вопросы с установкой соответствия и "впечатление об увиденном"
            
            
            testOut.outString.add(new String[15]);
            int sizeOfTestOut = testOut.outString.size();
            String[] outStr = testOut.outString.get(sizeOfTestOut-1);
            
            if(testIn.sections2.size()==1){outStr[0] = testIn.ID+"_"+sizeOfTestOut; }
            else
            { testIn.sections2.forEach(sect -> {if(sect[0].equals(questions[1]))outStr[0] 
            = testIn.ID+"_"+Translit.cyr2lat(sect[2])+"_"+sizeOfTestOut; }); }
            outStr[1] = questions[2];
            
            switch(questions[3]){
                case "1":outStr[2]="multiple_choice";break;
                case "2":outStr[2]="multiple_response";break;
                case "5":outStr[2]="order";break;
            }
            
            outStr[3] = questions[2];
            outStr[4] = "1";
            outStr[5] = "";
            outStr[6] = "";
                        
            if(questions[3].equals("5")){
                TreeMap<Integer, String> tm = new TreeMap<>(); 
                for(String [] answers : testIn.answers2) {
                    if(answers[1].equals(questions[0]))
                    tm.put(parseInt(answers[7]),answers[2]);
                }
                int j=1;
                for(String ans : tm.values()){
                outStr[5] = outStr[5]+ans+"#";
                outStr[6] = outStr[6]+j+"#";
                j++;
                }
            }else {           
                int j=1;
                //int emptyAns=0;
                for(String [] answers : testIn.answers2) { 
                if(answers[1].equals(questions[0])){
                    outStr[5] = outStr[5]+answers[2]+"#";
                    if(!answers[6].equals("-1000")) outStr[6] = outStr[6]+j+"#";
                    j++;
                }
                
            }
                    if(j==1){
                    try (FileOutputStream fos = new FileOutputStream(errFile)){
                        String err = "Ошибка!: Вопрос "+questions+" будет удалён!\n";
                        fos.write(err.getBytes());
                        System.err.println("\"Ошибка!: Вопрос \"+testIn.questions2.get(del)[0]+\" будет удалён!\"");
                    } catch (IOException ex) {
                    Logger.getLogger(ImportData.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //testIn.questions2.remove(del);
                    return;
                    }
            
            }
            if(outStr[6].endsWith("#")) outStr[6] = outStr[6].substring(0, outStr[6].length()-1);
            if(outStr[5].endsWith("#")) outStr[5] = outStr[5].substring(0, outStr[5].length()-1);
            
                outStr[7]="random";
                outStr[8]="";
                outStr[9]="";
                outStr[10]="";
                outStr[11]="";
                outStr[12]="";
                outStr[13]="";
                outStr[14]="";
            
            //System.out.print(questions[3]+" ");
            System.out.printf("%s %.20s... %s %.20s... %s %s %s %s %s %s %s %s %s %s %s\n",outStr[0],outStr[1],outStr[2],outStr[3],outStr[4],outStr[5],outStr[6],outStr[7],
                    outStr[8],outStr[9],outStr[10],outStr[11],outStr[12],outStr[13],outStr[14]);
           });
    
    }
    
    void parseQuestions(File srcq, int iterTest){ //добавляем в класс Test вопросы
    System.out.println("Читаем файл "+srcq.getAbsoluteFile().getName()+"!");
    try (FileInputStream f = new FileInputStream(srcq); InputStreamReader is = new InputStreamReader(f, "Cp1251");)
    {
        BufferedReader br = new BufferedReader(is);
    //FileOutputStream fos = new FileOutputStream(new File(srcq.getAbsolutePath()+1));
    String tempStr="";
    String[] tempStrArr;
    
    Boolean f1=false; // { флаг начала строки
    Boolean f2=false; // | флаг разделитель
    int start=0;      // счётчик {
        
    int i;    
    //System.out.println("");
    while((i = br.read())!= -1){
    
    switch(i){                                  //Убираем перенос каретки
        case (13): break;                       //
        case (10): break;                       //
          default: tempStr +=(char)i;           //
                    //fos.write(i); break;
             }
        }
    tempStrArr = tempStr.split("\\|");
    int k=0;
    String[] strArr;
    Test tempTest = listTests.get(iterTest);
    for (String word : tempStrArr) {
    //word = new String(word.getBytes("UTF-8"),"Cp866");
        if(k%12==0)tempTest.questions2.add(new String[12]);
    tempTest.questions2.get(tempTest.questions2.size()-1)[k%12] = word;
    //System.out.print(word+"+++");
        
    k++;
    }
    
    }catch(IOException e){System.err.println(e);}
    System.out.println("В тест "+listTests.get(iterTest).ID+" загружены вопросы!\n");
    }

    void parseAnswers(File srcq, int iterTest){ //добавляем в класс Test ответы
    System.out.println("Читаем файл "+srcq.getAbsoluteFile().getName()+"!");
    try (FileInputStream f = new FileInputStream(srcq); InputStreamReader is = new InputStreamReader(f, "Cp1251")){
    BufferedReader br = new BufferedReader(is);
    //FileOutputStream fos = new FileOutputStream(new File(srcq.getAbsolutePath()+1));
    String tempStr="";
    String[] tempStrArr;
    
    Boolean f1=false; // { флаг начала строки
    Boolean f2=false; // | флаг разделитель
    int start=0;      // счётчик {
        
    int i;    
    //System.out.println(f.toString());
    while((i = br.read())!= -1){
    
    switch(i){                                  //Убираем перенос каретки
        case (13): break;                       //
        case (10): break;                       //
          default: tempStr +=(char)i;           //
                    //fos.write(i); break;
             }
        }
    tempStrArr = tempStr.split("\\|");
    int k=0;
    //String[] strArr;
    
    Test tempTest = listTests.get(iterTest);
    for (String word : tempStrArr) {
    if(k%8==0)tempTest.answers2.add(new String[8]);
    tempTest.answers2.get(tempTest.answers2.size()-1)[k%8] = word;
    //System.out.print(word+"+++");
        
    k++;
    }
    
    }catch(IOException e){System.err.println(e);}
    System.out.println("В тест "+listTests.get(iterTest).ID+" загружены ответы!\n");
    }

    void parseSections(File srcq, int iterTest){ //добавляем в класс Test секции вопросов
    System.out.println("Читаем файл "+srcq.getAbsoluteFile().getName()+"!");
    try (FileInputStream f = new FileInputStream(srcq); InputStreamReader is = new InputStreamReader(f, "Cp1251")){
    BufferedReader br = new BufferedReader(is);
    //FileOutputStream fos = new FileOutputStream(new File(srcq.getAbsolutePath()+1));
    String tempStr="";
    String[] tempStrArr;
    
    int i;    
    while((i = br.read())!= -1){
    
    switch(i){                                  //Убираем перенос каретки
        case (13): break;                       //
        case (10): break;                       //
          default: tempStr +=(char)i;           //
                    //fos.write(i); break;
             }
        }
    //System.out.println(listTests.get(listTests.size()-1).getID());
    //System.out.print(tempStr);
    //fos.close();    
    tempStrArr = tempStr.split("\\|");
    int k=0;
    //String[] strArr;
    Test tempTest = listTests.get(iterTest);
    for (String word : tempStrArr) {
    if(k%6==0)tempTest.sections2.add(new String[6]);
    tempTest.sections2.get(tempTest.sections2.size()-1)[k%6] = word;
    //System.out.print(word+"+++");
        
    k++;
    }
    
    }catch(IOException e){System.err.println(e);}
    System.out.println("В тест "+listTests.get(iterTest).ID+" загружены секции вопросов!\n");
    }

    void parseTestName(File srcq, int iterTest){ //добавляем в класс Test имя теста
    System.out.println("Читаем файл "+srcq.getAbsoluteFile().getName()+"!");
    try (FileInputStream f = new FileInputStream(srcq); InputStreamReader is = new InputStreamReader(f, "Cp1251")){
    BufferedReader br = new BufferedReader(is);
    //FileOutputStream fos = new FileOutputStream(new File(srcq.getAbsolutePath()+1));
    String tempStr="";
    String[] tempStrArr;
    
    Boolean f1=false; // { флаг начала строки
    Boolean f2=false; // | флаг разделитель
    int start=0;      // счётчик {
        
    int i;    
    while((i = br.read())!= -1){
    
    switch(i){                                  //Убираем перенос каретки
        case (13): break;                       //
        case (10): break;                       //
          default: tempStr +=(char)i;           //
                    //fos.write(i); break;
             }
        }
    //System.out.println(listTests.get(listTests.size()-1).getID());
    //System.out.print(tempStr);
    //fos.close();    
    tempStrArr = tempStr.split("\\|");
    int k=0;
    //String[] strArr;
    Test tempTest = listTests.get(iterTest);
    for (String word : tempStrArr) {
    //tempTest.nameArr = new String[9];
    tempTest.nameArr[k] = word;
    //System.out.print(word+"+++");
        
    k++;
    }
    
    }catch(IOException e){System.err.println(e);}
    System.out.println("В тест "+listTests.get(iterTest).ID+" загружены имя теста и параметры!\n");
    }
}

class TestOut {
    ArrayList<String[]> outString = new ArrayList<>();
    String name = "";
}


class Test {
    ArrayList<String[]> questions2 = new ArrayList<>();
    ArrayList<String[]> answers2 = new ArrayList<>();
    ArrayList<String[]> sections2 = new ArrayList<>();
    //String questions[][];
    //String answers[][];
    //String sections[][];
    String ID, name;
    String[] nameArr = new String[9];
        
    public Test(String ID){
    this.ID = ID;
    }
    
    public void setQuestions(int i, int j, String data){
    this.questions2.get(i)[j] = data;
    }
    public void setAnswers(int i, int j, String data){
    this.answers2.get(i)[j] = data;
    }
    public void setSections(int i, int j, String data){
    this.sections2.get(i)[j] = data;
    }
    public void setID(String data){
    this.ID = data;
    }
    public void setName(String data){
    this.name = data;
    }
    
    public String getQuestions(int i, int j){
    return this.questions2.get(i)[j];
    }
    public String getAnswers(int i, int j){
    return this.answers2.get(i)[j];
    }
    public String getSections(int i, int j){
    return this.sections2.get(i)[j];
    }
    public String getID(){
    return this.ID;
    }
    public String getName(){
    return this.name;
    }
}
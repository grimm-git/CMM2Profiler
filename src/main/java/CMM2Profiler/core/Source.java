/*
 * Copyright (C) 2026 grimm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package CMM2Profiler.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the source code of a MMBasic program.
 * The complete source include all include files will be read and put into a
 * single array:   0  main
 *                 1  include #1
 *                 2  include #2
 *                 3    :
 *                 n  include #n
 * 
 * The array IncludeFiles contain a register of all nnclude files and where
 * they are located in the source array
 * 
 * @author Matthias Grimm
 */
public class Source
{
    /**
     * <ul>
     * <li>NODATA
     * - Nothing has been loaded, filed didn't exist
     * <li>SOURCEONLY
     * - Only the source code is loaded, no profiler data
     * <li>PROFILERONLY
     * - Only the profiler data is loaded, no detailed source
     * <li>SOURCEANDPROFILER
     * - Both, source and profiler data is loded, full analyse capabilities.
     * </ul>
     */
    public enum Mode {NODATA, SOURCEONLY, PROFILERONLY, SOURCEANDPROFILER}
    
    private final ArrayList<SourceLine> SourceLines = new ArrayList<>();
    private final LinkedHashMap<String,SourceFile> StructureMap = new LinkedHashMap<>();
    private final ArrayList<Function> FunctionList = new ArrayList<>();
    
    private int codeLineNo;
    private Mode codeMode;
    
    public Source()
    {
        codeMode = Mode.NODATA;
    }
    
    public SourceFile             getSourceFile(String name) { return StructureMap.get(name); }
    public Set<String>            getSourceFileNames()       { return StructureMap.keySet(); }
    public ArrayList<Function>    getFunctionList()          { return FunctionList; }
    public Collection<SourceFile> getStructureMap()          { return StructureMap.values(); }
    public SourceLine             getSourceLine(int idx)     { return SourceLines.get(idx); }
    public int                    getSourceLineCnt()         { return SourceLines.size();  }
    public Mode                   getMode()                  { return codeMode; }
    
    public Mode load(String base, String path) throws IOException
    {
        File fh;

        FunctionList.clear();
        StructureMap.clear();
        SourceLines.clear();
        codeLineNo=0;

        fh=new File(base, path+".bas");
        if (fh.isFile()) {
            loadSource(base, path+".bas");
            codeMode = Mode.SOURCEONLY;
            fh=new File(base, path+".csv");
            if (fh.isFile()) {
                loadProfilerLogOnSource(fh);
                codeMode = Mode.SOURCEANDPROFILER;
            }
        } else {
            fh=new File(base, path+".csv");
            if (fh.isFile()) {
                loadProfilerLog(fh);
                codeMode = Mode.PROFILERONLY;
            }
        }
        
        cleanupSourceLines();
        extractFunctionReferences();
        return codeMode;
    }
    
    private void loadSource(String base, String path) throws IOException
    {
        int mainLines = loadSourceFile(base, path);  // load main source file
        SourceFile src=new SourceFile(path,0,mainLines-1);
        StructureMap.put("Main Program", src);
        
        int lineno=mainLines;
        for (int n=0 ; n < mainLines ; n++) {
            String codeLine = SourceLines.get(n).getSource();
            if (codeLine.length() < 12) continue;
            String temp = codeLine.substring(0, 8).toUpperCase(Locale.US);
            if (temp.equals("#INCLUDE")) {
                int a = codeLine.indexOf('"')+1;
                int b = codeLine.indexOf('"', a);
                String name = codeLine.substring(a,b);
                int cnt=loadSourceFile(base,name);    // load include file
                src=new SourceFile(name,lineno,lineno+cnt-1);
                StructureMap.put(name, src);
                lineno += cnt;
            }
        }
    }
    
    public int loadSourceFile(String base, String path) throws IOException
    {
        BufferedReader reader;
        String line;
        int level=0;
        int cnt=0;

        File fh=new File(base, path);
        InputStream iStream = new FileInputStream(fh);    
        reader = new BufferedReader(new InputStreamReader(iStream));
          
        line = reader.readLine();
        while (line != null) {
            SourceLine srcLine = new SourceLine(line);
            level = srcLine.setLevel(level);
            codeLineNo = srcLine.setLineNo(codeLineNo);
            SourceLines.add(srcLine);
            cnt++;
            
            line = reader.readLine();
        }
        reader.close();
        return cnt;
    }
 
    private void loadProfilerLogOnSource(File fh) throws IOException
    {
        int lineno;
        
        try (BufferedReader reader = openProfileReader(fh)) {
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                String name = parts[parts.length-2];
                if (name.isEmpty()) name="Main Program";
                SourceFile obj=StructureMap.get(name);
                if (obj == null) throw new IOException("unknown source file \""+name+"\"");
                
                lineno = convertInt(parts[parts.length-1])-1;
                lineno += obj.getFirstLine();
                
                SourceLine source = getSourceLine(lineno);
                source.setCalls(convertInt(parts[0]));
                source.setTime(convertFloat(parts[1]));
                
                line = reader.readLine();
            }
        }
    }

    private void loadProfilerLog(File fh) throws IOException
    {
        LinkedHashMap<String,ArrayList<SourceLine>> SMap = new LinkedHashMap<>();
        
        try (BufferedReader reader = openProfileReader(fh)) {
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                String name = parts[parts.length-2];
                if (name.isEmpty()) name="Main Program";
                
                ArrayList<SourceLine> SFile = SMap.get(name);
                if (SFile == null) {
                    SFile = new ArrayList<SourceLine>();
                    SMap.put(name, SFile);
                }
               
                String codeLine = parts[2].trim().replaceAll("^\\\"|\\\"$", "");
                SourceLine srcLine = new SourceLine(codeLine);
                srcLine.setCalls(convertInt(parts[0]));
                srcLine.setTime(convertFloat(parts[1]));
                codeLineNo = srcLine.setLineNo(codeLineNo);
                SFile.add(srcLine);
                
                line = reader.readLine();
            }
        }
        
        int lineno=0;
        for (Entry<String,ArrayList<SourceLine>> item : SMap.entrySet()) {
            String name = item.getKey();
            ArrayList<SourceLine> srcList = item.getValue();
            
            SourceFile sFile = new SourceFile(name,lineno,lineno+srcList.size()-1);
            StructureMap.put(name, sFile);
            SourceLines.addAll(srcList);
            lineno += srcList.size();
        }
    }

    /**
     * Open a buffered reader for the profiler data, verifies the file format
     * and returns the reader, if everything is in order.
     * 
     * @param fh Initialized file handle
     * @return BufferdReader object
     * @throws IOException for "File not found" and "Bad file formet"
     */    
    private BufferedReader openProfileReader(File fh) throws IOException
    {
        InputStream iStream = new FileInputStream(fh);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
            
        // Read header and verify file format
        String line = reader.readLine();
        if (line.charAt(0)=='/')
            line=line.substring(1);
        String [] parts = line.split("/");
        if (parts.length != 3) throw new IOException("Bad file format");
        if (!parts[2].endsWith(".bas")) throw new IOException("Bad file format");

        return reader;
    }
    
    private void extractFunctionReferences()
    {
        for (Function func : FunctionList) {
            Pattern regex = Pattern.compile("(?:^|[\\+\\-,=\\s])"+Pattern.quote(func.getName())+"(?=(?:[ (:]|$))", Pattern.CASE_INSENSITIVE);
            for (SourceLine srcLine : SourceLines) {
                if (srcLine == func.getData()) continue;
                
                Matcher m = regex.matcher(srcLine.getSource());
                if (m.find())
                   func.addReference(srcLine);
            }
        }
    }
    
    private void cleanupSourceLines()
    {
        int lineno=-1;
        boolean catchNext=false;
        boolean lastWasEmpty=true;
        SourceLine includeFile=null;
        SourceLine curFunction=null;
        Iterator<SourceLine> iter=SourceLines.iterator();

        int first=0, last=0;
        for (SourceFile srcFile : StructureMap.values()) {
            last=first+srcFile.getLastLine()-srcFile.getFirstLine();
            includeFile = srcFile.getSource();
            curFunction = srcFile.getSource();
            
            while(iter.hasNext()) {
                SourceLine srcLine = iter.next();
                if (srcLine.getType()==SourceLine.Type.COMMENT
                  || srcLine.getSource().toLowerCase(Locale.US).startsWith("#include")
                  || (lastWasEmpty && srcLine.getType()==SourceLine.Type.EMPTY)) {
                    iter.remove();
                    --last;
                } else {
                    lineno++;
                    lastWasEmpty = (srcLine.getType() == SourceLine.Type.EMPTY
                                 || srcLine.getType() == SourceLine.Type.COMMENT);

                    if (srcLine.isFunction()) {
                        FunctionList.add(new Function(srcLine));
                        // The function call has always an execution counter of one. So we look
                        // for the first command in the function to get the real execution counter
                        curFunction=srcLine;
                        catchNext=true;
                                                
                    } else {
                        int calls = srcLine.getCalls();
                        float time = srcLine.getTime();
                        
                        if (catchNext) {
                            if (calls > 0 || srcLine.isEndFunction()) {
                                // Here we copy the execution counter from the first
                                // command in the function to the function itself.
                                curFunction.setCalls(calls);
                                catchNext=false;
                            }
                        }
                        
                        if (curFunction != null) {
                            curFunction.addTime(time*calls);
                        
                            if (srcLine.isEndFunction()) {
                                calls = curFunction.getCalls();
                                time  = curFunction.getTime();
                                curFunction.setTime(calls==0 ? 0 : time/calls);
                                curFunction=includeFile;
                                curFunction.addTime(time);
                            }
                        }
                    }
                }
                if (lineno >= last) break;
            }

            srcFile.setFirstLine(first);
            srcFile.setLastLine(last);
            first=last+1;
        }
    }
    
    private int convertInt(String number)
    {
        try {
            return parseInt(number.trim());
        } catch (NumberFormatException ex) { /* EMPTY */ }

        return 0;
    }

    private float convertFloat(String number)
    {
        try {
            return parseFloat(number.trim());
        } catch (NumberFormatException ex) { /* EMPTY */ }
        
        try {  /* try european format */
            return parseFloat(number.replace('.', ','));
        } catch (NumberFormatException ex) { /* EMPTY */ }

        return 0;
    }
}

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
import java.util.Set;

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
    private String basePath;
    private final ArrayList<SourceLine> SourceLines = new ArrayList<>();
    private final LinkedHashMap<String,SourceFile> StructureMap = new LinkedHashMap<>();
    private final ArrayList<Function> FunctionList = new ArrayList<>();
    
    private int codeLineNo;
    
    public Source()
    {
        basePath="";
    }
    
    public SourceFile getSourceFile(String name)
    {
        return StructureMap.get(name);
    }
    
    public Set<String> getSourceFileNames()
    {
        return StructureMap.keySet();
    }
    
    public ArrayList<Function> getFunctionList()
    {
        return FunctionList;
    }

    public Collection<SourceFile> getStructureMap()
    {
        return StructureMap.values();
    }
    
    public SourceLine getSourceLine(int idx)
    {
        return SourceLines.get(idx);
    }
   
    public int getSourceLineCnt()
    {
        return SourceLines.size();
    }
    
    public void load(String base, String path) throws IOException
    {
        SourceFile src;
        int mainLines;
        int lineno;

        basePath=base;

        // Initialize Source for new code
        FunctionList.clear();
        StructureMap.clear();
        SourceLines.clear();
       
        // handle main source file
        mainLines = loadSourceFile(base, path+".bas");  // load main source file
        src=new SourceFile(path,0,mainLines-1);
        StructureMap.put("Main Program", src);
        
        lineno=mainLines;
        for (int n=0 ; n < mainLines ; n++) {
            String codeLine = SourceLines.get(n).getSource();
            if (codeLine.length() < 12) continue;
            String temp = codeLine.substring(0, 8).toUpperCase(Locale.US);
            if (temp.equals("#INCLUDE")) {
                int a = codeLine.indexOf('"')+1;
                int b = codeLine.indexOf('"', a);
                String name = codeLine.substring(a,b);
                int cnt=loadSourceFile(base,name);
                src=new SourceFile(name,lineno,lineno+cnt-1);
                StructureMap.put(name, src);
                lineno += cnt;
            }
        }
        
        loadProfilerLog(base,path+".csv");
        
        lineno=-1;
        boolean catchNext=false;
        boolean lastWasEmpty=true;
        SourceLine curFunction=null;
        Iterator<SourceLine> iter=SourceLines.iterator();

        int first=0, last=0;
        for (SourceFile srcFile : StructureMap.values()) {
            last=first+srcFile.getLastLine()-srcFile.getFirstLine();

            while(iter.hasNext()) {
                SourceLine srcLine = iter.next();
                if (srcLine.getType()==SourceLine.Type.COMMENT
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
                        
                    } else if (catchNext) {
                        int calls = srcLine.getCalls();
                        if (calls > 0 || srcLine.isEndFunction()) {
                            // Here we copy the execution counter from the first
                            // command in the function to the function itself.
                            curFunction.setCalls(calls);
                            catchNext=false;
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
 
    private void loadProfilerLog(String base, String path) throws IOException
    {
        BufferedReader reader;
        String line;
        String name;
        String[] parts;
        int lineno;
        
        File fh=new File(base, path);
        InputStream iStream = new FileInputStream(fh);
        reader = new BufferedReader(new InputStreamReader(iStream));
            
        // Read header and verify file format
        line = reader.readLine();
        if (line.charAt(0)=='/') line=line.substring(1);
        parts = line.split("/");
        if (parts.length != 3) throw new IOException("Bad file format");
        if (!parts[2].endsWith(".bas")) throw new IOException("Bad file format");

        line = reader.readLine();
        while (line != null) {
            parts = line.split(",");
            name = parts[parts.length-2];
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
        reader.close();
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

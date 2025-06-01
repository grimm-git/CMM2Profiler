/*
 * Copyright (C) 2025 grimm
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
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author grimm
 */
public class SourceFile
{
    private String filePath;
    private int lastLineNo;
    private final HashMap<Integer,String> SourceMap = new HashMap<>();
    
    public SourceFile()
    {
        filePath="";
    }
    
    public void load(String filePath, String fileName) throws IOException
    {
        BufferedReader reader;
        String line;
        int lineno;

        File fh=new File(filePath, fileName);
        InputStream iStream = new FileInputStream(fh);
        this.filePath=fileName;
            
        reader = new BufferedReader(new InputStreamReader(iStream));
          
        lineno=0;
        line = reader.readLine();
        while (line != null) {
            lineno++;
            if (!line.isBlank()) SourceMap.put(lineno, line.trim());
            line = reader.readLine();
        }
        reader.close();
        lastLineNo=lineno;
    }
    
    public boolean addLine(int no, String line)
    {
        if (this.SourceMap.containsKey(no) == false) {
            this.SourceMap.put(no, line);
            return true;
        }
        return false;
    }
    
    public HashMap<Integer,String> getSourceMap()
    {
        return SourceMap;
    }
    
    public String getSourceLine(int no)
    {
        return this.SourceMap.get(no);
    }

    public String getFilePath()
    {
        return filePath;
    }

    public int getLastLineNo()
    {
        return lastLineNo;
    }
    
    public String removeLine(int no)
    {
        return this.SourceMap.remove(no);
    }   
}

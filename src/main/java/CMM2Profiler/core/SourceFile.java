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

/**
 *
 * @author grimm
 */
public class SourceFile
{
    private String basePath;
    private String filePath;
    private final ArrayList<SourceLineData> SourceLineList = new ArrayList<>();
    
    public SourceFile()
    {
        basePath="";
        filePath="";
    }
    
    public void load(String base, String path) throws IOException
    {
        BufferedReader reader;
        String line;
        int lineno;

        File fh=new File(base, path);
        InputStream iStream = new FileInputStream(fh);
        basePath=base;
        filePath=path;
            
        reader = new BufferedReader(new InputStreamReader(iStream));
          
        lineno=1;
        line = reader.readLine();
        while (line != null) {
            SourceLineList.add(new SourceLineData(line, lineno++,0,0));
            line = reader.readLine();
        }
        reader.close();
    }

    public ArrayList<SourceLineData> getSourceList() { return SourceLineList; }
    
    public String getSourceLine(int no)
    {
        return no<SourceLineList.size() ? SourceLineList.get(no).getCodeLine() : "";
    }

    public String getBasePath() { return basePath; }
    public String getFilePath() { return filePath; }
    public int getLastLineNo() { return SourceLineList.size()-1; }
}

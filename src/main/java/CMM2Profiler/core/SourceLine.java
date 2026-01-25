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

import static CMM2Profiler.core.MMBasic.*;
import java.util.Locale;

/**
 * This class stores one line of source code, stripped. The comment has been
 * seperated to another field. A type categorized the contents.
 * @author Matthias Grimm
 */
public class SourceLine
{
    public enum Type {EMPTY,SOURCE,COMMENT,OPTION,DEFINE,INCLUDE,HEADER};
    
    private Type lineType;
    private int lineNo;
    private int lineLevel;
    private int lineCalls;
    private float lineTime;
    private String Source;
    private String Comment;

    public static SourceLine createSourceHeader(String text)
    {
        SourceLine header = new SourceLine();
        header.lineType = Type.HEADER;
        header.Source = text;
        return header;
    }
    
    private SourceLine()
    {
        lineNo=0;
        lineLevel=0;
        lineCalls=0;
        lineTime=0;
        lineType=SourceLine.Type.SOURCE;
        Source="";
        Comment="";
    }
    
    public SourceLine(String line)
    {
        this();
        
        String tmp;
        int pos;
        
        Source=line.trim();
        if (Source.isEmpty()) 
            lineType=SourceLine.Type.EMPTY;
        else {
            pos=line.indexOf(" '");
            if (pos==-1) pos=line.indexOf('\'');
            if (pos==0) {
                Comment=Source;
                Source="";
                lineType=SourceLine.Type.COMMENT;
            } else if (pos>0) {
                Comment=line.substring(pos).trim();
                Source=line.substring(0, pos-1).trim();
            }

            if (lineType==SourceLine.Type.SOURCE) {
                tmp=Source.toUpperCase(Locale.US);
                if (tmp.startsWith("OPTION"))
                    lineType=SourceLine.Type.OPTION;
                else if (tmp.startsWith("#DEFINE"))
                    lineType=SourceLine.Type.DEFINE;
                else if (tmp.startsWith("#INCLUDE"))
                    lineType=SourceLine.Type.INCLUDE;
            }
        }
    }

    public int setLineNo(int no)
    {
        lineNo=no;
        return isCodeLine() ? no+1 : no;
    }
    
    public int setLevel(int level)
    {
        String codeLine = Source.toLowerCase(Locale.US);
        lineLevel=level;
        
        if (codeLine.startsWith("function")
             || codeLine.startsWith("sub")
             || codeLine.startsWith("select case")
             || (codeLine.startsWith("do") && !isDoLoopOneliner(codeLine))
             || (codeLine.startsWith("for") && !isForNextOneliner(codeLine))
             || (codeLine.startsWith("if") && !isIfThenOneliner(codeLine))) {
            return level+1;
            
        } else if (codeLine.startsWith("else")
             || codeLine.startsWith("case ")) {
            lineLevel=level-1;
            
        } else if (codeLine.startsWith("end function")
             || codeLine.startsWith("end sub")
             || codeLine.startsWith("end select")
             || codeLine.startsWith("endif")
             || codeLine.startsWith("next")
             || codeLine.startsWith("loop")) {
            lineLevel=level-1;
            return level-1;
        }
        
        return level;
    }
    
    public boolean isCodeLine()
    {
        return (lineType==SourceLine.Type.SOURCE
             || lineType==SourceLine.Type.INCLUDE
             || lineType==SourceLine.Type.DEFINE);
    }

    public boolean isEmpty()
    {
        return lineType==SourceLine.Type.EMPTY;
    }

    public boolean isFunction()
    {
        String tmp = Source.toLowerCase(Locale.US);
        return tmp.startsWith("function ") || tmp.startsWith("sub ");
    }
    
    public boolean isEndFunction()
    {
        String tmp = Source.toLowerCase(Locale.US);
        return tmp.startsWith("end function") || tmp.startsWith("end sub");
    }

    public String getFunctionName()
    {
        int pos;
        String name="";
        
        String tmp = Source.toLowerCase(Locale.US);
        if (tmp.startsWith("function")) {
            pos = tmp.indexOf('(', 9);
            if (pos>=0)
                name = Source.substring(9,pos);
            
        } else if (tmp.startsWith("sub")) {
            pos = tmp.indexOf(' ', 4);
            if (pos==-1)
                pos=tmp.indexOf('(', 4);
            name = pos==-1 ? Source.substring(4) : Source.substring(4,pos);
        }
        return name.trim();
    }

    public SourceLine.Type getType() { return lineType; }
    public int getLineNo() { return lineNo; }
    public int getLevel() { return lineLevel; }
    public int getCalls() { return lineCalls; }
    public void setCalls(int calls) { lineCalls=calls; }
    public float getTime() { return lineTime; }
    public void setTime(float time) { lineTime=time; }
    public String getSource() { return Source; }
    public String getComment() { return Comment; }
    
}

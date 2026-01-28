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

import java.util.Comparator;
import java.util.Locale;

/**
 *
 * @author grimm
 */
public class Function
{
    public enum Type {SUB,FUNCTION};

    private Type type;
    private String name;
    private SourceLine data;
    
    public Function()
    {
        type = Type.SUB;
        name = "";
        data = null;
    }
    
    public Function(SourceLine srcLine)
    {
        if (srcLine.isFunction()) {
            data = srcLine;
            name = srcLine.getFunctionName();
            
            String tmp = srcLine.getSource().toLowerCase(Locale.US);
            type = tmp.startsWith("function") ? Type.FUNCTION : Type.SUB;
            
        } else
            throw new IllegalArgumentException("Source line is neither a Sub nor a Function.");
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public void setName(String funcname)    { name = funcname; }
    public String getName()                 { return name; }
    public void setData(SourceLine srcLine) { data = srcLine; }
    public SourceLine getData()             { return data; }
    public boolean isSub()                  { return type==Type.SUB; }
    public boolean isFunction()             { return type==Type.FUNCTION; }
    
    // ----------------------------------------------------------------------------------- 
    //                               Static class contents
    // ----------------------------------------------------------------------------------- 
    /**
     * Default comparator for sorting a list of Functions by name.
     */
    public final static Comparator<Function> CompName = (Function o1, Function o2) -> {
        String name1 = o1.toString().toLowerCase(Locale.getDefault());
        String name2 = o2.toString().toLowerCase(Locale.getDefault());
        //ascending order
        return name1.compareTo(name2);
    };

    public final static Comparator<Function> CompTime = (Function o1, Function o2) -> {
        float time1 = o2.getData().getTime();
        float time2 = o1.getData().getTime();
        
        //ascending order
        return Float.compare(time1, time2);
    };

    public final static Comparator<Function> CompCalls = (Function o1, Function o2) -> {
        int calls1 = o2.getData().getCalls();
        int calls2 = o1.getData().getCalls();
        
        //ascending order
        return Integer.compare(calls1, calls2);
    };
}

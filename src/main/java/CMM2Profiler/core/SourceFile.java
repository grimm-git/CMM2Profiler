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

/**
 *
 * @author Matthias Grimm
 */
public class SourceFile
{
    private final String path;
    private int firstline;    // in full source context
    private int lastline;
       
    public SourceFile(String path, int first, int last)
    {
        this.path=path;
        this.firstline=first;
        this.lastline=last;
    }
    
    public String getPath()              { return path;      }
    public int    getFirstLine()         { return firstline; }
    public void   setFirstLine(int line) { firstline = line; }
    public int    getLastLine()          { return lastline;  }
    public void   setLastLine(int line)  { lastline = line;  }
}

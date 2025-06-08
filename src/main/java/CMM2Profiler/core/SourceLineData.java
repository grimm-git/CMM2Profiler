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

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author grimm
 */
public class SourceLineData
{
    private final IntegerProperty slNo    = new SimpleIntegerProperty();
    private final StringProperty  slCode  = new SimpleStringProperty();
    private final IntegerProperty slCalls = new SimpleIntegerProperty();
    private final FloatProperty   slTime  = new SimpleFloatProperty();

    public SourceLineData()
    {
        slNo.set(0);
        slCode.set("");
        slCalls.set(0);
        slTime.set(0);
    }

    public SourceLineData(String line, Integer no, Integer calls, float time)
    {
        slNo.set(no);
        slCode.set(line.trim());
        slCalls.set(calls);
        slTime.set(time);
    }
    
    public int getLineNo() { return slNo.get(); }
    public void getLineNo(int value) { slNo.set(value); }

    public String getCodeLine() { return slCode.get(); }
    public void getCodeLine(String line) { slCode.set(line); }
    
    public int getNoOfCalls() { return slCalls.get(); }
    public void getNoOfCalls(int value) { slCalls.set(value); }
    
    public float getExecTime() { return slTime.get(); }
    public void getExecTime(float value) { slTime.set(value); }
    
    public boolean isCodeLine()
    {
        String line=slCode.get();
        return !(line.isEmpty() || line.startsWith("'"));
    }
    
    public IntegerProperty slNoProperty()    { return slNo; };
    public StringProperty  slCodeProperty()  { return slCode; };
    public IntegerProperty slCallsProperty() { return slCalls; }
    public FloatProperty   slTimeProperty()  { return slTime; }
}

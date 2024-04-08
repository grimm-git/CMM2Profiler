
package CMM2Profiler.core;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import java.util.Locale;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * This class contains one dataset from the CMM2 Profiler
 * 
 * @author grimm
 */
public class ProfilerData
{
    private final IntegerProperty callsCnt = new SimpleIntegerProperty();
    private final FloatProperty execTime = new SimpleFloatProperty();
    private final StringProperty codeLine = new SimpleStringProperty();
    private final StringProperty sourceFile = new SimpleStringProperty();
    private final IntegerProperty lineNo = new SimpleIntegerProperty();
   
    public ProfilerData()
    {
        callsCnt.set(0);
        execTime.set(0);
        codeLine.set("");
        sourceFile.set("");
        lineNo.set(0);
    }

    public ProfilerData(String line)
    {
        int n;
        String tmp;
        
        String[] parts=line.split(",");
        callsCnt.set(convertInt(parts[0]));
        execTime.set(convertFloat(parts[1]));

        tmp="";
        for (n=2;n<parts.length-2;n++) {
            if (n>2) tmp=tmp+",";
            tmp=tmp+parts[n];
        }
        tmp=tmp.trim();
        if (tmp.startsWith("\"")) tmp=tmp.substring(1);
        if (tmp.endsWith("\"")) tmp=tmp.substring(0, tmp.length()-1);
        codeLine.set(tmp);
                
        sourceFile.set(parts[parts.length-2]);
        lineNo.set(convertInt(parts[parts.length-1]));
    }
    
    public int getCallsCnt()
    {
        return callsCnt.get();
    }
    
    public void setCallsCnt(int val)
    {
        callsCnt.set(val);
    }

    public float getExecTime()
    {
        return execTime.get();
    }
    
    public void setExecTime(float val)
    {
        execTime.set(val);
    }

    public Float getCallsTime()
    {
        return execTime.get()*callsCnt.get();
    }
    
    public String getCodeline()
    {
        return codeLine.get();
    }

    public void setCodeLine(String arg)
    {
        codeLine.set(arg);
    }

    public String getSourceFile()
    {
        return sourceFile.get();
    }

    public void setSourceFile(String arg)
    {
        sourceFile.set(arg);
    }

    public int getLineNo()
    {
        return lineNo.get();
    }

    public void setLineNo(int val)
    {
        lineNo.set(val);
    }

    public boolean isFunction()
    {
        String tmp = codeLine.get().toLowerCase(Locale.US);
        return tmp.startsWith("function ") || tmp.startsWith("sub ");
    }
    
    public boolean isEndFunction()
    {
        String tmp = codeLine.get().toLowerCase(Locale.US);
        return tmp.startsWith("end function ") || tmp.startsWith("end sub ");
    }

    public boolean isMainCode()
    {
        return sourceFile.get().isEmpty();
    }

    public IntegerProperty callsCntProperty()   { return callsCnt; }
    public FloatProperty   execTimeProperty()   { return execTime; }
    public StringProperty  codeLineProperty()   { return codeLine; };
    public StringProperty  sourceFileProperty() { return sourceFile; };
    public IntegerProperty lineNoProperty()     { return lineNo; };
    
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

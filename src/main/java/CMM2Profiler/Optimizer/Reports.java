package CMM2Profiler.Optimizer;

import java.util.Locale;

public enum Reports
{
    INTRO("Introduction","Text/introduction.html"),
    VARIABLES("Variable length","Text/variable_length.html"),
    INC("a=a+b, a=a-b","Text/inc.html");

    private final static String REPORTFOLDER = "reports/";

    private String reportName;
    private String reportTemplate;

    Reports(String name, String template)
    {
        reportName=name;
        reportTemplate=template;
    }

    public static Reports findReports(String id)
    {
        for (Reports item : Reports.values())
            if (item.reportName.toLowerCase(Locale.US).equals(id.toLowerCase(Locale.US)))
                return item;
        return null;
    }

    public String getTemplate()
    {
        return REPORTFOLDER + reportTemplate;
    }

    public String toString()
    {
        return reportName;
    }
}

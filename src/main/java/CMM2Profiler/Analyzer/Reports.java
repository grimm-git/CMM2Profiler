package CMM2Profiler.Analyzer;

import java.util.Locale;
import java.util.function.Supplier;

public enum Reports
{
    INTRO    ("Introduction",    "Text/introduction.html",   ReportIntroduction::new),
    VARIABLES("Variable length",  "Text/variable_length.html", ReportVariables::new),
    INC      ("a=a+b, a=a-b",     "Text/increment.html",       ReportIncrement::new);

    private final static String REPORTFOLDER = "reports/";

    private final String reportName;
    private final String reportTemplate;
    private final Supplier<Report> reportFactory;

    Reports(String name, String template, Supplier<Report> factory)
    {
        reportName=name;
        reportTemplate=template;
        reportFactory=factory;
    }

    /**
     * Creates the report object that belongs to this type. Every report type
     * has to name its class in the constructor, so a new type cannot be added
     * without one.
     *
     * @return a new report object of the matching class
     */
    public Report createReport()
    {
        return reportFactory.get();
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

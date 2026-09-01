/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
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
package CMM2Profiler.Analyzer;

import CMM2Profiler.core.Source;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Report base class. Every report is inherited from this class.
 * It loads a HTML template, adds its style to the head of it and fills the
 * marked divs of the template with the results of its analysis.
 *
 * @author Matthias Grimm <codingjoker@web.de>
 */
public abstract class Report
{
    private StringBuilder ReportBuilder;

    private static final Pattern HEAD = Pattern.compile("<head", Pattern.CASE_INSENSITIVE);

    protected Report()
    {
        ReportBuilder = new StringBuilder();
    }

    /**
     * Builds the report. Every report scans the source code its own way, loads
     * its template and fills the marked divs of it. The result is picked up
     * with {@link #getReportHTML()}.<p>
     *
     * The method runs in a worker thread and must not touch the GUI.
     *
     * @param src      source code of a MMBasic program
     * @param template path of the template of this report
     * @throws IOException if the template cannot be read
     */
    public abstract void create(Source src, String template) throws IOException;

    public String getReportHTML()
    {
        return ReportBuilder.toString();
    }

    /**
     * Loads a template and prepares it for WebEngine.loadContent(). That method
     * hands the page to the browser without a document URL, so the relative
     * link to the style sheet of the template would not be resolved any more.
     * The base address puts that back.<p>
     *
     * The base address has to stand in front of the first relative link of the
     * page, so it goes to the very beginning of the head.
     *
     * @param template path of the template
     * @throws IOException if the template cannot be read
     */
    protected void load(String template) throws IOException
    {
        File file = new File(template);
        ReportBuilder.setLength(0); // clear ReportBuffer
        ReportBuilder.append(Files.readString(file.toPath()));

        injectHead("<base href=\"" + file.getParentFile().toURI() + "\"/>\n");
    }

    /**
     * Adds text at the very beginning of the head of a template.
     *
     * @param content text to add
     */
    protected void injectHead(String content)
    {
        Matcher m = HEAD.matcher(ReportBuilder);
        if (!m.find()) { ReportBuilder.insert(0, content); return; }

        int pos = ReportBuilder.indexOf(">", m.start());   // plain indexOf is fine, '>' has no case
        if (pos == -1) { ReportBuilder.insert(0, content); return; }

        ReportBuilder.insert(pos+1, "\n" + content);
    }

    /**
     * Fills one of the marked divs of a template. A template that does not
     * carry the div is left alone, so a template may show a part of the report
     * only.
     *
     * @param id      id of the div to fill
     * @param content HTML to put into the div
     */
    protected void inject(String id, String content)
    {
        Pattern marker = Pattern.compile("<div\\s+id=['\"]"+Pattern.quote(id)+"['\"]\\s*>\\s*</div>",
                                         Pattern.CASE_INSENSITIVE);

        Matcher m = marker.matcher(ReportBuilder);
        if (m.find())
            ReportBuilder.replace(m.start(), m.end(), "<div id=\""+id+"\">"+content+"</div>");
    }

    /**
     * @param text any text
     * @return the text with the HTML special characters replaced
     */
    protected String escape(String text)
    {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}

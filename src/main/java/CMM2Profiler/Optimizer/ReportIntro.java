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
package CMM2Profiler.Optimizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import CMM2Profiler.core.Source;

/**
 * @author Matthias Grimm <codingjoker@web.de>
 */
public class ReportIntro
{
    public ReportIntro() { }

    /**
     * Dummy report to give the user some introduction to the Report system.<p>
     *
     * The method runs in a worker thread and must not touch the GUI.
     *
     * @param src      source code of a MMBasic program
     * @param template path of the template, relative to the report folder
     * @return the finished report as one HTML string
     * @throws IOException if the template cannot be read
     */
    public String create(Source src, String template) throws IOException
    {
        File file = new File(template);
        String html = Files.readString(file.toPath());

        // loadContent() hands the page to the browser without a document URL,
        // so the relative link to the style sheet of the template would not be
        // resolved any more. The base address puts that back.
        html = injectHead(html, "<base href=\"" + file.getParentFile().toURI() + "\"/>\n");
        return html;
    }

    /**
     * Adds text at the very beginning of the head of the template. The base
     * address has to stand in front of the first relative link of the page,
     * otherwise the browser resolves that link without it and the style sheet
     * of the template is not found.
     *
     * @param html    the template
     * @param content text to add
     * @return the template with the extended head
     */
    private String injectHead(String html, String content)
    {
        int pos = html.toLowerCase(Locale.US).indexOf("<head");
        if (pos == -1) return content+html;

        pos = html.indexOf('>', pos);
        if (pos == -1) return content+html;

        return html.substring(0, pos+1) + "\n" + content + html.substring(pos+1);
    }

}

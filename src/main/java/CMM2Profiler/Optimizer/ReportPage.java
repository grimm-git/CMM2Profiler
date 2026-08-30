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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for the report classes. Every report loads a HTML template, adds its
 * style to the head of it and fills the marked divs of the template with the
 * results of its analysis. This class holds the part of that work which is the
 * same for all of them.
 *
 * @author Matthias Grimm
 */
public class ReportPage
{
    private ReportPage() { /* static class */ }

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
     * @param style    style of the generated blocks, added to the head
     * @return the template, ready to be filled
     * @throws IOException if the template cannot be read
     */
    public static String load(String template, String style) throws IOException
    {
        File file = new File(template);
        String html = Files.readString(file.toPath());

        return injectHead(html, "<base href=\"" + file.getParentFile().toURI() + "\"/>\n" + style);
    }

    /**
     * Adds text at the very beginning of the head of a template.
     *
     * @param html    the template
     * @param content text to add
     * @return the template with the extended head
     */
    public static String injectHead(String html, String content)
    {
        int pos = html.toLowerCase(Locale.US).indexOf("<head");
        if (pos == -1) return content+html;

        pos = html.indexOf('>', pos);
        if (pos == -1) return content+html;

        return html.substring(0, pos+1) + "\n" + content + html.substring(pos+1);
    }

    /**
     * Fills one of the marked divs of a template. A template that does not
     * carry the div is left alone, so a template may show a part of the report
     * only.
     *
     * @param html    the template
     * @param id      id of the div to fill
     * @param content HTML to put into the div
     * @return the template with the filled div
     */
    public static String inject(String html, String id, String content)
    {
        Pattern marker = Pattern.compile("<div\\s+id=['\"]"+Pattern.quote(id)+"['\"]\\s*>\\s*</div>",
                                         Pattern.CASE_INSENSITIVE);

        Matcher m = marker.matcher(html);
        if (!m.find()) return html;

        return m.replaceFirst(Matcher.quoteReplacement("<div id=\""+id+"\">"+content+"</div>"));
    }

    /**
     * @param text any text
     * @return the text with the HTML special characters replaced
     */
    public static String escape(String text)
    {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}

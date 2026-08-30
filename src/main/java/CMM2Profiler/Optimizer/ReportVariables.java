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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static CMM2Profiler.core.MMBasic.*;
import CMM2Profiler.core.Source;
import CMM2Profiler.core.SourceLine;

/**
 * Collects the names of all variables, subs and functions of a MMBasic program
 * and sorts them into three groups. MMBasic looks variables up by their name,
 * so the length of a name has a direct influence on the execution speed. The
 * histograms of {@link NameList} show where shortening names pays off.<p>
 *
 * The scanner classifies a name as follows:
 * <ul>
 * <li>LOCAL and STATIC declarations and the parameters of a sub or a function
 *     are local. Parameters are not marked by a keyword but they are local to
 *     the routine nevertheless, and they are assigned to like any other local
 *     variable.
 * <li>DIM and CONST declarations are global. CONST is included because a
 *     constant occupies a slot in the variable table of the interpreter and is
 *     looked up by its name just like a variable.
 * <li>Every other name at the beginning of a statement that is followed by an
 *     "=" is global, unless it is a local variable of the current routine.
 * </ul>
 *
 * The scan runs in two passes. The first pass sorts the names into the three
 * groups, the second pass counts how often every name is used. Counting needs
 * its own pass because a name may be used before the line that declares it, and
 * because the same name may be a local variable in one routine and a global
 * variable everywhere else. The second pass therefore repeats the scope
 * tracking of the first one.<p>
 *
 * Two kinds of assignments are deliberately dropped: the assignment of the
 * return value of a function, which uses the name of the function and not the
 * name of a variable, and the read only system variables of the MM.* family.<p>
 *
 * The type suffixes "$", "%" and "!" are type markers and not part of the name,
 * so they are removed. Otherwise "x2" and "x2!" would be counted as two
 * variables and the histogram would show a name length that is one character
 * too long.
 *
 * @author Matthias Grimm
 */
public class ReportVariables
{
    private final NameList globals  = new NameList("Global Variables");
    private final NameList locals   = new NameList("Local Variables");
    private final NameList routines = new NameList("Subs and Functions");

    /** Local variables and parameters of every routine, lower case */
    private final LinkedHashMap<String,HashSet<String>> scopeMap = new LinkedHashMap<>();

    /** Local variables and parameters of the routine currently scanned, lower case */
    private HashSet<String> scopeLocals = new HashSet<>();

    /** Name of the routine currently scanned, empty outside of a sub or function */
    private String scopeName = "";

    /** Leading name of a declaration or of an assignment target */
    private static final Pattern IDENT =
            Pattern.compile("^([A-Za-z_][A-Za-z_0-9.]*[$%!]?)");

    /** A complete assignment target, with an optional array subscript */
    private static final Pattern TARGET =
            Pattern.compile("^([A-Za-z_][A-Za-z_0-9.]*[$%!]?)\\s*(\\(.*\\))?$");

    /** Type of a DIM, LOCAL, STATIC or CONST declaration */
    private static final Pattern TYPE =
            Pattern.compile("^(integer|float|string)\\b\\s*", Pattern.CASE_INSENSITIVE);

    /** Parameter passing mode, in front of the parameter name */
    private static final Pattern PASSING =
            Pattern.compile("^(byref|byval)\\b\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * CSS class of a clickable variable name in a report. The controller looks
     * for this class to recognise a click on a name, so both sides have to use
     * the same string.
     */
    public final static String VAR_CLASS = "var";

    /**
     * Values of the data-list attribute of a clickable name. The same name may
     * be a local variable in one routine and a global one everywhere else, so
     * the group has to travel with the name to find the right entry again.
     */
    public final static String LIST_GLOBAL  = "global";
    public final static String LIST_LOCAL   = "local";
    public final static String LIST_ROUTINE = "routine";

    /** Geometry of the SVG histograms, all values in pixels */
    private final static int BAR_WIDTH    = 26;
    private final static int PLOT_HEIGHT  = 180;
    private final static int MARGIN_TOP   = 18;
    private final static int MARGIN_LEFT  = 40;
    private final static int MARGIN_RIGHT = 14;
    private final static int MARGIN_FOOT  = 36;
    private final static int GRIDLINES    = 4;

    /** The counter axis ends on a multiple of this value */
    private final static int AXIS_UNIT    = 5;

    /** Style of the generated blocks, added to the head of the template */
    private final static String REPORTSTYLE =
          "<style type=\"text/css\">\n"
        + "svg.histogram { display:block; margin:4px auto 16px auto; }\n"
        + "p.summary { color:#555; font-size:90%; margin:6px 0 8px 0; }\n"
        + "table.varlist { width:100%; table-layout:fixed; border:0; border-collapse:collapse;"
        + " border:0; font-size:90%; margin-bottom:24px; }\n"
        + "table.varlist td { border:0; padding:1px 8px 1px 0; white-space:nowrap;"
        + " overflow:hidden; text-overflow:ellipsis; }\n"
        + "table.lenlist { width:100%; border-collapse:collapse; border:0;"
        + " font-size:90%; margin-bottom:24px; }\n"
        + "table.lenlist td { border:0; padding:2px 8px 2px 0; vertical-align:top; }\n"
        + "table.lenlist td.len { width:3em; text-align:right; font-weight:bold;"
        + " white-space:nowrap; }\n"
        + "span.var { cursor:pointer; color:#2a6099; }\n"
        + "span.var:hover { text-decoration:underline; }\n"
        + "</style>\n";

    public ReportVariables()  { }

    /**
     * Builds the complete report. The source code is scanned, the template is
     * loaded and every marked div of the template is filled with a histogram or
     * with a table of names. The result is a self contained HTML page, ready to
     * be handed to WebEngine.loadContent().<p>
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
        extract(src);

        String html = ReportPage.load(template, REPORTSTYLE);


        html = ReportPage.inject(html, "histogram_global",   buildHistogram(globals));
        html = ReportPage.inject(html, "varlist_global",     buildLengthTable(globals, LIST_GLOBAL));
        html = ReportPage.inject(html, "histogram_local",    buildHistogram(locals));
        html = ReportPage.inject(html, "varlist_local",      buildLengthTable(locals, LIST_LOCAL));
        html = ReportPage.inject(html, "histogram_routines", buildHistogram(routines));
        html = ReportPage.inject(html, "varlist_routines",   buildLengthTable(routines, LIST_ROUTINE));

        return html;
    }

    /**
     * Draws the histogram of the name lengths of one group as a SVG bar chart.
     * Name lengths that do not occur keep their place on the axis, so the shape
     * of the distribution stays visible.
     *
     * @param list one of the three name groups
     * @return heading, summary and the SVG chart
     */
    private String buildHistogram(NameList list)
    {
        StringBuilder svg = new StringBuilder();

        svg.append("<p class=\"summary\">").append(list.size()).append(" names, ")
           .append(list.getTotalCount()).append(" usages, longest name ")
           .append(list.getMaxLength()).append(" characters</p>\n");

        TreeMap<Integer,Integer> histogram = list.getHistogram();
        if (histogram.isEmpty())
            return svg.append("<p>No names of this kind were found.</p>\n").toString();

        int first = histogram.firstKey();
        int last  = histogram.lastKey();
        int bins  = last-first+1;

        int maxCount=0;
        for (int count : histogram.values())
            if (count > maxCount) maxCount = count;

        int axisMax = niceAxisMax(maxCount);

        int width  = MARGIN_LEFT + bins*BAR_WIDTH + MARGIN_RIGHT;
        int height = MARGIN_TOP + PLOT_HEIGHT + MARGIN_FOOT;
        int base   = MARGIN_TOP + PLOT_HEIGHT;

        svg.append("<svg class=\"histogram\" xmlns=\"http://www.w3.org/2000/svg\" width=\"")
           .append(width).append("\" height=\"").append(height)
           .append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");

        // horizontal grid lines and the scale of the counter axis
        for (int n=0 ; n <= GRIDLINES ; n++) {
            int y = base - n*PLOT_HEIGHT/GRIDLINES;

            svg.append("<line x1=\"").append(MARGIN_LEFT).append("\" y1=\"").append(y)
               .append("\" x2=\"").append(width-MARGIN_RIGHT).append("\" y2=\"").append(y)
               .append("\" stroke=\"#d0d0d0\"/>\n")
               .append("<text x=\"").append(MARGIN_LEFT-6).append("\" y=\"").append(y+4)
               .append("\" text-anchor=\"end\" font-size=\"10\" fill=\"#666\">")
               .append(n*axisMax/GRIDLINES).append("</text>\n");
        }

        for (int len=first ; len <= last ; len++) {
            Integer entry = histogram.get(len);
            int count = entry==null ? 0 : entry;
            int x     = MARGIN_LEFT + (len-first)*BAR_WIDTH;

            if (count > 0) {
                // A length that occurs at all deserves at least one pixel,
                // otherwise rare name lengths would vanish from the chart.
                int bar = count*PLOT_HEIGHT / axisMax;
                if (bar == 0) bar = 1;

                svg.append("<rect x=\"").append(x+3).append("\" y=\"").append(base-bar)
                   .append("\" width=\"").append(BAR_WIDTH-6).append("\" height=\"").append(bar)
                   .append("\" fill=\"#4a7ebb\"/>\n")
                   .append("<text x=\"").append(x+BAR_WIDTH/2).append("\" y=\"").append(base-bar-4)
                   .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#333\">")
                   .append(count).append("</text>\n");
            }

            svg.append("<text x=\"").append(x+BAR_WIDTH/2).append("\" y=\"").append(base+15)
               .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#333\">")
               .append(len).append("</text>\n");
        }

        svg.append("<line x1=\"").append(MARGIN_LEFT).append("\" y1=\"").append(base)
           .append("\" x2=\"").append(width-MARGIN_RIGHT).append("\" y2=\"").append(base)
           .append("\" stroke=\"#888\"/>\n")
           .append("<text x=\"").append(MARGIN_LEFT + bins*BAR_WIDTH/2).append("\" y=\"")
           .append(height-6).append("\" text-anchor=\"middle\" font-size=\"11\" fill=\"#555\">")
           .append("length of the name</text>\n")
           .append("</svg>\n");

        return svg.toString();
    }

    /**
     * Rounds the top of the counter axis up, so that every grid line carries a
     * round number. The distance between two grid lines is a multiple of five,
     * which makes the labels multiples of five or of ten.
     *
     * @param maxCount highest counter of the histogram
     * @return top value of the counter axis, a multiple of GRIDLINES*AXIS_UNIT
     */
    private int niceAxisMax(int maxCount)
    {
        int step = (maxCount + GRIDLINES-1) / GRIDLINES;      // round up
        step = (step + AXIS_UNIT-1) / AXIS_UNIT * AXIS_UNIT;  // round up again

        return step * GRIDLINES;
    }

    /**
     * Builds a table that groups the names of one group by their length. The
     * first column holds the number of characters, the second one the names of
     * that length as a comma separated list. The longest names come first, so
     * the entries worth shortening stand at the top.
     *
     * @param list  one of the three name groups
     * @param group  value of the data-list attribute of the names
     * @return the table as HTML
     */
    private String buildLengthTable(NameList list, String group)
    {
        if (list.isEmpty()) return "";

        // Walking the names in alphabetical order builds every line of the
        // table in alphabetical order as well.
        TreeMap<Integer,StringBuilder> byLength = new TreeMap<>();

        for (Variable entry : list.getSorted(Variable.CompName)) {
            StringBuilder line = byLength.get(entry.getLength());
            if (line == null) {
                line = new StringBuilder();
                byLength.put(entry.getLength(), line);
            }

            if (line.length() > 0) line.append(", ");
            line.append(nameSpan(entry.getName(), group));
        }

        StringBuilder html = new StringBuilder();
        html.append("<table class=\"lenlist\">\n");

        for (int length : byLength.descendingKeySet())
            html.append("<tr><td class=\"len\">").append(length)
                .append("</td><td>").append(byLength.get(length))
                .append("</td></tr>\n");

        return html.append("</table>\n").toString();
    }

    /**
     * Wraps a name into a clickable element. The name is carried in a data
     * attribute as well, so the controller does not have to parse the text of
     * the element.
     *
     * @param name  name of a variable, a sub or a function
     * @param group name group the name belongs to
     * @return the name as a HTML element
     */
    private String nameSpan(String name, String group)
    {
        String text = ReportPage.escape(name);
        return "<span class=\""+VAR_CLASS+"\" data-list=\""+group+"\" data-name=\""+text
             + "\">"+text+"</span>";
    }

    /**
     * Looks a name up again after it was clicked in the report. The group is
     * needed because the same name may sit in two lists at once.
     *
     * @param group one of LIST_GLOBAL, LIST_LOCAL or LIST_ROUTINE
     * @param name  name of a variable, a sub or a function
     * @return the entry or null, if the name is unknown
     */
    public Variable findVariable(String group, String name)
    {
        if (name == null || group == null) return null;

        switch (group) {
            case LIST_GLOBAL:  return globals.get(name);
            case LIST_LOCAL:   return locals.get(name);
            case LIST_ROUTINE: return routines.get(name);
            default:           return null;
        }
    }


    public NameList getGlobals()  { return globals;  }
    public NameList getLocals()   { return locals;   }
    public NameList getRoutines() { return routines; }

    /**
     * Scans the whole source code and fills the three name lists. Previous
     * results are discarded, so the method may be called again after a new
     * program has been loaded.
     *
     * @param src source code of a MMBasic program
     */
    public final void extract(Source src)
    {
        globals.clear();
        locals.clear();
        routines.clear();
        scopeMap.clear();
        closeScope();

        for (int n=0 ; n < src.getSourceLineCnt() ; n++) {
            SourceLine srcLine = src.getSourceLine(n);

            // Comments, empty lines, OPTION, #DEFINE and #INCLUDE carry no
            // variables. Source.load() has already removed the comments.
            if (srcLine.getType() != SourceLine.Type.SOURCE) continue;

            String code = srcLine.getSource();
            if (code.isEmpty()) continue;

            if (srcLine.isFunction())    { openScope(srcLine); continue; }
            if (srcLine.isEndFunction()) { closeScope();       continue; }

            for (String statement : splitStatements(code))
                scanStatement(statement);
        }
        closeScope();

        // MMBasic keeps variables, subs and functions in one namespace, so a
        // name cannot be a routine and a variable at the same time. A routine
        // that is written to like an array has been filed as a global variable
        // by the first pass and is taken out again here.
        for (Variable entry : routines.getNames())
            globals.remove(entry.getName());

        countUsage(src);
    }

    /**
     * Second pass. Counts how often every name of the three lists is used. Every
     * appearance in the code counts, the declaration of a name as well as every
     * read and write access to it. Names inside comments and inside string
     * constants do not count, and neither do numbers.
     *
     * @param src source code of a MMBasic program
     */
    private void countUsage(Source src)
    {
        globals.clearReferences();
        locals.clearReferences();
        routines.clearReferences();
        closeScope();

        for (int n=0 ; n < src.getSourceLineCnt() ; n++) {
            SourceLine srcLine = src.getSourceLine(n);
            if (srcLine.getType() != SourceLine.Type.SOURCE) continue;

            String code = srcLine.getSource();
            if (code.isEmpty()) continue;

            if (srcLine.isEndFunction()) { closeScope(); continue; }
            if (srcLine.isFunction())    enterScope(extractName(srcLine.getFunctionName()));

            for (String name : splitNames(code))
                countName(name, srcLine);
        }
        closeScope();
    }

    /**
     * Books one usage of a name. A name that belongs to none of the three lists
     * is a keyword or a function of MMBasic itself and is ignored.
     *
     * @param text    name as it was found in the source code
     * @param srcLine source line the name was found in
     */
    private void countName(String text, SourceLine srcLine)
    {
        String name = extractName(text);
        if (name.isEmpty()) return;

        String key = name.toLowerCase(Locale.US);
        Variable entry;

        // A local variable of the current routine hides a global one of the
        // same name, so the local list has to be asked first.
        if (scopeLocals.contains(key)) entry = locals.get(key);
        else if ((entry=routines.get(key)) == null) entry = globals.get(key);

        if (entry != null) entry.addReference(srcLine);
    }

    /**
     * Splits a line of code into the names it uses. String constants, numbers
     * and the &amp;H, &amp;O and &amp;B constants of MMBasic hold no names.
     *
     * @param code one line of source code, without its comment
     * @return every name of the line, in the order of appearance
     */
    private ArrayList<String> splitNames(String code)
    {
        ArrayList<String> names = new ArrayList<>();
        int pos=0;

        while (pos < code.length()) {
            char chr = code.charAt(pos);

            if (chr=='"') {                          // string constant
                while (++pos < code.length() && code.charAt(pos)!='"') { /* EMPTY */ }
                pos++;

            } else if (chr=='&') {                   // hex, octal or binary constant
                while (++pos < code.length() && isNameChar(code.charAt(pos))) { /* EMPTY */ }

            } else if (Character.isDigit(chr)) {     // decimal constant, 1.5e3 as well
                while (pos < code.length() && isNameChar(code.charAt(pos))) pos++;

            } else if (Character.isLetter(chr) || chr=='_') {
                int start=pos;
                while (pos < code.length() && isNameChar(code.charAt(pos))) pos++;
                if (pos < code.length() && "$%!".indexOf(code.charAt(pos)) != -1) pos++;
                names.add(code.substring(start, pos));

            } else
                pos++;
        }
        return names;
    }

    /**
     * A new sub or function starts. Its name goes into the list of routines and
     * its parameters are the first local variables of the new scope.
     *
     * @param srcLine source line holding the SUB or FUNCTION statement
     */
    private void openScope(SourceLine srcLine)
    {
        closeScope();

        String code = srcLine.getSource();
        enterScope(extractName(srcLine.getFunctionName()));
        if (!scopeName.isEmpty()) routines.add(scopeName);

        int open = code.indexOf('(');
        if (open == -1) return;

        int close = findClosingParen(code, open);
        if (close == -1) return;

        for (String param : splitTopLevel(code.substring(open+1, close), ','))
            addLocal(param);
    }

    /**
     * Makes the given routine the current scope. In the first pass the set of
     * local names is empty and is filled while the routine is scanned, in the
     * second pass the set of the first pass is picked up again.
     *
     * @param name name of the sub or function
     */
    private void enterScope(String name)
    {
        scopeName = name;

        String key = name.toLowerCase(Locale.US);
        scopeLocals = scopeMap.get(key);
        if (scopeLocals == null) {
            scopeLocals = new HashSet<>();
            scopeMap.put(key, scopeLocals);
        }
    }

    private void closeScope()
    {
        scopeLocals = new HashSet<>();
        scopeName = "";
    }

    /**
     * Examines a single statement for a declaration or an assignment. Control
     * structures may carry a statement of their own, which is scanned
     * recursively.
     *
     * @param statement one statement, without the surrounding line
     */
    private void scanStatement(String statement)
    {
        String code = statement.trim();
        if (code.isEmpty()) return;

        switch (leadingKeyword(code)) {
            case "dim":
            case "const":
                parseDeclaration(stripKeyword(code), false);
                return;

            case "local":
            case "static":
                parseDeclaration(stripKeyword(code), true);
                return;

            // The condition of an IF holds comparisons and not assignments.
            // Only the statements behind the THEN are of interest. A one line
            // IF may carry an ELSE branch, which holds a statement of its own.
            case "if":
            case "elseif":
                String tail = behindThen(code);
                int split = findWord(tail, "else");

                if (split == -1)
                    scanStatement(tail);
                else {
                    scanStatement(tail.substring(0, split));
                    scanStatement(tail.substring(split+4));
                }
                return;

            case "else":
                scanStatement(stripKeyword(code));
                return;

            // "FOR i=1 TO 10" assigns the start value to the loop variable
            case "for":
            case "let":
                code = stripKeyword(code);
                break;

            default:
                break;
        }
        parseAssignment(code);
    }

    /**
     * Parses the variable list of a DIM, LOCAL, STATIC or CONST statement. The
     * list may carry a type, array dimensions and initial values, all of which
     * are skipped.
     *
     * @param text    the statement without its keyword
     * @param isLocal true for LOCAL and STATIC, false for DIM and CONST
     */
    private void parseDeclaration(String text, boolean isLocal)
    {
        String code = TYPE.matcher(text.trim()).replaceFirst("");

        for (String item : splitTopLevel(code, ',')) {
            if (isLocal) addLocal(item);
            else         addGlobal(item);
        }
    }

    /**
     * Detects an assignment at the beginning of a statement and files the
     * target as a global variable. Assignments to a local variable, to the
     * return value of a function and to a system variable are ignored.
     *
     * @param statement statement with all control keywords removed
     */
    private void parseAssignment(String statement)
    {
        int pos = findAssignment(statement);
        if (pos == -1) return;

        Matcher m = TARGET.matcher(statement.substring(0, pos).trim());
        if (!m.matches()) return;

        String name = extractName(m.group(1));
        if (name.isEmpty()) return;

        String key = name.toLowerCase(Locale.US);
        if (key.equals(scopeName.toLowerCase(Locale.US))) return;  // function return value
        if (key.startsWith("mm."))                return;  // read only system variable
        if (scopeLocals.contains(key))                    return;  // a known local variable

        globals.add(name);
    }

    private void addLocal(String text)
    {
        String name = extractName(text);
        if (name.isEmpty()) return;

        locals.add(name);
        scopeLocals.add(name.toLowerCase(Locale.US));
    }

    private void addGlobal(String text)
    {
        String name = extractName(text);
        if (!name.isEmpty()) globals.add(name);
    }

    /**
     * Cuts the plain name out of a declaration item, a parameter or an
     * assignment target and removes the type suffix.
     *
     * @param text text starting with a name
     * @return the name or an empty string, if the text does not start with one
     */
    private String extractName(String text)
    {
        Matcher m = IDENT.matcher(PASSING.matcher(text.trim()).replaceFirst(""));
        if (!m.find()) return "";

        String name = m.group(1);
        if ("$%!".indexOf(name.charAt(name.length()-1)) != -1)
            name = name.substring(0, name.length()-1);

        return name;
    }










}

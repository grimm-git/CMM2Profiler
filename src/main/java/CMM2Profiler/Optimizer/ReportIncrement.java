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

import static CMM2Profiler.core.MMBasic.*;
import CMM2Profiler.core.Source;
import CMM2Profiler.core.SourceLine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches the source code for statements that count a variable up or down.
 * MMBasic can do the same with INC, which saves the interpreter the second
 * lookup of the variable and the assignment itself. There is no DEC command,
 * a subtraction becomes an INC with a negative increment.<p>
 *
 * Three forms are recognised, b being any number or expression:
 * <ul>
 * <li>a = a + b
 * <li>a = a - b
 * <li>a = b + a, which is the same as the first one, as an addition may be
 *     written the other way round. "a = b - a" is <em>not</em> a decrement, it
 *     negates the variable, and is therefore not reported.
 * </ul>
 *
 * INC works on numbers only. A string is put together with "+" as well,
 * but it cannot be counted up, so names that hold a string are left out. A name
 * is taken for a string if it ends with a "$", if it was declared with the type
 * STRING, or if it is a function that returns a string.<p>
 *
 * The scanner walks the same way through the source as {@link ReportVariables}.
 * A line may hold several statements separated by a colon, and a one line IF
 * may carry a statement behind its THEN and another one behind its ELSE, so the
 * line is taken apart before it is examined. Declarations are skipped, an
 * initial value is no increment.
 *
 * @author Matthias Grimm
 */
public class ReportIncrement
{
    /**
     * CSS class of a clickable statement in a report. The controller looks for
     * this class to recognise a click, so both sides have to use the same
     * string.
     */
    public final static String INC_CLASS = "inc";

    /** A plain variable or one array element, nothing else can be counted up */
    private static final Pattern TARGET =
            Pattern.compile("^[A-Za-z_][A-Za-z_0-9.]*[$%!]?\\s*(\\(.*\\))?$");

    /** Leading name of a declaration item */
    private static final Pattern IDENT =
            Pattern.compile("^([A-Za-z_][A-Za-z_0-9.]*[$%!]?)");

    /** The type STRING at the beginning of a declaration */
    private static final Pattern STRINGTYPE =
            Pattern.compile("^string\\b\\s*", Pattern.CASE_INSENSITIVE);

    /** The type STRING behind the name of a declaration or of a function */
    private static final Pattern ASSTRING =
            Pattern.compile("\\bas\\s+string\\b", Pattern.CASE_INSENSITIVE);

    /** Names that hold a string and can therefore not be counted with INC */
    private final HashSet<String> stringNames = new HashSet<>();

    /** Style of the generated blocks, added to the head of the template */
    private final static String REPORTSTYLE =
          "<style type=\"text/css\">\n"
        + "p.summary { color:#555; font-size:90%; margin:2px 0 8px 0; }\n"
        + "table.inclist { width:100%; border-collapse:collapse; border:0;"
        + " font-size:90%; margin-bottom:24px; }\n"
        + "table.inclist th { text-align:left; border:0; border-bottom:1px solid #ccc;"
        + " padding:2px 12px 2px 0; }\n"
        + "table.inclist td { border:0; padding:1px 12px 1px 0; vertical-align:top; }\n"
        + "table.inclist td.num { text-align:right; white-space:nowrap; }\n"
        + "table.inclist span { font-family:monospace; }\n"
        + "table.inclist span.inc { cursor:pointer; color:#2a6099; }\n"
        + "table.inclist span.inc:hover { text-decoration:underline; }\n"
        + "</style>\n";

    private final ArrayList<Increment> increments = new ArrayList<>();

    public ArrayList<Increment> getIncrements() { return increments; }
    public int size()                           { return increments.size(); }

    /**
     * Looks a statement up again after it was clicked in the report.
     *
     * @param index position of the statement, as it was put into the report
     * @return the entry or null, if the position is unknown
     */
    public Increment findIncrement(int index)
    {
        return index>=0 && index<increments.size() ? increments.get(index) : null;
    }

    /**
     * Builds the complete report. The source code is scanned, the template is
     * loaded and every marked div of the template is filled with the appropriate
     * contents. The result is a self contained HTML page, ready to be handed to
     * WebEngine.loadContent().<p>
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
        return ReportPage.inject(html, "increments", buildTable());
    }

    /**
     * Scans the whole source code for increment statements. Previous results
     * are discarded, so the method may be called again after a new program has
     * been loaded.
     *
     * @param src source code of a MMBasic program
     */
    public final void extract(Source src)
    {
        increments.clear();
        collectStringNames(src);

        for (int n=0 ; n < src.getSourceLineCnt() ; n++) {
            SourceLine srcLine = src.getSourceLine(n);

            // Comments, empty lines, OPTION, #DEFINE and #INCLUDE carry no
            // statements. Source.load() has already removed the comments.
            if (srcLine.getType() != SourceLine.Type.SOURCE) continue;

            String code = srcLine.getSource();
            if (code.isEmpty()) continue;

            for (String statement : splitStatements(code))
                scanStatement(statement, srcLine);
        }
    }

    /**
     * Builds the table of all statements that were found. The most often
     * executed statement comes first, as that is where the replacement saves
     * the most time.
     *
     * @return the table as HTML
     */
    private String buildTable()
    {
        StringBuilder html = new StringBuilder();

        html.append("<p class=\"summary\">")
            .append(increments.isEmpty() ? "No statement of this kind was found."
                                         : increments.size()+" statements can be written with INC.")
            .append("</p>\n");

        if (increments.isEmpty()) return html.toString();

        html.append("<table class=\"inclist\">\n")
            .append("<tr><th>Calls</th><th>Statement</th><th>Proposal</th></tr>\n");

        ArrayList<Increment> list = new ArrayList<>(increments);
        list.sort(Increment.CompCalls);

        for (Increment entry : list)
            html.append("</td><td class=\"num\">").append(entry.getCalls())
                .append("</td><td><span class=\"").append(INC_CLASS)
                .append("\" data-idx=\"").append(entry.getIndex()).append("\">")
                .append(ReportPage.escape(entry.getStatement()))
                .append("</span></td><td><span>")
                .append(ReportPage.escape(entry.getProposal()))
                .append("</span></td></tr>\n");

        return html.append("</table>\n").toString();
    }

    /**
     * Collects the names that hold a string. Only the declarations are looked
     * at, a name that carries a "$" is recognised by its suffix later on and
     * does not have to be collected.
     *
     * @param src source code of a MMBasic program
     */
    private void collectStringNames(Source src)
    {
        stringNames.clear();

        for (int n=0 ; n < src.getSourceLineCnt() ; n++) {
            SourceLine srcLine = src.getSourceLine(n);
            if (srcLine.getType() != SourceLine.Type.SOURCE) continue;

            String code = srcLine.getSource();
            if (code.isEmpty()) continue;

            // The name of a function that returns a string is used like a
            // string variable to hand the result back.
            if (srcLine.isFunction()) {
                if (ASSTRING.matcher(code).find())
                    addStringName(srcLine.getFunctionName());
                continue;
            }

            for (String statement : splitStatements(code)) {
                String stmt = statement.trim();

                switch (leadingKeyword(stmt)) {
                    case "dim":
                    case "local":
                    case "static":
                    case "const":
                        break;
                    default:
                        continue;
                }

                String rest = stripKeyword(stmt).trim();
                Matcher m = STRINGTYPE.matcher(rest);
                boolean typed = m.find();
                if (typed) rest = rest.substring(m.end());

                for (String item : splitTopLevel(rest, ','))
                    if (typed || ASSTRING.matcher(item).find())
                        addStringName(item);
            }
        }
    }

    /**
     * Files one name as a string. A name with a type suffix does not have to be
     * remembered, the suffix tells the type wherever the name is used, and
     * remembering it would hide a numeric name of the same spelling.
     *
     * @param text text starting with a name
     */
    private void addStringName(String text)
    {
        Matcher m = IDENT.matcher(text.trim());
        if (!m.find()) return;

        String name = m.group(1);
        if ("$%!".indexOf(name.charAt(name.length()-1)) != -1) return;

        stringNames.add(name.toLowerCase(Locale.US));
    }

    /**
     * Tests whether a name holds a string. INC works on numbers only.
     *
     * @param target left side of an assignment
     * @return true, if the name holds a string
     */
    private boolean isStringTarget(String target)
    {
        int pos = target.indexOf('(');
        String name = (pos==-1 ? target : target.substring(0, pos)).trim();
        if (name.isEmpty()) return false;

        char suffix = name.charAt(name.length()-1);
        if (suffix == '$') return true;                     // a string by its suffix
        if (suffix == '%' || suffix == '!') return false;   // a number by its suffix

        return stringNames.contains(name.toLowerCase(Locale.US));
    }

    /**
     * Examines one statement. Control structures may carry a statement of
     * their own, which is examined recursively.
     *
     * @param statement one statement, without the surrounding line
     * @param srcLine   source line the statement belongs to
     */
    private void scanStatement(String statement, SourceLine srcLine)
    {
        String code = statement.trim();
        if (code.isEmpty()) return;

        switch (leadingKeyword(code)) {
            // A declaration with an initial value is no increment
            case "dim":
            case "local":
            case "static":
            case "const":
                return;

            // The condition of an IF holds comparisons, only the statements
            // behind the THEN and behind the ELSE can be increments.
            case "if":
            case "elseif":
                String tail = behindThen(code);
                int split = findWord(tail, "else");

                if (split == -1)
                    scanStatement(tail, srcLine);
                else {
                    scanStatement(tail.substring(0, split), srcLine);
                    scanStatement(tail.substring(split+4), srcLine);
                }
                return;

            case "else":
                scanStatement(stripKeyword(code), srcLine);
                return;

            case "let":
                code = stripKeyword(code);
                break;

            default:
                break;
        }
        checkIncrement(code, srcLine);
    }

    /**
     * Tests whether a statement counts its own target up or down and files it
     * if it does.
     *
     * @param statement statement with all control keywords removed
     * @param srcLine   source line the statement belongs to
     */
    private void checkIncrement(String statement, SourceLine srcLine)
    {
        int pos = findAssignment(statement);
        if (pos == -1) return;

        String target = statement.substring(0, pos).trim();
        String value  = statement.substring(pos+1).trim();
        if (!TARGET.matcher(target).matches()) return;
        if (isStringTarget(target)) return;

        if (checkLeading(target, value, statement, srcLine)) return;
        checkTrailing(target, value, statement, srcLine);
    }

    /**
     * Looks for "a = a + b" and "a = a - b", the target in front of the
     * operator.
     *
     * @return true, if the statement was filed
     */
    private boolean checkLeading(String target, String value, String statement, SourceLine srcLine)
    {
        int end = skipName(value, target, 1);
        if (end == -1) return false;

        while (end < value.length() && Character.isWhitespace(value.charAt(end))) end++;
        if (end >= value.length()) return false;

        char operator = value.charAt(end);
        if (operator!='+' && operator!='-') return false;

        String expression = value.substring(end+1).trim();
        if (expression.isEmpty()) return false;

        add(target, operator=='+' ? Increment.Type.ADD : Increment.Type.SUB,
            expression, statement, srcLine);
        return true;
    }

    /**
     * Looks for "a = b + a", the target behind the operator. An addition may be
     * written either way round, so this is an increment as well. A subtraction
     * is not, "a = b - a" negates the variable and changes its value in a
     * completely different way.
     *
     * @return true, if the statement was filed
     */
    private boolean checkTrailing(String target, String value, String statement, SourceLine srcLine)
    {
        int start = skipName(value, target, -1);
        if (start <= 0) return false;

        int op = start-1;
        while (op >= 0 && Character.isWhitespace(value.charAt(op))) op--;
        if (op < 0 || value.charAt(op) != '+') return false;

        // A target inside brackets belongs to a sub expression and is no
        // summand of the statement itself.
        if (parenDepth(value, op) != 0) return false;

        String expression = value.substring(0, op).trim();
        if (expression.isEmpty()) return false;

        add(target, Increment.Type.ADD, expression, statement, srcLine);
        return true;
    }

    private void add(String target, Increment.Type type, String expression,
                     String statement, SourceLine srcLine)
    {
        increments.add(new Increment(increments.size(), target, type,
                                     expression, statement, srcLine));
    }

    /**
     * Compares the beginning or the end of an expression with the target of the
     * assignment. MMBasic is case insensitive and the spaces of an array index
     * may differ from one side to the other, so both are ignored.
     *
     * @param value  right side of the assignment
     * @param target left side of the assignment
     * @param dir    1 to compare the beginning, -1 to compare the end
     * @return position behind the target for dir 1, position of the target for
     *         dir -1, or -1 if the target is not there
     */
    private int skipName(String value, String target, int dir)
    {
        int v = dir>0 ? 0 : value.length()-1;
        int t = dir>0 ? 0 : target.length()-1;

        while (true) {
            t = skipSpace(target, t, dir);
            v = skipSpace(value, v, dir);

            if (t < 0 || t >= target.length()) return dir>0 ? v : v+1;
            if (v < 0 || v >= value.length())  return -1;

            if (Character.toLowerCase(target.charAt(t)) != Character.toLowerCase(value.charAt(v)))
                return -1;
            t += dir;
            v += dir;
        }
    }

    /**
     * Skips the spaces at a position. Spaces inside an array index do not
     * matter, but a space between two characters of a name does, otherwise
     * "ab" would be found in "a b".
     *
     * @param text text to walk through
     * @param pos  position to start at
     * @param dir  1 to walk forward, -1 to walk backward
     * @return position of the next character that carries a meaning
     */
    private int skipSpace(String text, int pos, int dir)
    {
        int next = pos;
        while (next >= 0 && next < text.length() && Character.isWhitespace(text.charAt(next)))
            next += dir;

        int before = pos-dir;
        if (next != pos && before >= 0 && before < text.length()
                && next >= 0 && next < text.length()
                && isNameChar(text.charAt(before)) && isNameChar(text.charAt(next)))
            return pos;                       // the space separates two names

        return next;
    }

    /**
     * @param text a statement
     * @param pos  position in the statement
     * @return number of brackets that are still open at that position
     */
    private int parenDepth(String text, int pos)
    {
        boolean inString=false;
        int depth=0;

        for (int n=0 ; n < pos && n < text.length() ; n++) {
            char chr = text.charAt(n);

            if (chr=='"') inString = !inString;
            if (inString) continue;

            if (chr=='(') depth++;
            else if (chr==')') depth--;
        }
        return depth;
    }
}

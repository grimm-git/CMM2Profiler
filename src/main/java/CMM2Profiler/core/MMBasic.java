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
package CMM2Profiler.core;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 *
 * @author Matthias Grimm <codingjoker@web.de>
 */
public class MMBasic
{

    public static boolean isIfThenOneliner(String line)
    {
        String code = stripComment(line).toUpperCase(Locale.US);
        String pattern = "^IF\\b.+\\bTHEN\\b.+";
        return Pattern.matches(pattern, code);
    }

    public static boolean isForNextOneliner(String line)
    {
        String code = stripComment(line).toUpperCase(Locale.US);
        String pattern = "^FOR\\b.+\\bTO\\b.+\\bNEXT\\b.*";
        return Pattern.matches(pattern, code);
    }

    public static boolean isDoLoopOneliner(String line)
    {
        String code = stripComment(line).toUpperCase(Locale.US);
        String pattern = "^DO\\b.*\\bLOOP\\b.*";
        return Pattern.matches(pattern, code);
    }
    

    /**
     * @param code a statement
     * @return the first word of the statement in lower case
     */
    public static String leadingKeyword(String code)
    {
        int end=0;
        while (end < code.length() && Character.isLetter(code.charAt(end))) end++;

        return code.substring(0, end).toLowerCase(Locale.US);
    }

    public static String stripKeyword(String code)
    {
        return code.substring(leadingKeyword(code).length()).trim();
    }

    /**
     * @param code an IF or ELSEIF statement
     * @return the statement behind the THEN, empty for a multi line IF
     */
    public static String behindThen(String code)
    {
        int pos = findWord(code, "then");
        return pos==-1 ? "" : code.substring(pos+4).trim();
    }

    /**
     * Searches the position of the "=" of an assignment. The comparisons "&lt;=",
     * "&gt;=" and "&lt;&gt;" are no assignments, and an "=" inside a string or inside
     * a pair of brackets belongs to an expression and not to the statement.
     *
     * @param code a statement
     * @return position of the assignment operator or -1 if there is none
     */
    public static int findAssignment(String code)
    {
        boolean inString=false;
        int depth=0;

        for (int n=0 ; n < code.length() ; n++) {
            char chr = code.charAt(n);

            if (chr=='"') inString = !inString;
            if (inString) continue;

            if (chr=='(') depth++;
            else if (chr==')') depth--;
            else if (chr=='=' && depth==0) {
                char prev = n>0 ? code.charAt(n-1) : ' ';
                char next = n+1<code.length() ? code.charAt(n+1) : ' ';
                if (prev!='<' && prev!='>' && prev!='=' && next!='=')
                    return n;
            }
        }
        return -1;
    }

    /**
     * Searches a keyword outside of strings and brackets.
     *
     * @param code a statement
     * @param word keyword to look for, lower case
     * @return position of the keyword or -1 if it is not there
     */
    public static int findWord(String code, String word)
    {
        String text = code.toLowerCase(Locale.US);
        boolean inString=false;
        int depth=0;

        for (int n=0 ; n < text.length() ; n++) {
            char chr = text.charAt(n);

            if (chr=='"') inString = !inString;
            if (inString) continue;

            if (chr=='(') depth++;
            else if (chr==')') depth--;
            else if (depth==0 && text.startsWith(word, n)
                    && !isNamePart(text, n-1) && !isNamePart(text, n+word.length()))
                return n;
        }
        return -1;
    }

    public static boolean isNamePart(String text, int pos)
    {
        if (pos < 0 || pos >= text.length()) return false;

        return isNameChar(text.charAt(pos));
    }

    /**
     * @param chr a character
     * @return true, if the character may be part of a MMBasic name
     */
    public static boolean isNameChar(char chr)
    {
        return Character.isLetterOrDigit(chr) || chr=='_' || chr=='.';
    }

    /**
     * @param code a statement
     * @param open position of an opening bracket
     * @return position of the matching closing bracket or -1
     */
    public static int findClosingParen(String code, int open)
    {
        boolean inString=false;
        int depth=0;

        for (int n=open ; n < code.length() ; n++) {
            char chr = code.charAt(n);

            if (chr=='"') inString = !inString;
            if (inString) continue;

            if (chr=='(') depth++;
            else if (chr==')' && --depth==0) return n;
        }
        return -1;
    }

    /**
     * Splits a source line into its statements. MMBasic separates statements of
     * one line by a colon.
     *
     * @param line one line of source code, without its comment
     * @return list of statements, never empty
     */
    public static ArrayList<String> splitStatements(String line)
    {
        return splitTopLevel(line, ':');
    }

    /**
     * Splits a text at a separator, ignoring separators inside strings and
     * inside brackets.
     *
     * @param text      text to split
     * @param separator character to split at
     * @return list of the parts, never empty
     */
    public static ArrayList<String> splitTopLevel(String text, char separator)
    {
        ArrayList<String> parts = new ArrayList<>();
        boolean inString=false;
        int depth=0;
        int start=0;

        for (int n=0 ; n < text.length() ; n++) {
            char chr = text.charAt(n);

            if (chr=='"') inString = !inString;
            if (inString) continue;

            if (chr=='(') depth++;
            else if (chr==')') depth--;
            else if (chr==separator && depth==0) {
                parts.add(text.substring(start, n).trim());
                start = n+1;
            }
        }
        parts.add(text.substring(start).trim());
        return parts;
    }

    private static String stripComment(String line)
    {
        int idx = line.indexOf("'");
        if (idx != -1) return line.substring(0, idx).trim();
        return line.trim();
    }

}

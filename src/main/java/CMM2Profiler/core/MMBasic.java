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

import java.util.regex.Pattern;

/**
 *
 * @author grimm
 */
public class MMBasic
{

    public static boolean isIfThenOneliner(String line)
    {
        String code = stripComment(line).toUpperCase();
        String pattern = "^IF\\b.+\\bTHEN\\b.+";
        return Pattern.matches(pattern, code);
    }

    public static boolean isForNextOneliner(String line)
    {
        String code = stripComment(line).toUpperCase();
        String pattern = "^FOR\\b.+\\bTO\\b.+\\bNEXT\\b.*";
        return Pattern.matches(pattern, code);
    }

    public static boolean isDoLoopOneliner(String line)
    {
        String code = stripComment(line).toUpperCase();
        String pattern = "^DO\\b.*\\bLOOP\\b.*";
        return Pattern.matches(pattern, code);
    }
    
    private static String stripComment(String line)
    {
        int idx = line.indexOf("'");
        if (idx != -1) return line.substring(0, idx).trim();
        return line.trim();
    }
   
}

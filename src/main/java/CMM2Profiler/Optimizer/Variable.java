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

import CMM2Profiler.core.SourceLine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/**
 * One distinct name found in the source code. This may be the name of a
 * variable or the name of a sub or a function.<p>
 *
 * MMBasic is case insensitive, so a name is stored with the spelling of its
 * first appearance in the source code.<p>
 *
 * Every usage of the name is booked with the source line it was found in, the
 * declaration as well as every read and write access. A line that uses the name
 * more than once is booked once per usage, so the number of references is the
 * number of usages. Together with the length of the name this is a measure of
 * how much time the interpreter spends on looking this name up.
 *
 * @author Matthias Grimm
 */
public class Variable
{
    private final String name;
    private final ArrayList<SourceLine> references = new ArrayList<>();

    public Variable(String varName)
    {
        name = varName;
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Books one usage of this name.
     *
     * @param srcLine source line the name was found in
     */
    public void addReference(SourceLine srcLine)
    {
        references.add(srcLine);
    }

    /**
     * All source lines this name is used in, in the order of appearance. A line
     * that uses the name more than once appears more than once in the list.
     *
     * @return the source lines this name is used in
     */
    public ArrayList<SourceLine> getReferences() { return references; }

    public String getName()   { return name; }
    public int    getLength() { return name.length(); }
    public int    getCount()  { return references.size(); }
    public void   clearReferences() { references.clear(); }

    // -----------------------------------------------------------------------------------
    //                               Static class contents
    // -----------------------------------------------------------------------------------
    /**
     * Default comparator for sorting a list of names alphabetically.
     */
    public final static Comparator<Variable> CompName = (Variable o1, Variable o2) -> {
        String name1 = o1.getName().toLowerCase(Locale.US);
        String name2 = o2.getName().toLowerCase(Locale.US);
        //ascending order
        return name1.compareTo(name2);
    };

    /**
     * Sorts the longest name to the top. Names of equal length are sorted
     * alphabetically, so the order is stable.
     */
    public final static Comparator<Variable> CompLength = (Variable o1, Variable o2) -> {
        int diff = Integer.compare(o2.getLength(), o1.getLength());
        return diff!=0 ? diff : CompName.compare(o1, o2);
    };

    /**
     * Sorts the most often used name to the top. This is the interesting order
     * for the optimizer, as a long name that is used often costs the most time.
     */
    public final static Comparator<Variable> CompCount = (Variable o1, Variable o2) -> {
        int diff = Integer.compare(o2.getCount(), o1.getCount());
        return diff!=0 ? diff : CompName.compare(o1, o2);
    };
}

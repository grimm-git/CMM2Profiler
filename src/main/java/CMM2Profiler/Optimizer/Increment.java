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
import java.util.Comparator;
import java.util.Locale;

/**
 * One statement of the form "a = a + b" or "a = a - b" found in the source
 * code. MMBasic offers the command INC for exactly this, which spares the
 * interpreter one lookup of the variable and the whole assignment.<p>
 *
 * There is no DEC command, a subtraction is an INC with a negative increment.
 * An increment of one is the default and is left out.
 *
 * The target is the name as it is written on the left side of the statement,
 * an array element like "Blocks.X(n)" included. The expression is everything
 * behind the operator, whatever it is.
 *
 * @author Matthias Grimm
 */
public class Increment
{
    /** "a = a + b" adds, "a = a - b" subtracts */
    public enum Type {ADD, SUB};

    private final String target;
    private final String expression;
    private final String statement;
    private final Type type;
    private final SourceLine srcLine;
    private final int index;

    public Increment(int index, String target, Type type, String expression,
                     String statement, SourceLine srcLine)
    {
        this.index      = index;
        this.target     = target;
        this.type       = type;
        this.expression = expression;
        this.statement  = statement;
        this.srcLine    = srcLine;
    }

    @Override
    public String toString()
    {
        return statement;
    }

    /**
     * The statement rewritten with the MMBasic command that replaces it. There
     * is no DEC command, so a subtraction becomes an INC with a negated
     * increment. INC counts up by one if no increment is given, so an increment
     * of exactly one is left out.
     *
     * @return the proposed replacement of the statement
     */
    public String getProposal()
    {
        String value = type==Type.ADD ? expression : negate(expression);

        return "INC " + target + (value.equals("1") ? "" : ", " + value);
    }

    /**
     * Turns an expression into its negative counterpart. A leading sign is
     * flipped instead of a second one being put in front of it, so that
     * "a = a - -1" becomes "INC a, 1" and not "INC a, --1".
     *
     * @param expression the expression to negate
     * @return the negated expression
     */
    private String negate(String expression)
    {
        String expr = expression.trim();

        if (expr.startsWith("-")) return expr.substring(1).trim();
        if (expr.startsWith("+")) return "-" + expr.substring(1).trim();

        return "-" + expr;
    }

    /**
     * Position of this entry in the list of the report. The table of the report
     * is sorted, so the position travels with the statement into the HTML and
     * brings a click back to the right entry.
     *
     * @return position in the unsorted list of the report
     */
    public int        getIndex()      { return index; }

    public String     getTarget()     { return target; }
    public String     getExpression() { return expression; }
    public String     getStatement()  { return statement; }
    public Type       getType()       { return type; }
    public boolean    isAdd()         { return type==Type.ADD; }
    public SourceLine getSourceLine() { return srcLine; }
    public int        getLineNo()     { return srcLine.getLineNo(); }
    public int        getCalls()      { return srcLine.getCalls(); }

    // -----------------------------------------------------------------------------------
    //                               Static class contents
    // -----------------------------------------------------------------------------------
    /**
     * Default comparator, sorts by the name of the target variable.
     */
    public final static Comparator<Increment> CompTarget = (Increment o1, Increment o2) -> {
        int diff = o1.getTarget().toLowerCase(Locale.US)
                    .compareTo(o2.getTarget().toLowerCase(Locale.US));
        return diff!=0 ? diff : Integer.compare(o1.getLineNo(), o2.getLineNo());
    };

    /**
     * Sorts by the position in the source code.
     */
    public final static Comparator<Increment> CompLine = (Increment o1, Increment o2) ->
            Integer.compare(o1.getLineNo(), o2.getLineNo());

    /**
     * Sorts the most often executed statement to the top. That is the one
     * where the replacement saves the most time. Without profiler data all
     * counters are zero and the order falls back to the source position.
     */
    public final static Comparator<Increment> CompCalls = (Increment o1, Increment o2) -> {
        int diff = Integer.compare(o2.getCalls(), o1.getCalls());
        return diff!=0 ? diff : CompLine.compare(o1, o2);
    };
}

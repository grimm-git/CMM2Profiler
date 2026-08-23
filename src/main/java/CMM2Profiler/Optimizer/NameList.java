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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A group of distinct names, for example all global variables of a program.
 * Names are collected case insensitively, as MMBasic does not distinguish
 * between upper and lower case. The order of the list is the order of the
 * first appearance in the source code.<p>
 *
 * The class also provides the histogram of the name lengths of the group.
 *
 * @author Matthias Grimm
 */
public class NameList
{
    private final String title;
    private final LinkedHashMap<String,Variable> names = new LinkedHashMap<>();

    public NameList(String listTitle)
    {
        title = listTitle;
    }

    @Override
    public String toString()
    {
        return title;
    }

    /**
     * Add a name to the list. A name already in the list is not added a second
     * time, only its counter is increased.
     *
     * @param name name of a variable, a sub or a function
     * @return the list entry of that name, never null
     */
    public Variable add(String name)
    {
        String key = name.toLowerCase(Locale.US);

        Variable entry = names.get(key);
        if (entry == null) {
            entry = new Variable(name);
            names.put(key, entry);
        }
        entry.incCount();
        return entry;
    }

    public boolean contains(String name)
    {
        return names.containsKey(name.toLowerCase(Locale.US));
    }

    public Variable get(String name)
    {
        return names.get(name.toLowerCase(Locale.US));
    }

    /**
     * Returns a sorted copy of the list. The list itself keeps the order of
     * appearance, so sorting for a table view does not destroy it.
     *
     * @param comp one of the comparators of {@link Variable}
     * @return sorted copy of the list contents
     */
    public ArrayList<Variable> getSorted(Comparator<Variable> comp)
    {
        ArrayList<Variable> list = new ArrayList<>(names.values());
        list.sort(comp);
        return list;
    }

    /**
     * Histogram of the name lengths of this group: length of the name mapped
     * to the number of names of that length. Lengths that do not occur are not
     * part of the map. The map is sorted by name length in ascending order.
     *
     * @return histogram of the name lengths
     */
    public TreeMap<Integer,Integer> getHistogram()
    {
        TreeMap<Integer,Integer> histogram = new TreeMap<>();

        for (Variable entry : names.values()) {
            Integer cnt = histogram.get(entry.getLength());
            histogram.put(entry.getLength(), cnt==null ? 1 : cnt+1);
        }
        return histogram;
    }

    /**
     * Length of the longest name of the group. Together with the histogram
     * this is all that is needed to scale a bar chart.
     *
     * @return length of the longest name or 0 for an empty list
     */
    public int getMaxLength()
    {
        int max=0;
        for (Variable entry : names.values())
            if (entry.getLength() > max) max = entry.getLength();

        return max;
    }

    /**
     * Number of usages of all names of the group together. This is at least
     * {@link #size()}, as every name is used at least once.
     *
     * @return total number of usages
     */
    public int getTotalCount()
    {
        int sum=0;
        for (Variable entry : names.values())
            sum += entry.getCount();

        return sum;
    }

    public String getTitle()                    { return title; }
    public int size()                           { return names.size(); }
    public boolean isEmpty()                    { return names.isEmpty(); }
    public Collection<Variable> getNames()  { return names.values(); }
    public Map<String,Variable> getMap()    { return names; }
    public void clear()                         { names.clear(); }

    /**
     * Removes a name from the list.
     *
     * @param name name to remove
     * @return true, if the name was in the list
     */
    public boolean remove(String name)
    {
        return names.remove(name.toLowerCase(Locale.US)) != null;
    }

    /**
     * Sets the usage counter of every name back to zero. The names themselves
     * stay in the list, so the counting may be repeated.
     */
    public void clearCounts()
    {
        for (Variable entry : names.values())
            entry.resetCount();
    }
}

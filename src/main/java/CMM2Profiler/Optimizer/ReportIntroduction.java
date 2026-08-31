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
import CMM2Profiler.core.Source;

/**
 * @author Matthias Grimm <codingjoker@web.de>
 */
public class ReportIntroduction
extends Report
{
    /**
     * Dummy report to give the user some introduction to the Report system.<p>
     *
     * The method runs in a worker thread and must not touch the GUI.
     *
     * @param src      source code of a MMBasic program
     * @param template path of the template, relative to the report folder
     * @throws IOException if the template cannot be read
     */
    public void create(Source src, String template) throws IOException
    {
        load(template);
    }
}

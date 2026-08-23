/*
 * Copyright (C) 2018 Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package CMM2Profiler.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import CMM2Profiler.Optimizer.Reports;
import CMM2Profiler.core.Source;

public class OptimizerData
{
    private final ObservableList<String> ReportList =  FXCollections.observableArrayList();
    protected Source mainSource = new Source();   // copy from main class

    public OptimizerData()
    {
        for (Reports item : Reports.values())
            ReportList.add(item.toString());
    }

    public ObservableList<String> getReportList() { return ReportList; }
}

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
package CMM2Profiler.utils;

import java.io.IOException;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import CMM2Profiler.Optimizer.ReportIncrement;
import CMM2Profiler.Optimizer.ReportIntro;
import CMM2Profiler.Optimizer.ReportVariables;
import CMM2Profiler.Optimizer.Reports;
import CMM2Profiler.core.Source;

/**
 *
 * @author Matthias Grimm <codingjoker@web.de>
 */
public class ErrandFactory
{
    public static Errand<Source.Mode> loadSourceErrand(Source src, String filePath, String fileName,
                                        EventHandler<WorkerStateEvent> onSuccess,
                                        EventHandler<WorkerStateEvent> onFailure)
    {
        Errand<Source.Mode> E = new Errand<>() {
                @Override 
                protected Source.Mode call() throws IOException {
                    return src.load(filePath, fileName);
                }};
        
        E.setOnSucceeded(onSuccess);
        E.setOnFailed(onFailure);
        E.setOnCancelled(onFailure);
        return E;
    }
    
    public static void execErrandLoadSource(Source src, String filePath, String fileName,
                                        EventHandler<WorkerStateEvent> onSuccess,
                                        EventHandler<WorkerStateEvent> onFailure)
    {
        loadSourceErrand(src, filePath, fileName, onSuccess, onFailure).execute();
    }
    
    public static Errand<String> createReportErrand(Reports report, Source src,
                                        ReportVariables vars, ReportIncrement incs,
                                        EventHandler<WorkerStateEvent> onSuccess,
                                        EventHandler<WorkerStateEvent> onFailure)
    {
        Errand<String> E = new Errand<>() {
                @Override 
                protected String call() throws IOException {
                    switch (report) {
                    case INTRO:
                        ReportIntro RI = new ReportIntro();
                        return RI.create(src,report.getTemplate());
                    case VARIABLES:
                        // the caller keeps the object, its name lists are
                        // needed again when a name in the report is clicked
                        return vars.create(src,report.getTemplate());
                    case INC:
                        // the caller keeps the object, its statements are
                        // needed again when one of them is clicked
                        return incs.create(src,report.getTemplate());
                    default:
                        return null;
                    }
                }};
        
        E.setOnSucceeded(onSuccess);
        E.setOnFailed(onFailure);
        E.setOnCancelled(onFailure);
        return E;
    }
    
    public static void execErrandCreateReport(Reports report, Source src,
                                        ReportVariables vars, ReportIncrement incs,
                                        EventHandler<WorkerStateEvent> onSuccess,
                                        EventHandler<WorkerStateEvent> onFailure)
    {
        createReportErrand(report, src, vars, incs, onSuccess, onFailure).execute();
    }
}

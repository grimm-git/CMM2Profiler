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
package CMM2Profiler.utils;

import CMM2Profiler.Registry;
import CMM2Profiler.gui.ProgressDialog;
import javafx.concurrent.Task;

/**
 * An Errand is a single task that will be executed and closed. It is not foreseen for
 * continously running tasks.
 * 
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 * @param <V>  Datatype of return data from the Errand
 */
public abstract class Errand<V>
extends Task<V>
{
    String actionText;
  
    /**
     * Configure the Task to open a progress dialog and show the progress of the nbackground
     * task. The dialog will be fed by the task properties progressProperty.<p>
     * The dialog will automatically openen, when the task is started and will close after the taks
     * has finished.
     * 
     * @param msg  Message to explain what the background task is doing. Will be displayed in the progress dialog
     */
    public void withProgressDialog(String msg)
    {
        new ProgressDialog(this);
        updateMessage(msg);
        actionText = msg;
    }

    public void setMessage(String msg)
    {
        updateMessage(msg);
        actionText = msg;
    }

    public void setMessage(double avg, int units)
    {
        double rest = avg * units;
        int rest_m = (int) rest / 60;
        int rest_s = (int) rest % 60;
        if (rest_m > 0)
            updateMessage(String.format("%s  %d:%02d to go (%.3fs)", actionText, rest_m, rest_s, avg));
        else
            updateMessage(String.format("%s  %d seconds to go (%.3fs)", actionText, rest_s, avg));
    }

    public void execute()
    {
        Registry Reg = Registry.get();
        Reg.getExecutor().execute(this);
    }
    
    protected void setProgress(int val, int max) {
        setProgress((double) val, (double) max);
    }

    protected void setProgress(double val, double max) {
        // Adjust Infinity / NaN to be -1 for both val and max.
        if (Double.isInfinite(val) || Double.isNaN(val)) val = -1;
        if (Double.isInfinite(max) || Double.isNaN(max)) max = -1;

        if (val < 0)   val = -1;
        if (max < 0)   max = -1;
        if (val > max) val = max;   // Clamp the workDone if necessary so as not to exceed max

        if (val != -1) updateProgress(val,max);
    }
}

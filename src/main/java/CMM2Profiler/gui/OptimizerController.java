/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
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

import java.io.IOException;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import CMM2Profiler.Optimizer.Reports;
import CMM2Profiler.core.Source;
import static CMM2Profiler.utils.ErrandFactory.execErrandCreateReport;

/**
 * FXML controller class.<p>
 * This class describes a detachable panel to show a project journal. Only one panel of a kind
 * could be opened. Only the contents might change. The Singleton pattern will guarantee that.<p>
 * 
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
public class OptimizerController
extends WindowFX       
{
    private static volatile OptimizerController instance = null;   // Singleton object instance 
    
    @FXML  private ListView<String> listReports;
    @FXML  private WebView viewReport;
    @FXML  private TextField textSearch;
    @FXML  private Button btnClose;
    @FXML  private Label errorMsg;
    
    private final OptimizerData dataModel;

    @SuppressWarnings("DoubleCheckedLocking")
    public static OptimizerController open() throws IOException
    {
        if (instance == null)
        {
            synchronized(OptimizerController.class)
            {
                if (instance == null)
                    instance = new OptimizerController();
            }
        }
        return instance;
    }

    public void setSource(Source src)
    {
        dataModel.mainSource=src;
    }

    /**
     * Singletons require a private constructor, so no further objects of this class could
     * be instanciated.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    private OptimizerController() throws IOException
    {
        super("OptimizerWindow.fxml", "cmm2profiler.css");
        stage.setTitle("Performance Optimizer");
        stage.setResizable(true);
        setMsgLabel(errorMsg);

        dataModel = new OptimizerData();

        listReports.setItems(dataModel.getReportList());
        listReports.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listReports.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        Reports rep=Reports.findReports(newVal);

                        execErrandCreateReport(rep, dataModel.mainSource,
                                this::reportSucceeded, this::taskFailed);
                    }
                });

        textSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            });
    }
   
    @FXML
    protected void handleAction(ActionEvent ev)
    {
        if (ev.getSource() == btnClose)  close();
    }
    
    @FXML
    protected void handleKeys(KeyEvent ev)
    {
        if (ev.getEventType() == KeyEvent.KEY_PRESSED) {
            if (ev.getCode() == KeyCode.ENTER) {
                if (ev.getSource() == btnClose)  close();
            }
        }
    }
     
    // ---------------------------------------------------------------------------------------- 
    //                          task helper functions
    // ---------------------------------------------------------------------------------------- 
    private void reportSucceeded(WorkerStateEvent ev)
    {
        String report = (String) ev.getSource().getValue();

        if (report == null) {
            showError("This report is not available yet.");
            return;
        }

        // The report is a complete HTML page and carries its own base address,
        // so loadContent() finds the style sheet of the template.
        viewReport.getEngine().loadContent(report, "text/html");
        showSuccess("Report successfully created!");
    }

    private void taskFailed(WorkerStateEvent ev)
    {
        Throwable ex = ev.getSource().getException();
        String errormsg = ex == null ? "Creating Report failed!" : ex.getLocalizedMessage();
        showError(errormsg);
    }
}

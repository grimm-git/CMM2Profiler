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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import CMM2Profiler.Optimizer.Increment;
import CMM2Profiler.Optimizer.Report;
import CMM2Profiler.Optimizer.ReportIncrement;
import CMM2Profiler.Optimizer.ReportVariables;
import CMM2Profiler.Optimizer.Reports;
import CMM2Profiler.Optimizer.Variable;
import CMM2Profiler.core.Source;
import CMM2Profiler.core.SourceLine;
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

    /** Window the reference buttons of a clicked name are built in */
    private MainWindowController mainWindow = null;

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

    public void setMainWindow(MainWindowController ctrl)
    {
        mainWindow = ctrl;
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
        listReports.getSelectionModel().select("Introduction");

        // Every report brings its own document, so the click handler for the
        // variable names has to be installed again after every load.
        viewReport.getEngine().getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal == Worker.State.SUCCEEDED)
                        installNameHandler();
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
     
    /**
     * Makes the variable names of the report clickable. One listener on the
     * root element of the document is enough, as the click events of the names
     * bubble up to it. That keeps the report free of hundreds of listeners.
     */
    private void installNameHandler()
    {
        Document doc = viewReport.getEngine().getDocument();
        if (doc == null) return;

        ((EventTarget) doc.getDocumentElement()).addEventListener("click", ev -> {
                    Node node = (Node) ev.getTarget();
                    if (!(node instanceof Element)) return;

                    Element element = (Element) node;
                    String  style   = element.getAttribute("class");

                    // Let the browser finish the dispatch of the click before
                    // the GUI of the main window is rebuilt.
                    if (ReportVariables.VAR_CLASS.equals(style)) {
                        String group = element.getAttribute("data-list");
                        String name  = element.getAttribute("data-name");
                        Platform.runLater(() -> showReferences(group, name));

                    } else if (ReportIncrement.INC_CLASS.equals(style)) {
                        String index = element.getAttribute("data-idx");
                        Platform.runLater(() -> showStatement(index));
                    }
                }, false);
    }

    /**
     * Looks the clicked statement up in the report data and hands its source
     * line to the main window, which turns it into a jump button.
     *
     * TODO: Encapsulate the function with try..catch. getReportInc() needs to throw an exception if the type is wrong
     * 
     * @param index position of the statement in the report
     */
    private void showStatement(String index)
    {
        if (mainWindow == null) {
            showError("No main window to show the references in.");
            return;
        }

        Increment entry = null;
        try {
            entry = dataModel.getReportInc().findIncrement(Integer.parseInt(index));
        } catch (NumberFormatException ex) { /* EMPTY */ }

        if (entry == null) {
            showError("Unknown statement.");
            return;
        }

        ArrayList<SourceLine> refs = new ArrayList<>();
        refs.add(entry.getSourceLine());

        mainWindow.setReferenceLabel(3, entry.getStatement(), "");
        mainWindow.createRefButtons(refs);
    }

    /**
     * Looks the clicked name up in the report data and hands its source line
     * references to the main window, which turns them into jump buttons.
     *
     * TODO: Encapsulate the function with try..catch. getReportVars() needs to throw an exception if the type is wrong
     *
     * @param group name group the clicked name belongs to
     * @param name  name of the variable, the sub or the function
     */
    private void showReferences(String group, String name)
    {
        if (mainWindow == null) {
            showError("No main window to show the references in.");
            return;
        }

        Variable entry = dataModel.getReportVars().findVariable(group, name);
        if (entry == null) {
            showError("Unknown name \""+name+"\".");
            return;
        }

        // A line that uses the name more than once is booked once per usage.
        // The jump buttons need every line only once, and the LinkedHashSet
        // keeps them in the order of their appearance in the source.
        ArrayList<SourceLine> refs = new ArrayList<>(new LinkedHashSet<>(entry.getReferences()));

        switch (group) {
            case ReportVariables.LIST_GLOBAL:
                mainWindow.setReferenceLabel(1, name, "(used "+entry.getCount()+" times in "+refs.size()+" lines)");
                break;
            case ReportVariables.LIST_LOCAL:
                mainWindow.setReferenceLabel(2, name, "(used "+entry.getCount()+" times in "+refs.size()+" lines)");
                break;
            case ReportVariables.LIST_ROUTINE:
                mainWindow.setReferenceLabel(0, name, "");
                break;
        }
        mainWindow.createRefButtons(refs);
    }

    // ---------------------------------------------------------------------------------------- 
    //                          task helper functions
    // ---------------------------------------------------------------------------------------- 
    private void reportSucceeded(WorkerStateEvent ev)
    {
        Report rep = (Report) ev.getSource().getValue();

        if (rep != null) {
            dataModel.setReport(rep);

            // The report is a complete HTML page and carries its own base address,
            // so loadContent() finds the style sheet of the template.
            viewReport.getEngine().loadContent(rep.getReportHTML(), "text/html");
            showSuccess("Report successfully created!");
        } else
            showError("This report is not available yet.");
    }

    private void taskFailed(WorkerStateEvent ev)
    {
        Throwable ex = ev.getSource().getException();
        String errormsg = ex == null ? "Creating Report failed!" : ex.getLocalizedMessage();
        showError(errormsg);
    }
}

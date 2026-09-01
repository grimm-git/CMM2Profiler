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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;

import CMM2Profiler.Analyzer.Increment;
import CMM2Profiler.Analyzer.Report;
import CMM2Profiler.Analyzer.ReportIncrement;
import CMM2Profiler.Analyzer.ReportVariables;
import CMM2Profiler.Analyzer.Reports;
import CMM2Profiler.Analyzer.Variable;
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
public class AnalyzerController
extends WindowFX       
{
    private static volatile AnalyzerController instance = null;   // Singleton object instance 
    
    @FXML  private ListView<String> listReports;
    @FXML  private WebView viewReport;
    @FXML  private TextField textSearch;
    @FXML  private Button btnClose;
    @FXML  private Label errorMsg;
    
    private final AnalyzerData dataModel;

    /** Window the reference buttons of a clicked name are built in */
    private MainWindowController mainWindow = null;

    @SuppressWarnings("DoubleCheckedLocking")
    public static AnalyzerController open() throws IOException
    {
        if (instance == null)
        {
            synchronized(AnalyzerController.class)
            {
                if (instance == null)
                    instance = new AnalyzerController();
            }
        }
        return instance;
    }

    public void setSource(Source src)
    {
        dataModel.mainSource=src;
        listReports.getSelectionModel().select("Introduction");

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
    private AnalyzerController() throws IOException
    {
        super("AnalyzerWindow.fxml", "cmm2profiler.css");
        stage.setTitle("Performance Analyzer");
        stage.setResizable(true);
        setMsgLabel(errorMsg);

        dataModel = new AnalyzerData();

        listReports.setItems(dataModel.getReportList());
        listReports.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listReports.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        Reports rep=Reports.findReports(newVal);

                        execErrandCreateReport(rep, dataModel.mainSource,
                                this::reportSucceeded, this::taskFailed);
                    }
                });

        // Every report brings its own document, so the click handler for the
        // variable names has to be installed again after every load.
        viewReport.getEngine().getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal == Worker.State.SUCCEEDED)
                        installNameHandler();
                });

        // Every keystroke starts a new search at the top of the report
        textSearch.textProperty().addListener((obs, oldVal, newVal) -> searchReport(newVal, false));
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
                if (ev.getSource() == textSearch) searchReport(textSearch.getText(), true);
            }
        }
    }
     
    /**
     * Searches a text in the report and scrolls to it. The browser does the
     * work, window.find() selects the hit and brings it into view.<p>
     *
     * Every change of the search field starts again at the top of the page,
     * otherwise the next keystroke would carry on behind the hit of the
     * previous one and walk through the report while the word is typed. ENTER
     * carries on and steps to the next hit, wrapping around at the end.
     *
     * @param text search text
     * @param next false to start at the top, true to step to the next hit
     */
    private void searchReport(String text, boolean next)
    {
        WebEngine engine = viewReport.getEngine();

        // While a report is still loading there is nothing to search in
        if (engine.getLoadWorker().getState() != Worker.State.SUCCEEDED) return;

        if (text == null || text.isEmpty()) {
            engine.executeScript("window.getSelection().removeAllRanges();");
            clearMessage();
            return;
        }

        if (!next)
            engine.executeScript("window.getSelection().removeAllRanges(); window.scrollTo(0,0);");

        Object found = engine.executeScript(
                "window.find(\""+escapeScript(text)+"\", false, false, true)");

        if (Boolean.TRUE.equals(found))
            clearMessage();
        else
            showError("\""+text+"\" not found.");
    }

    /**
     * Makes a text safe to be put into a JavaScript string. A text field holds
     * one line, so the quotes and the backslash are all that can hurt.
     *
     * @param text text to put into a script
     * @return the text with the special characters escaped
     */
    private String escapeScript(String text)
    {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
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
     * @param index position of the statement in the report
     */
    private void showStatement(String index)
    {
        try {
            if (mainWindow == null) throw new IllegalArgumentException("Main window not open.");

            Increment entry = null;
            entry = dataModel.getReportInc().findIncrement(Integer.parseInt(index));

            ArrayList<SourceLine> refs = new ArrayList<>();
            refs.add(entry.getSourceLine());

            mainWindow.setReferenceLabel(3, entry.getStatement(), "");
            mainWindow.createRefButtons(refs);

        } catch(NumberFormatException ex) {
            showError("Unknown Statement");
        } catch(IllegalArgumentException ex) {
            showError(ex.getLocalizedMessage());
        }
    }

    /**
     * Looks the clicked name up in the report data and hands its source line
     * references to the main window, which turns them into jump buttons.
     *
     * @param group name group the clicked name belongs to
     * @param name  name of the variable, the sub or the function
     */
    private void showReferences(String group, String name)
    {
        try {
            if (mainWindow == null) throw new IllegalArgumentException("Main window not open.");

            Variable entry = dataModel.getReportVars().findVariable(group, name);
            if (entry == null) throw new IllegalArgumentException("Unknown variable \""+name+"\".");

            // LinkedHashSet secures that each reference appears only once
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

        } catch(IllegalArgumentException ex) {
            showError(ex.getLocalizedMessage());
        }
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

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

import CMM2Profiler.Defaults;
import CMM2Profiler.core.ProfilerData;
import CMM2Profiler.core.SourceFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * FXML Controller class.<p>
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
public class MainWindowController
extends WindowFX
{
    @FXML  private Label lbPrgName;
    @FXML  private Button  btnClose;
    @FXML  private Label  errorMsg;
    @FXML  private MenuItem miOpen;
    @FXML  private MenuItem miExit;
    @FXML  private MenuItem miAbout;

    @FXML  private TreeTableView<ProfilerData> tableFunctions;
    @FXML  private TreeTableColumn<ProfilerData, String> colName;
    @FXML  private TreeTableColumn<ProfilerData, Integer> colCallsCnt;
    @FXML  private TreeTableColumn<ProfilerData, Float> colExecTime;
    @FXML  private TreeTableColumn<ProfilerData, Float> colCallsTime;
    
    @FXML  private TableView<DMSourceLine> tableSource;
    @FXML  private TableColumn<DMSourceLine, Integer> colLine;
    @FXML  private TableColumn<DMSourceLine, String> colCode;

    private final DoubleProperty tableFunctionsBarWidthProperty = new SimpleDoubleProperty();
    private final DoubleProperty tableSourceBarWidthProperty = new SimpleDoubleProperty();
    private final MainWindowData dataModel;

    public MainWindowController(Stage stage) throws IOException
    {
        super(stage, "MainWindow.fxml", "cmm2profiler.css");
        stage.setTitle(String.format("CMM2 Profiler V%d.%d%s",
                Defaults.APP_VERSION, Defaults.APP_REVISION, Defaults.APP_SUFFIX));
        stage.setResizable(true);
        setMsgLabel(errorMsg);
        
        dataModel = new MainWindowData(); // create dialogue data model

        dataModel.errorMsgProperty().addListener(
            (ObservableValue<? extends String> ov, String oldVal, String newVal) -> {
                showError(newVal);
            });
        dataModel.successMsgProperty().addListener(
            (ObservableValue<? extends String> ov, String oldVal, String newVal) -> {
                showSuccess(newVal);
            });
        
        lbPrgName.textProperty().bind(dataModel.nameProperty());
        
        colName.setCellValueFactory(new TreeItemPropertyValueFactory<>("codeLine"));
        colName.setCellFactory(cellFactoryFunction);
        colName.getStyleClass().add("column-align-left");
        colName.setReorderable(false);

        colCallsCnt.setCellValueFactory(new TreeItemPropertyValueFactory<>("callsCnt"));
        colCallsCnt.getStyleClass().add("column-align-right");
        colCallsCnt.setReorderable(false);

        colExecTime.setCellValueFactory(new TreeItemPropertyValueFactory<>("execTime"));
        colExecTime.setCellFactory(cellFactoryFloat);
        colExecTime.getStyleClass().add("column-align-right");
        colExecTime.setReorderable(false);
        
        colCallsTime.setCellValueFactory(new TreeItemPropertyValueFactory<>("CallsTime"));
        colCallsTime.setCellFactory(cellFactoryFloat);
        colCallsTime.getStyleClass().add("column-align-right");
        colCallsTime.setReorderable(false);
        colCallsTime.prefWidthProperty().bind(tableFunctions.widthProperty()
                                       .subtract(colName.widthProperty())
                                       .subtract(colCallsCnt.widthProperty())
                                       .subtract(colExecTime.widthProperty())
                                       .subtract(tableFunctionsBarWidthProperty)
                                       .subtract(2));
        
        colLine.setCellValueFactory(new PropertyValueFactory<>("lineno"));
        colLine.getStyleClass().add("column-align-left");
        colLine.setReorderable(false);
        
        colCode.setCellValueFactory(new PropertyValueFactory<>("codeline"));
        colCode.getStyleClass().add("column-align-left");
        colCode.setReorderable(false);
        colCode.prefWidthProperty().bind(tableSource.widthProperty()
                                       .subtract(colLine.widthProperty())
                                       .subtract(tableSourceBarWidthProperty)
                                       .subtract(2));

        tableSource.setItems(dataModel.getSourceList());
        
        stage.setOnShown(ev -> {
            final ScrollBar bar = getVerticalScrollbar(tableFunctions);
            if (bar != null) {
                tableFunctionsBarWidthProperty.set(bar.visibleProperty().get() ? bar.getWidth() : 0);
                bar.visibleProperty().addListener((obs, oldVal, newVal) -> {  
                    tableFunctionsBarWidthProperty.set(newVal ? bar.getWidth() : 0);
                });
            }
            final ScrollBar bar2 = getVerticalScrollbar(tableSource);
            if (bar2 != null) {
                tableSourceBarWidthProperty.set(bar2.visibleProperty().get() ? bar2.getWidth() : 0);
                bar2.visibleProperty().addListener((obs, oldVal, newVal) -> {  
                    tableSourceBarWidthProperty.set(newVal ? bar2.getWidth() : 0);
                });
            }
        });
    }
    
    private void loadProfilerLog(String filePath, String fileName)
    {
        BufferedReader reader;
        ProfilerData rootFunction;
        ProfilerData mainFunction;
        ProfilerData curFunction;
        boolean catchNext=false;

        try {
            File fh=new File(filePath, fileName);
            InputStream iStream = new FileInputStream(fh);
            reader = new BufferedReader(new InputStreamReader(iStream));
            
            // Read header and verify file format
            String line = reader.readLine();
            if (line.charAt(0)=='/') line=line.substring(1);
            String[] parts = line.split("/");
            if (parts.length != 3) throw new IOException("Bad file format");
            if (!parts[2].endsWith(".bas")) throw new IOException("Bad file format");

            rootFunction = new ProfilerData();
            rootFunction.setCodeLine(parts[1]);
            rootFunction.setSourceFile(parts[2]);
            dataModel.addNode(null, rootFunction);  // create and add root node

            mainFunction = new ProfilerData();
            mainFunction.setCodeLine("Main Program");
            mainFunction.setSourceFile(parts[2]);
            dataModel.addNode(rootFunction, mainFunction);  // create and add root node
                        
            curFunction = mainFunction;
            
            line = reader.readLine();
            while (line != null) {
                ProfilerData tmp = new ProfilerData(line);
                dataModel.addProfilerData(tmp);
                
                if (tmp.isFunction()) {
                    dataModel.addNode(rootFunction, tmp);  // add Node to the root
                    curFunction = tmp;
                    // The function call has always an execution counter of one. So we
                    // look for the first command in the function to get the real execution counter
                    catchNext=true;
                } else if (tmp.isEndFunction()) {
                    dataModel.addNode(curFunction, tmp);  // add Node to the function tree
                    curFunction.setExecTime(curFunction.getExecTime()+tmp.getExecTime());
                    curFunction=mainFunction;
                } else if (tmp.isMainCode()) {
                    dataModel.addNode(mainFunction, tmp);  // add Node to the main function
                    mainFunction.setExecTime(mainFunction.getExecTime()+tmp.getExecTime());
                } else {
                    dataModel.addNode(curFunction, tmp);  // add Node to the function tree
                    curFunction.setExecTime(curFunction.getExecTime()+tmp.getExecTime());
                    if (catchNext) {
                        // Here we copy the execution counter from the first command in the
                        // function to the function itself.
                        curFunction.setCallsCnt(tmp.getCallsCnt());
                        catchNext=false;
                    }
                }
                line = reader.readLine();
            }
            reader.close();

        } catch (IOException ex) {
            showError(ex.getLocalizedMessage());
        }
    }
    
    private void loadSourceFiles(String filePath, String fileName)
    {
        String temp;
        
        try {
            dataModel.mainSource.load(filePath, fileName);
            
            for(String codeLine : dataModel.mainSource.getSourceMap().values()) {
                if (codeLine.length() < 12) continue;
                temp = codeLine.substring(0, 8).toLowerCase();
                if (temp.equals("#include")) {
                    int a = codeLine.indexOf('"')+1;
                    int b = codeLine.indexOf('"', a);
                    SourceFile include=new SourceFile();
                    include.load(filePath, codeLine.substring(a,b));
                    dataModel.includes.add(include);
                }
            }
        
        } catch (IOException ex) {
            showError(ex.getLocalizedMessage());
        }
    }
    
    // ---------------------------------------------------------------------------------------- 
    //                                      FXML GUI handler
    // ---------------------------------------------------------------------------------------- 
    @FXML
    protected void handleAction(ActionEvent ev)
    {
        if (ev.getSource() == btnClose) close();
    }

    @FXML
    protected void handleKeys(KeyEvent ev)
    {
        if (ev.getEventType() == KeyEvent.KEY_PRESSED) {
            if (ev.getCode() == KeyCode.ENTER) {
                if (ev.getSource() == btnClose) close();
            }
        } else if (ev.getEventType() == KeyEvent.KEY_TYPED) {
            String str = ev.getCharacter();
            for (int n = 0; n < str.length(); n++) {
                char c = str.charAt(n);
                if (Character.isLetterOrDigit(c) || " -_()".indexOf(c) >= 0) {
                    errorMsg.setText("");
                }
            }
        }
    }

    @FXML
    protected void handleMenus(ActionEvent event) throws IOException
    {
        // File Menu
        if (event.getSource() == miOpen) {
            File fh=loadDialog("Open Source/Profiler File...");
            String fileName = fh.getName();
            String filePath = fh.getParent();
             
            int pos=fileName.lastIndexOf('.');
            if (pos != -1) fileName=fileName.substring(0,pos);
            dataModel.setProgramName(fileName+".bas");
           
            loadSourceFiles(filePath, fileName+".bas");
            dataModel.updateSourceList(dataModel.mainSource);
            
            loadProfilerLog(filePath, fileName+".csv");
            dataModel.updateFunctionTree();
            tableFunctions.setRoot(dataModel.getFunctionTree());
            
        } else if (event.getSource() == miExit) {
            close();

        // Help Menu
        } else if (event.getSource() == miAbout) {
            AboutController ctrl = new AboutController();
            ctrl.show();
        }
    }

    public File loadDialog(String title)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Basic Files", "*.bas"));
        
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        return selectedFile;
    }

    private Callback<TreeTableColumn<ProfilerData, Float>, TreeTableCell<ProfilerData, Float>> cellFactoryFloat = (tableColumn) -> {
        TreeTableCell<ProfilerData, Float> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);

                this.setText(null);
                this.setGraphic(null);

                if(!empty){
                    this.setText(String.format("%.3f", item/1000));
                }
            }
        };
        return tableCell;
    };
    
    private final Image imageSub = new Image(getClass().getResource("/images/16_sub.png").toExternalForm(), 16, 16, false, false);
    private final Image imageFunction = new Image(getClass().getResource("/images/16_function.png").toExternalForm(), 16, 16, false, false);
    private final Image imageCode = new Image(getClass().getResource("/images/16_code.png").toExternalForm(), 16, 16, false, false);

    private Callback<TreeTableColumn<ProfilerData, String>, TreeTableCell<ProfilerData, String>> cellFactoryFunction = (tableColumn) -> {
        TreeTableCell<ProfilerData, String> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                this.setText(null);
                this.setGraphic(null);

                if(!empty){
                    String tmp=item.toLowerCase();
                    if (tmp.startsWith("function ")) {
                        this.setText(item.substring(9));
                        this.setGraphic(new ImageView(imageFunction));

                    } else if (tmp.startsWith("sub ")) {
                        this.setText(item.substring(4));
                        this.setGraphic(new ImageView(imageSub));
                        
                    } else {
                        this.setText(item);
                        this.setGraphic(new ImageView(imageCode));
                    }
                }
            }
        };
        return tableCell;
    };

}

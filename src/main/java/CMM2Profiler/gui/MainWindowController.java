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
import CMM2Profiler.core.SourceLineData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
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
    
    @FXML  private Tab tabSource;
    @FXML  private TableView<SourceLineData> tableSource;
    @FXML  private TableColumn<SourceLineData, Integer> colSourceLine;
    @FXML  private TableColumn<SourceLineData, String> colSourceCode;
    @FXML  private TableColumn<SourceLineData, Float> colSourceTime;
    @FXML  private TableColumn<SourceLineData, Integer> colSourceCalls;
    @FXML  private ComboBox<String> comboSource;
    
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
        colExecTime.setCellFactory(treeFactoryFloat);
        colExecTime.getStyleClass().add("column-align-right");
        colExecTime.setReorderable(false);
        
        colCallsTime.setCellValueFactory(new TreeItemPropertyValueFactory<>("CallsTime"));
        colCallsTime.setCellFactory(treeFactoryFloat);
        colCallsTime.getStyleClass().add("column-align-right");
        colCallsTime.setReorderable(false);
        colCallsTime.prefWidthProperty().bind(tableFunctions.widthProperty()
                                       .subtract(colName.widthProperty())
                                       .subtract(colCallsCnt.widthProperty())
                                       .subtract(colExecTime.widthProperty())
                                       .subtract(tableFunctionsBarWidthProperty)
                                       .subtract(2));
        
        colSourceLine.setCellValueFactory(new PropertyValueFactory<>("slNo"));
        colSourceLine.getStyleClass().add("column-align-left");
        colSourceLine.setReorderable(false);
        
        colSourceCode.setCellValueFactory(new PropertyValueFactory<>("slCode"));
        colSourceCode.setCellFactory(formatCellLevel);
        colSourceCode.getStyleClass().add("column-align-left");
        colSourceCode.setReorderable(false);
        colSourceCode.prefWidthProperty().bind(tableSource.widthProperty()
                                       .subtract(colSourceLine.widthProperty())
                                       .subtract(colSourceCalls.widthProperty())
                                       .subtract(colSourceTime.widthProperty())
                                       .subtract(tableSourceBarWidthProperty)
                                       .subtract(2));

        colSourceCalls.setCellValueFactory(new PropertyValueFactory<>("slCalls"));
        colSourceCalls.setCellFactory(formatCellInt);
        colSourceCalls.getStyleClass().add("column-align-right");
        colSourceCalls.setReorderable(false);

        colSourceTime.setCellValueFactory(new PropertyValueFactory<>("slTime"));
        colSourceTime.setCellFactory(formatCellFloat);
        colSourceTime.getStyleClass().add("column-align-right");
        colSourceTime.setReorderable(false);

        tableSource.setItems(dataModel.getSourceList());

        comboSource.setItems(dataModel.getSFNList());
        comboSource.valueProperty().bindBidirectional(dataModel.selectedSFNProperty());
        comboSource.valueProperty().addListener((obs, oldVal, newVal) -> { showSourceFile(newVal); });
        tabSource.textProperty().bind(dataModel.selectedSFNProperty());
        
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
            dataModel.addSFNMain(fileName);
            dataModel.setProfilerDataInSource(dataModel.mainSource);
            
            for (int n=0 ; n < dataModel.mainSource.getLastLineNo() ; n++) {
                String codeLine = dataModel.mainSource.getSourceLine(n);
                if (codeLine.length() < 12) continue;
                temp = codeLine.substring(0, 8).toLowerCase();
                if (temp.equals("#include")) {
                    int a = codeLine.indexOf('"')+1;
                    int b = codeLine.indexOf('"', a);
                    SourceFile include=new SourceFile();
                    include.load(filePath, codeLine.substring(a,b));
                    dataModel.includes.add(include);
                    dataModel.addSFNInclude(codeLine.substring(a,b));
                    dataModel.setProfilerDataInSource(include);
                }
            }
        } catch (IOException ex) {
            showError(ex.getLocalizedMessage());
        }
    }
    
    private void showSourceFile(String newVal)
    {
        if (newVal.equals(dataModel.mainSource.getFilePath()))
            dataModel.updateSourceList(dataModel.mainSource);
        else {
            for (SourceFile sf : dataModel.includes) {
                if (newVal.equals(sf.getFilePath())) {
                    dataModel.updateSourceList(sf);
                    break;
                }
            }
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
            
            loadProfilerLog(filePath, fileName+".csv");
            dataModel.updateFunctionTree();
            tableFunctions.setRoot(dataModel.getFunctionTree());

            loadSourceFiles(filePath, fileName+".bas");
            
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

    /**
     * Table callbacks to format table cells
     */
    private Callback<TableColumn<SourceLineData, String>, TableCell<SourceLineData, String>> formatCellLevel = (tableColumn) -> {
        TableCell<SourceLineData, String> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if(!empty){
                    SourceLineData sl = this.getTableView().getItems().get(this.getIndex());
                    if (sl.isCodeLine() && sl.getLevel()>0) {
                        String format=String.format("%%%ds%%s",sl.getLevel()*4);
                        this.setText(String.format(format,"",item));
                    } else
                        this.setText(item);
                }
            }
        };
        return tableCell;
    };
    private Callback<TableColumn<SourceLineData, Float>, TableCell<SourceLineData, Float>> formatCellFloat = (tableColumn) -> {
        TableCell<SourceLineData, Float> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if(!empty){
                    SourceLineData sl = this.getTableView().getItems().get(this.getIndex());
                    if (sl.isCodeLine())
                        this.setText(String.format("%.3f", item/1000));
                }
            }
        };
        return tableCell;
    };
    private Callback<TableColumn<SourceLineData, Integer>, TableCell<SourceLineData, Integer>> formatCellInt = (tableColumn) -> {
        TableCell<SourceLineData, Integer> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if(!empty){
                    SourceLineData sl = this.getTableView().getItems().get(this.getIndex());
                    if (sl.isCodeLine())
                        this.setText(String.format("%d", item));
                }
            }
        };
        return tableCell;
    };

    /**
     * Tree table callbacks to format tree table cells
     */
    private Callback<TreeTableColumn<ProfilerData, Float>, TreeTableCell<ProfilerData, Float>> treeFactoryFloat = (tableColumn) -> {
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

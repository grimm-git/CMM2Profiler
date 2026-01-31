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
import CMM2Profiler.core.Function;
import CMM2Profiler.utils.ObjectConverter;
import CMM2Profiler.core.SourceLine;
import static CMM2Profiler.utils.ErrandFactory.execErrandLoadSource;
import java.io.File;
import java.io.IOException;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
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
    
    @FXML  private TreeTableView<SourceLine> SourceTree;
    @FXML  private TreeTableColumn<SourceLine, Integer> colLine;
    @FXML  private TreeTableColumn<SourceLine, Integer> colCalls;
    @FXML  private TreeTableColumn<SourceLine, Float> colTime;
    @FXML  private TreeTableColumn<SourceLine, String> colCode;
    @FXML  private TreeTableColumn<SourceLine, String> colComment;

    @FXML  private TableView<Function> tableFunctions;
    @FXML  private TableColumn<Function, String> colFuncName;
    @FXML  private TableColumn<Function, Integer> colFuncCalls;
    @FXML  private TableColumn<Function, Float> colFuncTime;
    
    @FXML  private ToggleGroup groupSourceTime;
    @FXML  private RadioButton radioST_S;
    @FXML  private RadioButton radioST_M;
    @FXML  private RadioButton radioST_U;

    @FXML  private ToggleGroup groupFunctionTime;
    @FXML  private RadioButton radioFT_M;
    @FXML  private RadioButton radioFT_U;
    
    
    private final MainWindowData dataModel;
    private final DoubleProperty treeTableBarWidthProperty = new SimpleDoubleProperty();
    private final DoubleProperty functionTableBarWidthProperty = new SimpleDoubleProperty();

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

        // Main Program Page
        lbPrgName.textProperty().bind(dataModel.nameProperty());
        
        // TreeTableView with Profiler Data
        colLine.setCellValueFactory(new TreeItemPropertyValueFactory<>("lineNo"));
        colLine.setCellFactory(formatTreeLineNo);
        colLine.getStyleClass().add("column-align-right");
        colLine.setReorderable(false);

        colCalls.setCellValueFactory(new TreeItemPropertyValueFactory<>("Calls"));
        colCalls.setCellFactory(formatTreeInt);
        colCalls.getStyleClass().add("column-align-right");
        colCalls.setReorderable(false);

        colTime.setCellValueFactory(new TreeItemPropertyValueFactory<>("Time"));
        colTime.setCellFactory(formatTreeFloat);
        colTime.getStyleClass().add("column-align-right");
        colTime.setReorderable(false);

        colCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("Source"));
        colCode.setCellFactory(formatTreeLevel);
        colCode.getStyleClass().add("column-align-left");
        colCode.setReorderable(false);

        colComment.setCellValueFactory(new TreeItemPropertyValueFactory<>("Comment"));
        colComment.getStyleClass().add("column-align-left");
        colComment.setReorderable(false);
        colComment.prefWidthProperty().bind(SourceTree.widthProperty()
                                       .subtract(colLine.widthProperty())
                                       .subtract(colCalls.widthProperty())
                                       .subtract(colTime.widthProperty())
                                       .subtract(colCode.widthProperty())
                                       .subtract(treeTableBarWidthProperty)
                                       .subtract(2));
       
        colFuncName.setCellValueFactory(new PropertyValueFactory<>("Name"));
        colFuncName.setCellFactory(TableCellLabel.cellFactory(new FunctionConverter()));
        colFuncName.getStyleClass().add("column-align-left");
        
        colFuncCalls.setCellValueFactory(new PropertyValueFactory<>("Calls"));
        colFuncCalls.setCellFactory(formatCellInt);
        colFuncCalls.getStyleClass().add("column-align-right");
        colFuncCalls.setSortable(true);
        
        colFuncTime.setCellValueFactory(new PropertyValueFactory<>("Time"));
        colFuncTime.setCellFactory(formatCellFloat);
        colFuncTime.getStyleClass().add("column-align-right");
        colFuncName.prefWidthProperty().bind(tableFunctions.widthProperty()
                                       .subtract(colFuncCalls.widthProperty())
                                       .subtract(colFuncTime.widthProperty())
                                       .subtract(functionTableBarWidthProperty)
                                       .subtract(2));

        tableFunctions.setItems(dataModel.getFunctionList());
        tableFunctions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableFunctions.getSelectionModel().setCellSelectionEnabled(false);
        tableFunctions.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Function>() {
                @Override
                public void onChanged(Change<? extends Function> change) {
                    while (change.next()) {
                        for (Function func : change.getAddedSubList()) {
                            SourceLine srcLine = func.getData();
                            TreeItem<SourceLine> item = dataModel.findTreeItem(dataModel.getProfilerTree(), srcLine);
                            
                            if (item != null) {
                                dataModel.expandToRoot(item);
                            
                                Platform.runLater(() -> {
                                    int row = SourceTree.getRow(item);
                                    if (row >= 0) {
                                        SourceTree.scrollTo(row);
                                        SourceTree.getSelectionModel().select(item);
                                    }});
                            }
                        }
                    }
                }});
        
        stage.setOnShown(ev -> {
            adjustTableWidth(getVerticalScrollbar(SourceTree), treeTableBarWidthProperty);
            adjustTableWidth(getVerticalScrollbar(tableFunctions), functionTableBarWidthProperty);
        });
        
        groupSourceTime.selectedToggleProperty().addListener(this::handleSourceTime);
        groupFunctionTime.selectedToggleProperty().addListener(this::handleFunctionTime);
    }

    private void adjustTableWidth(ScrollBar bar, DoubleProperty width)
    {
        if (bar == null) return;
        
        width.set(bar.visibleProperty().get() ? bar.getWidth() : 0);
        bar.visibleProperty().addListener((obs, oldVal, newVal) -> {  
                width.set(newVal ? bar.getWidth() : 0);
            });
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
                    clearMessage();
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
            if (fh != null) {
                String fileName = fh.getName();
                String filePath = fh.getParent();
             
                int pos=fileName.lastIndexOf('.');
                if (pos != -1) fileName=fileName.substring(0,pos);
                dataModel.setProgramName(fileName+".bas");
            
                execErrandLoadSource(dataModel.mainSource, filePath, fileName,
                        this::loadSourceSucceeded, this::taskFailed);
            }
            
        } else if (event.getSource() == miExit) {
            close();

        // Help Menu
        } else if (event.getSource() == miAbout) {
            AboutController ctrl = new AboutController();
            ctrl.show();
        }
    }
    
    protected void handleSourceTime(ObservableValue<? extends Toggle> observable,
                                       Toggle oldBtn, Toggle newBtn) {
        if (newBtn == radioST_S) {
            colTime.setText("Time [s]");
            dataModel.setSourceTimeScaler(1);
        } else if (newBtn == radioST_M) {
            colTime.setText("Time [ms]");
            dataModel.setSourceTimeScaler(2);
        } else if (newBtn == radioST_U) {
            colTime.setText("Time [µs]");
            dataModel.setSourceTimeScaler(3);
        }
        SourceTree.refresh();
    }
    protected void handleFunctionTime(ObservableValue<? extends Toggle> observable,
                                       Toggle oldBtn, Toggle newBtn) {
        if (newBtn == radioFT_M) {
            colFuncTime.setText("Time [ms]");
            dataModel.setFunctionTimeScaler(2);
        } else if (newBtn == radioFT_U) {
            colFuncTime.setText("Time [µs]");
            dataModel.setFunctionTimeScaler(3);
        }
        tableFunctions.refresh();
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
     * Callbacks to format table cells
     */
    private Callback<TableColumn<Function, Integer>, TableCell<Function, Integer>> formatCellInt = (tableColumn) -> {
        TableCell<Function, Integer> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                Function func = this.getTableRow().getItem();
                if (empty || item==null || func==null) return;

                this.setText(String.format("%d", item));
            }
        };
        return tableCell;
    };

    private Callback<TableColumn<Function, Float>, TableCell<Function, Float>> formatCellFloat = (tableColumn) -> {
        TableCell<Function, Float> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                Function func = this.getTableRow().getItem();
                if (empty || item==null || func==null) return;

                switch (dataModel.getFunctionTimeScaler()) {
                    case 2:
                        this.setText(String.format("%.2f", item/1000));
                        break;
                    case 3:
                        this.setText(String.format("%.0f", item));
                        break;
                }
            }
        };
        return tableCell;
    };
    
    /**
     * Tree table callbacks to format tree table cells
     */
    private Callback<TreeTableColumn<SourceLine, Integer>, TreeTableCell<SourceLine, Integer>> formatTreeLineNo = (tableColumn) -> {
        TreeTableCell<SourceLine, Integer> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                SourceLine srcLine = this.getTableRow().getItem();
                if (empty || item==null || srcLine==null) return;

                if (srcLine.isCodeLine())
                    this.setText(String.format("%d", item));
            }
        };
        return tableCell;
    };

    private Callback<TreeTableColumn<SourceLine, Integer>, TreeTableCell<SourceLine, Integer>> formatTreeInt = (tableColumn) -> {
        TreeTableCell<SourceLine, Integer> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                
                SourceLine srcLine = this.getTableRow().getItem();
                if (empty || item==null || srcLine==null) return;
                
                if (srcLine.isCodeLine() || srcLine.isHeader())
                    this.setText(String.format("%d", item));
            }
        };
        return tableCell;
    };

    private Callback<TreeTableColumn<SourceLine, Float>, TreeTableCell<SourceLine, Float>> formatTreeFloat = (tableColumn) -> {
        TreeTableCell<SourceLine, Float> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                SourceLine srcLine = this.getTableRow().getItem();
                if (empty || item==null || srcLine==null) return;
                
                if (srcLine.isCodeLine() || srcLine.isHeader()) {
                    switch (dataModel.getSourceTimeScaler()) {
                        case 1:
                            this.setText(String.format("%.3f", item/1000000));
                            break;
                        case 2:
                            this.setText(String.format("%.2f", item/1000));
                            break;
                        case 3:
                            this.setText(String.format("%.0f", item));
                            break;
                    }
                }
            }
        };
        return tableCell;
    };

    private Callback<TreeTableColumn<SourceLine, String>, TreeTableCell<SourceLine, String>> formatTreeLevel = (tableColumn) -> {
        TreeTableCell<SourceLine, String> tableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                SourceLine srcLine = this.getTableRow().getItem();
                if (empty || item==null || srcLine==null) return;

                if (srcLine.isCodeLine() && srcLine.getLevel()>0) {
                    String format=String.format("%%%ds%%s",srcLine.getLevel()*4);
                    this.setText(String.format(format,"",item));
                } else
                    this.setText(item);
            }
        };
        return tableCell;
    };

    // ---------------------------------------------------------------------------------------- 
    //                          task helper functions
    // ---------------------------------------------------------------------------------------- 
    private void loadSourceSucceeded(WorkerStateEvent ev)
    {
        dataModel.updateProfilerTree();
        SourceTree.setRoot(dataModel.getProfilerTree());
        SourceTree.setShowRoot(false);
        dataModel.updateFunctionList();
        showSuccess("Data successfully loaded!");
    }

    private void taskFailed(WorkerStateEvent ev)
    {
        Throwable ex = ev.getSource().getException();
        String errormsg = ex == null ? "Loading of data failed!" : ex.getLocalizedMessage();
        showError(errormsg);
    }

    // ---------------------------------------------------------------------------------------- 
    //                          private inner class for CellFactory
    // ---------------------------------------------------------------------------------------- 
    
    private final Image imgSub = new Image(getClass().getResource("/images/16_sub.png").toExternalForm(), 22, 22, false, false);
    private final Image imgFunction = new Image(getClass().getResource("/images/16_function.png").toExternalForm(), 22, 22, false, false);

    private class FunctionConverter
    extends ObjectConverter<Function>
    {
        public FunctionConverter() {
            super();
        }

        @Override
        public String getString(Function object) {
            return object.getName();
        }

        @Override
        public Image getImage(Function object) {
            return object.isFunction() ? imgFunction : imgSub;
        }
    }

}

/*
 * Copyright (C) 2022 grimm
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
package CMM2Profiler.gui;

import CMM2Profiler.core.Function;
import CMM2Profiler.core.Source;
import CMM2Profiler.core.SourceFile;
import CMM2Profiler.core.SourceLine;
import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 *
 * @author grimm
 */
public class MainWindowData
{
    private final StringProperty programName = new SimpleStringProperty();

    private final ObservableList<Function> functionList =  FXCollections.observableArrayList();

    private final StringProperty errorMsg = new SimpleStringProperty();
    private final StringProperty successMsg = new SimpleStringProperty();
    
    protected Source mainSource = new Source();
    private TreeItem<SourceLine> treeRoot=null;
    private ArrayList<TreeItem<SourceLine>> expandedItems = new ArrayList<>();
        
    public MainWindowData()
    {
    }
    
    public String getProgramName() { return programName.get(); }
    public void setProgramName(String name) { programName.set(name); }

    /**
     * Convinience Functions to access data model of Function List GUI element
     */
    public ObservableList<Function> getFunctionList() { return functionList; }
    public void updateFunctionList()
    {
        functionList.clear();
        functionList.addAll(mainSource.getFunctionList());
    }
    
    /**
     * Convinience Functions to access data model of Profiler Tree Table View
     */
    public TreeItem<SourceLine> getProfilerTree() { return treeRoot; }
    public void updateProfilerTree()
    {
        SourceLine header;

        header = SourceLine.createSourceHeader(programName.get());
        TreeItem<SourceLine> root = new TreeItem<>(header);
        
        int idx=0;
        for (SourceFile srcFile : mainSource.getStructureMap()) {
            header = SourceLine.createSourceHeader(idx++ == 0 ? "Main" : srcFile.getPath());
            TreeItem<SourceLine> include = new TreeItem<>(header);
            root.getChildren().add(include);
            
            boolean processEndFunction=false;
            TreeItem<SourceLine> parent = include;
            for (int lineno = srcFile.getFirstLine(); lineno<srcFile.getLastLine(); lineno++) {
                SourceLine srcLine = mainSource.getSourceLine(lineno);
                TreeItem<SourceLine> codeLine = new TreeItem<>(srcLine);

                if (processEndFunction) {
                    if (!srcLine.isEmpty()) {
                        parent = include;
                        processEndFunction=false;
                    }
                }
                
                parent.getChildren().add(codeLine);

                if (srcLine.isFunction()) {
                    parent = codeLine;
                } else if (srcLine.isEndFunction()) {
                    processEndFunction=true;
                }
            }
        }
        treeRoot=root;
    }

    public TreeItem<SourceLine> findTreeItem (TreeItem<SourceLine> node, SourceLine target)
    {
        if (node.getValue() == target)
            return node;

        for (TreeItem<SourceLine> child : node.getChildren()) {
            TreeItem<SourceLine> result = findTreeItem(child, target);
            if (result != null) 
                return result;
        }
        return null;
    }

    public void expandToRoot(TreeItem<SourceLine> item)
    {
        TreeItem<SourceLine> current = item.getParent();
        while (current != null) {
            current.setExpanded(true);
            current = current.getParent();
        }
    }
    
    /*
    public void calcRuntime(SourceFile source)
    {
        int idx=0;
                
        while (idx < source.getLastLineNo()) {
            SourceLineData sl = source.getSourceLineData(idx);
            String codeline=sl.getCodeLine().toLowerCase();
            
            if (codeline.startsWith("function ") || codeline.startsWith("sub")) {
                
            } else if (codeline.startsWith("if ") || codeline.startsWith("else if ")) {
                
            } else if (codeline.startsWith("else")) {
                
            } else if (codeline.startsWith("case")) {
                
            } else if (codeline.startsWith("for")) {
                
                // closed by next
            } else if (codeline.startsWith("do")) {
                
                // closed by loop
            }
        }
    }
    */
    
    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty nameProperty()        { return programName; }
    public StringProperty errorMsgProperty()    { return errorMsg; }
    public StringProperty successMsgProperty()  { return successMsg; }

}

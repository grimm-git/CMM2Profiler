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

import CMM2Profiler.core.Node;
import CMM2Profiler.core.ProfilerData;
import CMM2Profiler.core.SourceFile;
import CMM2Profiler.core.SourceLineData;
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
    private final StringProperty nameProgram = new SimpleStringProperty();
    private final ObservableList<String> listCodelines = FXCollections.observableArrayList();
    private final ObservableList<SourceLineData> sourceList = FXCollections.observableArrayList();
    private final ObservableList<String> listSFN =  FXCollections.observableArrayList();
    private final StringProperty selectedSFN = new SimpleStringProperty();

    private final StringProperty errorMsg = new SimpleStringProperty();
    private final StringProperty successMsg = new SimpleStringProperty();
    private TreeItem<ProfilerData> treeRoot=null;
            
    ArrayList<ProfilerData> dataProfiler = new ArrayList<>();
    Node<ProfilerData> functionTree=null;
    
    SourceFile mainSource = new SourceFile();
    ArrayList<SourceFile> includes = new ArrayList<>();

    public MainWindowData()
    {
        selectedSFN.set("Source");
    }
    
    public String getProgramName() { return nameProgram.get(); }
    public void setProgramName(String name) { nameProgram.set(name); }

    public void addProfilerData(ProfilerData item)
    {
        dataProfiler.add(item);
        listCodelines.add(item.getCodeline());
    }
     
    public ObservableList<String> getCodeLinesList() { return listCodelines; }
    public ObservableList<SourceLineData> getSourceList() { return sourceList; }
    public ObservableList<String> getSFNList() { return listSFN; }
    public TreeItem<ProfilerData> getFunctionTree() { return treeRoot; }

    public void addSFNMain(String name)
    {
        listSFN.clear();
        listSFN.add(name);
        selectedSFN.set(name);
    }

    public void addSFNInclude(String name) { listSFN.add(name); }
    
    public void addNode(ProfilerData parent, ProfilerData data)
    {
        if (parent == null) {
            if (functionTree == null) functionTree = new Node<>(data);
        } else {
            Node<ProfilerData> container = functionTree.findNode(parent);
            if (container == null) return;
            
            Node<ProfilerData> function = container.addChild(new Node<>(data));
        }
    }

    public void updateFunctionTree()
    {
        if (functionTree==null) return;
        
        TreeItem<ProfilerData> root = new TreeItem<>(functionTree.getData());
        
        // all all Functions/Subs
        for (Node<ProfilerData> node : functionTree.getChildren()) {
            TreeItem<ProfilerData> item = new TreeItem<>(node.getData());
            root.getChildren().add(item);

            // add all code lines for this function/sub
            for (Node<ProfilerData> code : node.getChildren()) {
                TreeItem<ProfilerData> child = new TreeItem<>(code.getData());
                item.getChildren().add(child);
            }        
        }
        treeRoot=root;
    }

    public void updateSourceList(SourceFile source)
    {
        sourceList.clear();
        sourceList.addAll(source.getSourceList());
    }
    
    public void setProfilerDataInSource(SourceFile source)
    {
        String srcName = source.getFilePath();
        if (source == mainSource) srcName = "";
        
        for (ProfilerData data : dataProfiler) {
            if (srcName.equals(data.getSourceFile())) {
                SourceLineData sl = source.getSourceLineData(data.getLineNo()-1);
                if (sl == null) continue;
                sl.setExecTime(data.getExecTime());
                sl.setNoOfCalls(data.getCallsCnt());
            }
        }
    }
    
    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty nameProperty()        { return nameProgram; }
    public StringProperty errorMsgProperty()    { return errorMsg; }
    public StringProperty successMsgProperty()  { return successMsg; }
    public StringProperty selectedSFNProperty()  { return selectedSFN; };

}

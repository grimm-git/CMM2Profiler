/*
 * Copyright (C) 2019 Matthias Grimm <matthiasgrimm@users.sourceforge.net>
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
package CMM2Profiler.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 * @param <T>
 */
public class Node<T>
{
    private T data = null;
    private Node<T> parent = null;
    private List<Node<T>> children = new ArrayList<>();
    
    public Node(T data)
    {
        this.data = data;
    }
    
    public synchronized Node<T> addChild(Node<T> child)
    {
        child.setParent(this);
        this.children.add(child);
        return child;
    }
    
    public synchronized void addChildren(List<Node<T>> children)
    {
        children.forEach(each -> each.setParent(this));
        this.children.addAll(children);
    }
    
    public List<Node<T>> getChildren()
    {
        return children;
    }
    
    public boolean isLeaf()
    {
        return children.isEmpty();
    }
    
    public synchronized T getData()
    {
        return data;
    }
    
    public synchronized void setData(T data)
    {
        this.data = data;
    }
    
    public synchronized void setParent(Node<T> parent)
    {
        this.parent = parent;
    }
    
    public synchronized Node<T> getParent()
    {
        return parent;
    }
    
    public synchronized Node<T> getRoot()
    {
        if (parent == null)
            return this;
        
        return parent.getRoot();
    }
    
    public synchronized Node<T> findNode(T data)
    {
        if (this.data == data) return this;
        if (this.data != null && this.data.equals(data)) return this;
        
        for (Node<T> item : getChildren()) {
            Node<T> found = item.findNode(data);
            if (found != null) return found;
        }
        return null;
    }

    public synchronized T findData(Predicate<T> callback)
    {
        if (callback.test(data)) return data;

        for (Node<T> item : getChildren()) {
            T found = item.findData(callback);
            if (found != null) return found;
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------- 
    //                               Static Debugging methods
    // ---------------------------------------------------------------------------------------- 

    public static <T> void printTree(Node<T> node, String appender)
    {
        System.out.println(appender + node.getData());
        node.getChildren().forEach(each -> printTree(each, appender + appender));
    }
}

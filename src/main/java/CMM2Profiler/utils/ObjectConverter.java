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

import javafx.scene.image.Image;

/**
 * This interface provides converter methods for a custom object in a way a Cell can
 * use it. The main task is to transform the custom object into something, the Cell can
 * display.<p>
 * 
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 * @param <T>  Type of the custom object and base type of the ListView or TableView
 */
public abstract class ObjectConverter<T>
{
    private final Image image;
    
    public ObjectConverter() {
        image = null;
    }

    public ObjectConverter(String imgName) {
        if (imgName == null)
            throw new NullPointerException("Cell image name = null!");

        image = new Image(getClass().getResource("/images/" + imgName).toExternalForm(),
                22, 22, false, false);
    }
    
    public abstract String getString(T item);
    public T setString(T item, String value) { return null; }
    public String getTitle(T item) { return ""; }
    
    public Image getImage(T item) {
        return image;
    }

    /**
     * This method could be overwritten to set an extra style class for each cell
     * 
     * @param item   Data item of the cell
     * @return       String with a style class name. The value will be ignored,
     *               if the return value is empty or null. There are no further checks for a
     *               correct style name. Wron data could lead to unforeseen behaviour.
     */
    public String getStyleClass(T item) {
        return "";
    }

    public boolean isEditable(T item) {
        return false;
    }

    public boolean isHighlighted(T item) {
        return false;
    }
    
    public String getFilter(T item) {
        return "";
    }
    
    /**
     * This method triggers the self destruction of the object. The object has to know how this
     * could be done. The converter just triggers the process.<p>
     * This is a very special use case and will mostly <b>not</b> be needed.
     * 
     * @param item   Itme, that should be destroyed/removed
     */
    public void delete(T item) { }
}

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

import CMM2Profiler.core.ObjectConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

/**
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 * @param <T>  Type of data in ListCell
 */
public class LabelListCell<T>
extends ListCell<T>
{
    /**
     * Provides a {@link ListCell} that displays an arbitrary object in a Label list cell
     * 
     * @param <T>  Native type of the ListView
     * @param converter  ObjectCOnverter to translate an object into Label information
     * @return A {@link Callback} that can be inserted into the
     *      {@link ListView#cellFactoryProperty() cell factory property} of a
     *      ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> cellFactory(ObjectConverter<T> converter) {
        return list -> { return new LabelListCell<>(converter); };
    }

    /***************************************************************************
     * Public API                                                              *
     **************************************************************************/

    Label label;

    private ObjectProperty<ObjectConverter<T>> converter =
            new SimpleObjectProperty<>(this, "converter");

    /**
     * The {@link ObjectConverter} property.
     * @return  Object converter object
     */
    public final ObjectProperty<ObjectConverter<T>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link ObjectConverter} to be used in this cell.
     *
     * @param value  Reference to an ObjectConverter object
     */
    public final void setConverter(ObjectConverter<T> value) {
        converterProperty().set(value);
    }

    /**
     * @return the {@link ObjectConverter} used in this cell.
     */
    public final ObjectConverter<T> getConverter() {
        return converterProperty().get();
    }

    public LabelListCell() { this(null); }
    public LabelListCell(ObjectConverter<T> converter)
    {
        setConverter(converter);
        label = new Label();
    }
    
    @Override
    public void updateItem(T item, boolean empty)
    {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);   

        } else {
            Image img = getItemImg(item, getConverter());
            label.setGraphic(img == null ? null : new ImageView(img));
            label.setText(getItemText(item, getConverter()));
            
            this.getStyleClass().remove("label-cell-highlight");
            if (isItemHighlighted(item, getConverter()))
                this.getStyleClass().add("label-cell-highlight");
            
            setGraphic(label);
            setText(null);
        }
   }
 
    private <T> String getItemText(T item, ObjectConverter<T> converter) {
        return converter == null ?
            item == null ? "" : item.toString() : converter.getString(item);
    }

    private <T> Image getItemImg(T item, ObjectConverter<T> converter) {
        return converter == null ? null : converter.getImage(item);
    }

    private <T> boolean isItemHighlighted(T item, ObjectConverter<T> converter) {
        return converter == null ? null : converter.isHighlighted(item);
    }
}

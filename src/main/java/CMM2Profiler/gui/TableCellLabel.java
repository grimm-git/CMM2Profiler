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

import CMM2Profiler.utils.ObjectConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

/**
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 * @param <S>  Type of data in TableView
 * @param <T>  Type of data in TableCell
 */
public class TableCellLabel<S,T>
extends TableCell<S,T>
{
    /**
     * Provides a {@link TableCell} that displays an arbitrary object in a Label table cell
     * 
     * @param <S>  Native type of the TableView
     * @param <T>  Native type of the TableCell
     * @param converter  ObjectConverter to translate an object into Label information
     * @return A {@link Callback} that can be inserted into the
     *      {@link TableView#cellFactoryProperty() cell factory property} of a
     *      TableView.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> cellFactory(ObjectConverter<S> converter) {
        return cell -> { return new TableCellLabel<>(converter); };
    }

    /***************************************************************************
     * Public API                                                              *
     **************************************************************************/

    Label label;

    private final ObjectProperty<ObjectConverter<S>> converter =
            new SimpleObjectProperty<>(this, "converter");

    /**
     * The {@link ObjectConverter} property.
     * @return  Object converter object
     */
    public final ObjectProperty<ObjectConverter<S>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link ObjectConverter} to be used in this cell.
     *
     * @param value  Reference to an ObjectConverter object
     */
    public final void setConverter(ObjectConverter<S> value) {
        converterProperty().set(value);
    }

    /**
     * @return the {@link ObjectConverter} used in this cell.
     */
    public final ObjectConverter<S> getConverter() {
        return converterProperty().get();
    }

    public TableCellLabel() { this(null); }
    public TableCellLabel(ObjectConverter<S> converter)
    {
        setConverter(converter);
        label = new Label();
    }
    
    @Override
    public void updateItem(T item, boolean empty)
    {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);   

        this.getStyleClass().remove("label-cell-highlight");

        S row = getTableRow().getItem();
        if (empty || item == null || row == null) return;

        ObjectConverter<S> converter = getConverter();
        if (converter == null) {
            label.setText(item.toString());
            
        } else {
            label.setGraphic(new ImageView(converter.getImage(row)));
            label.setText(converter.getString(row));
            
            if (converter.isHighlighted(row))
                this.getStyleClass().add("label-cell-highlight");
        }
           
        setGraphic(label);
        setText(null);
   }
}

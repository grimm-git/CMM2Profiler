/*
 * Copyright (C) 2026 Matthias Grimm <matthiasgrimm@users.sourceforge.net>
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

/**
 * Module descriptor of the CMM2Profiler application.
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
module CMM2Profiler
{
    requires javafx.controls;   // pulls in javafx.base and javafx.graphics
    requires javafx.fxml;

    // javafx.graphics instantiates Main (extends Application) reflectively
    opens CMM2Profiler to javafx.graphics;

    // FXMLLoader injects the @FXML annotated fields of the controllers
    opens CMM2Profiler.gui to javafx.fxml;

    // PropertyValueFactory and TreeItemPropertyValueFactory read the
    // properties of Function and SourceLine by reflection
    opens CMM2Profiler.core to javafx.base;
}

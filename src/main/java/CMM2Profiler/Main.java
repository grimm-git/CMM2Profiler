/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
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
package CMM2Profiler;

import CMM2Profiler.gui.MainWindowController;
import java.io.IOException;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 *
 * @author Matthias Grimm <codingjoker@web.de>
 */
public class Main
extends Application
{

    @Override
    public void start(Stage stage) throws IOException
    {
        setUserAgentStylesheet(STYLESHEET_MODENA);
        
        MainWindowController mw = new MainWindowController(stage);
        stage.getIcons().add(mw.getImageResource("cmm2profiler_16x16.png"));
        stage.getIcons().add(mw.getImageResource("cmm2profiler_32x32.png"));
        stage.getIcons().add(mw.getImageResource("cmm2profiler_64x64.png"));
        stage.getIcons().add(mw.getImageResource("cmm2profiler_128x128.png"));
        stage.getIcons().add(mw.getImageResource("cmm2profiler_256x256.png"));
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);       // start JavaFX Thread
        
        Registry Reg = Registry.get();
        Reg.close();
    }
}

/*Copyright (C) 2013  NaikSoftware

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software*/
package ua.naiksoftware.bluwar;

import java.util.Vector;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class Main extends MIDlet implements Runnable, Initializer {

    private Game game;
    private Display display;
    private Vector mapHeaders;
    private WaitScreen waitScreen;

    public void initApp() {
    }

    public void startApp() {
        display = Display.getDisplay(this);
        waitScreen = new WaitScreen(this, 5);
        display.setCurrent(waitScreen);
        new Thread(this).start();
    }

    public void run() {
        mapHeaders = getMaps();
        game = new Game((String) mapHeaders.elementAt(0), waitScreen);
        waitScreen.setProgress(5, "");
    }

    // Очистка памяти и старт игры.
    public void complete() {
        display.setCurrent(game);
        waitScreen = null;
        System.gc();
    }

    private Vector getMaps() {
        Vector v = new Vector();
        v.addElement("/map1.bwh");
        return v;
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        notifyDestroyed();
    }
}

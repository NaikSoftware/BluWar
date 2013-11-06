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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;

public class WaitScreen extends Canvas {

    private final Font fntTitle = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
    private final Font fntLabel = Font.getDefaultFont();
    private final int wScr, hScr;
    private final int wBar, hBar, xBar, yBar;
    private final int xLabel, yLabel;
    private int progress;
    private final int stepProgress, maxProgress;
    private String label;
    private static final int COLOR_BG = 0x127012;
    private static final int COLOR_RECT = 0xdddddd;
    private static final int COLOR_PROGRESS = 0xffffff;
    private static final int COLOR_TITLE = 0x7777ff;

    private final Initializer initializer;

    public WaitScreen(Initializer initial, int max) {
        setFullScreenMode(true);
        initializer = initial;
        label = "Please wait...";
        wScr = getWidth();
        hScr = getHeight();
        xBar = wScr / 10;
        yBar = hScr / 3 * 2;
        wBar = xBar * 8;
        hBar = hScr / 60;
        stepProgress = wBar / max;
        maxProgress = max;
        xLabel = wScr / 2;
        yLabel = hScr / 3;
        progress = 0;
        repaint();
    }

    public void paint(Graphics g) {
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, wScr, hScr);
        g.setColor(COLOR_TITLE);
        g.setFont(fntTitle);
        g.drawString("Loading", wScr / 2, 5, Graphics.HCENTER | Graphics.TOP);
        g.setColor(COLOR_PROGRESS);
        g.setFont(fntLabel);
        g.drawString(label, xLabel, yLabel, Graphics.HCENTER | Graphics.TOP);
        g.setColor(COLOR_RECT);
        g.drawRect(xBar - 2, yBar - 2, wBar + 3, hBar + 3);
        g.setColor(COLOR_PROGRESS);
        g.fillRect(xBar, yBar, progress * stepProgress, hBar);
    }

    public void setProgress(int p, String l) {
        progress = p;
        label = l;
        repaint();
        if (p >= maxProgress) {
            initializer.complete();
        }
    }
}

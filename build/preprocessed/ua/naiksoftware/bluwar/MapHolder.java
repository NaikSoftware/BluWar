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

//import filelog.Log;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import ua.naiksoftware.bluwar.maps.Map;
import java.io.IOException;
import javax.microedition.lcdui.game.Sprite;

public class MapHolder implements Runnable {

    private static final String tag = "MapHolder";

    // Пеpеменные для отрисовки одного тайла ( fillTile(...) ).
    private final byte blockSize;//px
    private final short[][] blocks;

    private static final byte VOID = -1;
    private static final byte FILL = -2;

    private static final int BG = 0x127712;// Фoн.

    // Содержит 2Д массивы пикселей детализированных блоков.
    private Vector detalied;
    private Image land;

    // Переменные для отрисовки сгенерированных тайлов карты ( draw(...) ).
    private int numRows, numCols;
    private final int wTile, hTile;
    private int xDrawPoint, yDrawPoint;
    private int xLastMap, yLastMap;
    private final int numOfTiles;
    private final Image[] tiles;
    private final int[][] matrixTiles;

    // Переменные для динамической подгрузки карты.
    // Расстояние, до конца тайла, после которого начинается подгрузка.
    private final int SHIFT_LOAD_W, SHIFT_LOAD_H;
    private boolean loadLeft = false, loadUp = false, loadRight = false, loadDown = false;
    private boolean mapDisplayed;
    private int savedX, savedY, savedXDrawPoint, savedYDrawPoint;

    public MapHolder(Map map, int scrW, int scrH, int viewX, int viewY) {
        blockSize = map.getBlockSize();
        //Log.d(tag, "blockSize = " + blockSize);
        final short detaliedStartCounts = map.getDetaliedStartCounts();
        blocks = map.getBlocks();
        boolean[][][] detaliedBlocks = map.getDetaliedBlocks();
        detalied = new Vector(detaliedStartCounts);
        for (short i = 0; i < detaliedStartCounts; i++) {
            detalied.addElement(detaliedBlocks[i]);
        }
        detaliedBlocks = null;
        System.gc();
        // Лучше всего, если экран 3:4.
        numRows = 4;// 4 default
        while (scrH % numRows != 0) {
            numRows++;
        }
        numCols = 3;// 3 default
        while (scrW % numCols != 0) {
            numCols++;
        }
        wTile = scrW / numCols;
        hTile = scrH / numRows;
        numRows += 2;// Буфер
        numCols += 2;// на один тайл с каждой стороны экрана.
        numOfTiles = numRows * numCols;
        SHIFT_LOAD_W = wTile / 2;
        SHIFT_LOAD_H = hTile / 2;
        tiles = new Image[numOfTiles];
        matrixTiles = new int[numRows][numCols];
        for (int i = 0; i < numOfTiles; i++) {
            tiles[i] = Image.createImage(wTile, hTile);
        }
        try {
            land = Image.createImage("/land.png");
        } catch (IOException e) {
        }
        //Log.d(tag, "scrW=" + scrW + " scrH=" + scrH);
        //Log.d(tag, "numRows=" + numRows + " numCols=" + numCols);
        //Log.d(tag, "wTile=" + wTile + " hTile=" + hTile);
        //Log.d(tag, "viewX="+viewX+" viewY="+viewY);
        mapDisplayed = true;
        new Thread(this).start();
        initNewViewport(viewX, viewY);
    }

    // x, y - координаты на карте с которых начинается рендеринг.
    public final void initNewViewport(int x, int y) {
        xDrawPoint = -wTile;
        yDrawPoint = -hTile;
        xLastMap = x;
        yLastMap = y;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                fillTile(tiles[i * numCols + j].getGraphics(),
                        x + xDrawPoint + j * wTile,
                        y + yDrawPoint + i * hTile);
                matrixTiles[i][j] = (i * numCols + j);
                //tiles[i * numCols + j].getGraphics().setColor(0x0000ff);
                //tiles[i * numCols + j].getGraphics().drawRect(0, 0, wTile - 1, hTile - 1);
                //tiles[i * numCols + j].getGraphics().drawString(Integer.toString(i * numCols + j), 3, 3, Graphics.LEFT | Graphics.TOP);
            }
        }
    }

    /*
     Отрисовываем отрендеренные тайлы и запускаем рендеринг новых если нужно
     в отдельном треде.
     */
    public void draw(Graphics g, int x, int y) {
        xDrawPoint += (xLastMap - x);
        yDrawPoint += (yLastMap - y);

        if (xDrawPoint > (-SHIFT_LOAD_W)) {
            if (!loadLeft) {
                savedX = x;
                savedY = y;
                savedXDrawPoint = xDrawPoint;
                savedYDrawPoint = yDrawPoint;
                loadLeft = true;
            }// else if (xDrawPoint >= (-5)) {}
        } else if (xDrawPoint < -(wTile + wTile - SHIFT_LOAD_W)) {
            if (!loadRight) {
                savedX = x;
                savedY = y;
                savedXDrawPoint = xDrawPoint;
                savedYDrawPoint = yDrawPoint;
                loadRight = true;
            }// else if (xDrawPoint <= -(wTile * 2 - 5)) {}
        }
        if (yDrawPoint > (-SHIFT_LOAD_H)) {
            if (!loadUp) {
                savedX = x;
                savedY = y;
                savedXDrawPoint = xDrawPoint;
                savedYDrawPoint = yDrawPoint;
                loadUp = true;
            }// else if...
        } else if (yDrawPoint < -(hTile + SHIFT_LOAD_H)) {
            if (!loadDown) {
                savedX = x;
                savedY = y;
                savedXDrawPoint = xDrawPoint;
                savedYDrawPoint = yDrawPoint;
                loadDown = true;
            }// else if...
        }
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                g.drawImage(tiles[matrixTiles[i][j]], xDrawPoint + j * wTile, yDrawPoint + i * hTile, Graphics.LEFT | Graphics.TOP);
            }
        }
        // end method
        xLastMap = x;
        yLastMap = y;
    }

    // Генерация тайлов в отдельном треде.
    public void run() {
        final int[] savedRow = new int[numCols];
        final int[] savedColumn = new int[numRows];
        int tmpTileId;
        while (mapDisplayed) {
            //--------------------
            // LEFT GEN
            if (loadLeft) {
                for (int i = 0; i < numRows; i++) {
                    tmpTileId = matrixTiles[i][numCols - 1];
                    savedColumn[i] = tmpTileId;
                    fillTile(tiles[tmpTileId].getGraphics(),
                            savedX + savedXDrawPoint - wTile,
                            savedY + savedYDrawPoint + (hTile * i));
                    //Log.d(tag, "tmpTileId=" + tmpTileId);

                }
                for (int i = 0; i < numRows; i++) {
                    for (int j = (numCols - 2); j > (-1); j--) {
                        matrixTiles[i][j + 1] = matrixTiles[i][j];
                        //Log.d(tag, "copy from [" + i + "][" + (j+1) + "] to [" + i + "][" + j + "]");
                    }
                }
                for (int i = 0; i < numRows; i++) {
                    matrixTiles[i][0] = savedColumn[i];
                }
                xDrawPoint -= wTile;
                savedXDrawPoint -= wTile;
                loadLeft = false;
                //--------------------
                // RIGHT GEN
            } else if (loadRight) {
                for (int i = 0; i < numRows; i++) {
                    tmpTileId = matrixTiles[i][0];
                    savedColumn[i] = tmpTileId;
                    fillTile(tiles[tmpTileId].getGraphics(),
                            savedX + savedXDrawPoint + (wTile * numCols),
                            savedY + savedYDrawPoint + (hTile * i));
                    //Log.d(tag, "tmpTileId=" + tmpTileId);

                }
                for (int i = 0; i < numRows; i++) {
                    for (int j = 1; j < numCols; j++) {
                        matrixTiles[i][j - 1] = matrixTiles[i][j];
                        //Log.d(tag, "copy from [" + i + "][" + (j-1) + "] to [" + i + "][" + j + "]");
                    }
                }
                for (int i = 0; i < numRows; i++) {
                    matrixTiles[i][numCols - 1] = savedColumn[i];
                }
                xDrawPoint += wTile;
                savedXDrawPoint += wTile;
                loadRight = false;
            } //--------------------
            // UP GEN
            else if (loadUp) {
                for (int i = 0; i < numCols; i++) {
                    tmpTileId = matrixTiles[numRows - 1][i];
                    savedRow[i] = tmpTileId;
                    fillTile(tiles[tmpTileId].getGraphics(),
                            savedX + savedXDrawPoint + (wTile * i),
                            savedY + savedYDrawPoint - hTile);
                    //Log.d(tag, "tmpTileId=" + tmpTileId);

                }
                for (int i = 0; i < numCols; i++) {
                    for (int j = (numRows - 2); j > (-1); j--) {
                        matrixTiles[j + 1][i] = matrixTiles[j][i];
                        //Log.d(tag, "copy from [" + (j + 1) + "][" + i + "] to [" + j + "][" + i + "]");
                    }
                }
                for (int i = 0; i < numCols; i++) {
                    matrixTiles[0][i] = savedRow[i];
                }
                yDrawPoint -= wTile;
                loadUp = false;
                //--------------------
                // DOWN GEN
            } else if (loadDown) {
                for (int i = 0; i < numCols; i++) {
                    tmpTileId = matrixTiles[0][i];
                    savedRow[i] = tmpTileId;
                    fillTile(tiles[tmpTileId].getGraphics(),
                            savedX + savedXDrawPoint + (wTile * i),
                            savedY + savedYDrawPoint + (hTile * numRows));
                    //Log.d(tag, "tmpTileId=" + tmpTileId);

                }
                for (int i = 0; i < numCols; i++) {
                    for (int j = 1; j < numRows; j++) {
                        matrixTiles[j - 1][i] = matrixTiles[j][i];
                        //Log.d(tag, "copy from [" + (j - 1) + "][" + i + "] to [" + j + "][" + i + "]");
                    }
                }
                for (int i = 0; i < numCols; i++) {
                    matrixTiles[numRows - 1][i] = savedRow[i];
                }
                yDrawPoint += wTile;
                loadDown = false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void stopMapDisplay() {
        mapDisplayed = false;
    }

    private void fillTile(Graphics g, int xOnMap, int yOnMap) {
        //Log.d(tag, "fillTile (map coords): " + xOnMap + ", " + yOnMap);
        // Проверяем, не находится ли тайл за границей видимости.
        //if (xOnMap < (-wTile) || xOnMap > (blockSize * blocks.length)
        //	|| yOnMap < (-hTile) || yOnMap > (blockSize * blocks[0].length)) {
        //    Log.d(tag, "fillTile fast return");
        //    return;
        //} ----- и без этого отсекается ненужное
        int paintX = xOnMap, paintY = yOnMap;
        int xOnMap2 = xOnMap + wTile, yOnMap2 = yOnMap + hTile;
        if (xOnMap < 0) {
            xOnMap = 0;
        } else if (xOnMap2 > (blockSize * blocks.length)) {
            xOnMap2 = blockSize * blocks.length;
        }
        if (yOnMap < 0) {
            yOnMap = 0;
        } else if (yOnMap2 > (blockSize * blocks[0].length)) {
            yOnMap2 = blockSize * blocks[0].length;
        }
        g.setColor(BG);
        g.fillRect(0, 0, wTile, hTile);
        int currBlockID;
        xOnMap -= (xOnMap % blockSize);
        yOnMap -= (yOnMap % blockSize);
        for (int i = xOnMap; i < xOnMap2; i += blockSize) {
            for (int j = yOnMap; j < yOnMap2; j += blockSize) {
                currBlockID = blocks[i / blockSize][j / blockSize];
                switch (currBlockID) {
                    //Воздух.
                    case VOID:
                        //TODO: закрасить цветом фона?
                        break;
                    //Земля.
                    case FILL:
                        g.drawImage(land, i - paintX, j - paintY, Graphics.LEFT | Graphics.TOP);
                        break;
                    //В одном блоке и земля и воздух. Рисуем попиксельно с тайлов.
                    default:
                        //TODO: закрасить цветом фона?
                        boolean[][] detaliedBlock = (boolean[][]) detalied.elementAt(currBlockID);
                        for (int i2 = 0; i2 < blockSize; i2++) {
                            for (int j2 = 0; j2 < blockSize; j2++) {
                                if (detaliedBlock[i2][j2]) {
                                    g.drawRegion(land, i2, j2, 1, 1, Sprite.TRANS_NONE, i - paintX + i2, j - paintY + j2, Graphics.LEFT | Graphics.TOP);
                                }
                            }
                        }
                }
            }
        }
    }
}

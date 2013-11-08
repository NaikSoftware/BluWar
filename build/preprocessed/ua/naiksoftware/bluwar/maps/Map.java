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

/* Примерно такая может быть карта:
 *
 *   . . . . . . . . . 
 *   . . . . . . . . .
 *   . . . . . . . . .
 *   . . . . . . . . .
 *   . . . % % . . . .
 *   ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈
 *   ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈
 *   ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈
 *   ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈ ≈
 *
 * . — пустые блоки (воздух) VOID
 * ≈ — однородная земля FILL
 * % — детализированные (в блоке и земля и воздух — true/false попиксельный перенос текстуры)
 */
package ua.naiksoftware.bluwar.maps;

import java.io.IOException;
import java.io.InputStreamReader;
//import filelog.Log;

public class Map {

    private String mapName;
    private byte blockSize;//px
    private byte wbl;
    private byte hbl;
    private short detaliedStartCounts;
    private short[][] blocks;
    /* Хранение неоднородных блоков (true - land, false - air).
     * 1 [] — ID детализированного блока
     * 2 [] — номер строки блока
     * 3 [] — номер колонки блока
     * При использовании типа массива byte можно использовать намного большее
     * колличество текстур. На скорость отрисовки сильно не должно повлиять.
     */
    private boolean[][][] detailBlocks;

    private static final byte VOID = -1;
    private static final byte FILL = -2;

    public Map(String mapHeader) {
        initMap(initMapHeader(mapHeader));
    }

    /*
     Формат:
     name:Map 1    — имя карты ддя отображения
     blsize:20     — размер блока карты (текстуры)
     blw:45        — блоков по ширине
     blh:25        — блоков по высоте
     bldet:172     — детализированных блоков
     link:map1.bwm — путь к карте
     Файл должен заканчиваться \n
     */
    private String initMapHeader(String mapHeader) {
        InputStreamReader isr = null;
        //try {
        isr = new InputStreamReader(getClass().getResourceAsStream(mapHeader));
        //} catch (IOException e) {}
        String[] pair;
        pair = readPair(isr);
        if (pair[0].equals(
                "name")) {
            mapName = pair[1];
        } else {
            throw new IllegalArgumentException("Name map not defined.");
        }
        pair = readPair(isr);
        if (pair[0].equals(
                "blsize")) {
            blockSize = (byte) Integer.parseInt(pair[1]);
        } else {
            throw new IllegalArgumentException("Block size not defined.");
        }
        pair = readPair(isr);
        if (pair[0].equals(
                "blw")) {
            wbl = (byte) Integer.parseInt(pair[1]);
        } else {
            throw new IllegalArgumentException("Block in row not defined.");
        }
        pair = readPair(isr);
        if (pair[0].equals(
                "blh")) {
            hbl = (byte) Integer.parseInt(pair[1]);
            blocks = new short[wbl][hbl];
        } else {
            throw new IllegalArgumentException("Block in column not defined.");
        }
        pair = readPair(isr);
        if (pair[0].equals(
                "bldet")) {
            detaliedStartCounts = (short) Integer.parseInt(pair[1]);
            detailBlocks = new boolean[detaliedStartCounts][blockSize][blockSize];
        } else {
            throw new IllegalArgumentException("Detalied blocks in map not defined.");
        }
        pair = readPair(isr);
        if (pair[0].equals(
                "link")) {
            return pair[1];
        } else {
            throw new IllegalArgumentException("Link to map not defined.");
        }
    }


    /*
     Формат:
     f - false
     t - true
     a - air
     l - land
     */
    private void initMap(String pathToMap) {
        InputStreamReader isr = null;
        //try {
        isr = new InputStreamReader(getClass().getResourceAsStream(pathToMap));
        //} catch (IOException e) {}
        int ch;
        short detIndex = 0;
        int arrlen = blockSize * blockSize;
        try {
            for (int i = 0; i < wbl; i++) {
                for (int j = 0; j < hbl; j++) {
                    ch = isr.read();
                    if (ch == 'a') {
                        blocks[i][j] = VOID;
                    } else if (ch == 'l') {
                        blocks[i][j] = FILL;
                    } else if (ch == 't' || ch == 'f') {
                        blocks[i][j] = detIndex;
                        //System.out.println("i=" + i + " j=" + j + " :: " + detIndex);
                        detailBlocks[detIndex][0][0] = ch == 't';
                        for (int k = 1; k < arrlen; k++) {
                            ch = isr.read();
                            if (ch == 't') {
                                detailBlocks[detIndex][k % blockSize][k / blockSize] = true;
                            } else if (ch == 'f') {
                                detailBlocks[detIndex][k % blockSize][k / blockSize] = false;
                            } else {
                                throw new IllegalArgumentException("Unknown character of detalied block (must be t or f): " + ch);
                            }
                        }
                        detIndex++;
                    } else {
                        throw new IllegalArgumentException("Unknown character: " + ch);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Broken map");
        } catch (Exception e) {
            e.printStackTrace();
            //Log.d("BW Map.java", "Other exception:" + e.getMessage());
        }
        //System.out.println("detIndex="+detIndex);
        //System.out.println("detaliedStartCounts="+detaliedStartCounts);
        if (detIndex != detaliedStartCounts) {
            throw new IllegalArgumentException("Bldet not equal with real numbers bldet");
        }
    }

    private String[] readPair(InputStreamReader isr) {
        StringBuffer key = new StringBuffer(), value = new StringBuffer();
        int ch;
        try {
            while ((ch = isr.read()) != ':') {
                key.append((char) ch);
            }
            while ((ch = isr.read()) != '\n') {
                value.append((char) ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d("BW Map.java", "readPair# key=" + key.toString() + " value=" + value.toString());
        return new String[]{key.toString(), value.toString()};
    }

    public boolean[][][] getDetaliedBlocks() {
        return detailBlocks;
    }

    public short[][] getBlocks() {
        return blocks;
    }

    public short getDetaliedStartCounts() {
        return detaliedStartCounts;
    }

    public byte getHbl() {
        return hbl;
    }

    public byte getWbl() {
        return wbl;
    }

    public byte getBlockSize() {
        return blockSize;
    }

    private void init() {

    }
}

package com.holub.life;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 기존 .life 형식(Neighborhood의 Memento가 하는 Java 직렬화)을 PatternFormat에 맞추는 Adapter.
 * 읽기와 쓰기를 Memento에 맡기므로 기존 코드와 호환됨
 */
public class LifeFormatAdapter implements PatternFormat {
    private final int widthInCells;

    // Memento는 isAlive(좌표)만 답할 수 있어서, 읽을 때 격자 전체를 훑어야 함
    public LifeFormatAdapter(int widthInCells) {
        this.widthInCells = widthInCells;
    }

    @Override
    public Set<Point> read(InputStream in) throws IOException {
        Cell.Memento memento = Neighborhood.createEmptyMemento();
        memento.load(in);

        Set<Point> cells = new HashSet<>();
        for (int row = 0; row < widthInCells; row++) {
            for (int column = 0; column < widthInCells; column++) {
                Point here = new Point(column, row);
                if (memento.isAlive(here)) {
                    cells.add(here);
                }
            }
        }
        return cells;
    }

    @Override
    public void write(Set<Point> cells, OutputStream out) throws IOException {
        Cell.Memento memento = Neighborhood.createEmptyMemento();
        for (Point cell : cells) {
            memento.markAsAlive(cell);
        }
        memento.flush(out);
    }
}

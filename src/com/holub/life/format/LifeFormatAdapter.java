package com.holub.life.format;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import com.holub.life.model.Cell;
import com.holub.life.model.CellColor;
import com.holub.life.model.LiveCells;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.PatternFormat;

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
    public String extension() {
        return "life";
    }

    @Override
    public String description() {
        return "Life Files";
    }

    // .life는 색까지 저장함. 색이 없는 예전 파일은 Memento가 기본색으로 답함
    @Override
    public LiveCells read(InputStream in) throws IOException {
        Cell.Memento memento = Neighborhood.createEmptyMemento();
        memento.load(in);

        Map<Point, CellColor> cells = new HashMap<>();
        for (int row = 0; row < widthInCells; row++) {
            for (int column = 0; column < widthInCells; column++) {
                Point here = new Point(column, row);
                if (memento.isAlive(here)) {
                    cells.put(here, memento.colorAt(here));
                }
            }
        }
        return new LiveCells(cells);
    }

    @Override
    public void write(LiveCells cells, OutputStream out) throws IOException {
        Cell.Memento memento = Neighborhood.createEmptyMemento();
        for (Map.Entry<Point, CellColor> cell : cells.toMap().entrySet()) {
            memento.markAsAlive(cell.getKey(), cell.getValue());
        }
        memento.flush(out);
    }
}

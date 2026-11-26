package com.holub.life;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 보드에서 살아 있는 셀들과 각 셀의 색 (x = 열, y = 행).
 * 위치와 색을 한 객체로 묶어서, 보드 밖(파일 형식, Undo 스냅샷)으로 주고받을 때 둘이 어긋나지 않게 함.
 * 불변 객체이므로 그대로 공유해도 됨
 */
public final class LiveCells {
    private final Map<Point, CellColor> cells;

    /** 받은 Map을 복사해서 가짐. 나중에 Map을 고쳐도 이 객체는 바뀌지 않음 */
    public LiveCells(Map<Point, CellColor> cells) {
        this.cells = copyOf(cells);
    }

    /** 색 정보가 없는 위치 목록(예: RLE)을 모두 기본색으로 만듦 */
    public static LiveCells of(Set<Point> positions) {
        Map<Point, CellColor> cells = new HashMap<>();
        for (Point p : positions) {
            cells.put(p, CellColor.DEFAULT);
        }
        return new LiveCells(cells);
    }

    public boolean isAlive(Point position) {
        return cells.containsKey(position);
    }

    /** @throws IllegalArgumentException 그 자리의 셀이 죽어 있으면 */
    public CellColor colorAt(Point position) {
        CellColor color = cells.get(position);
        if (color == null) {
            throw new IllegalArgumentException("no live cell at " + position);
        }
        return color;
    }

    /** 살아 있는 셀의 위치. 새 복사본이므로 고쳐도 이 객체는 바뀌지 않음 */
    public Set<Point> positions() {
        Set<Point> copy = new HashSet<>();
        for (Point p : cells.keySet()) {
            copy.add(new Point(p));
        }
        return copy;
    }

    /** 위치별 색. 새 복사본이므로 고쳐도 이 객체는 바뀌지 않음 (일부만 바꾼 새 LiveCells를 만들 때 씀) */
    public Map<Point, CellColor> toMap() {
        return copyOf(cells);
    }

    public int size() {
        return cells.size();
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    // Point는 바꿀 수 있는 객체라서 Point도 하나씩 복사함
    private static Map<Point, CellColor> copyOf(Map<Point, CellColor> cells) {
        Map<Point, CellColor> copy = new HashMap<>();
        for (Map.Entry<Point, CellColor> cell : cells.entrySet()) {
            copy.put(new Point(cell.getKey()), Objects.requireNonNull(cell.getValue()));
        }
        return copy;
    }

    // 위치와 색이 모두 같으면 같음. LifeGame이 편집으로 보드가 바뀌었는지 확인할 때 씀
    @Override
    public boolean equals(Object other) {
        return other instanceof LiveCells that && cells.equals(that.cells);
    }

    @Override
    public int hashCode() {
        return cells.hashCode();
    }

    @Override
    public String toString() {
        return "LiveCells" + cells;
    }
}

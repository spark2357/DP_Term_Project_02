package com.holub.life;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * 보드 스냅샷 (특정 시점의 보드 상태)
 * 살아 있는 셀의 좌표(x = 열, y = 행)와 그 셀의 색을 들고 있음
 */
// [수정] 2026-11-24: 셀 색 브랜치 병합. Undo/Redo 후에도 색이 유지되도록 Set<Point> 대신 Map<Point, CellColor>를 저장
public final class BoardSnapshot {
    private final Map<Point, CellColor> cells;

    BoardSnapshot(Map<Point, CellColor> cells) {
        this.cells = copyOf(cells);
    }

    // 복사본을 돌려줘서 받은 쪽에서 수정해도 저장된 스냅샷은 변경되지 않아서 여러 번 복원 가능
    Map<Point, CellColor> cells() {
        return copyOf(cells);
    }

    private static Map<Point, CellColor> copyOf(Map<Point, CellColor> cells) {
        Map<Point, CellColor> copy = new HashMap<>();
        for (Map.Entry<Point, CellColor> e : cells.entrySet()) {
            copy.put(new Point(e.getKey()), e.getValue());
        }
        return copy;
    }

    // 살아 있는 셀과 색이 같으면 같은 스냅샷. LifeGame이 변경이 있는 스냅샷인지 확인할 때 사용
    @Override
    public boolean equals(Object other) {
        return other instanceof BoardSnapshot snapshot && cells.equals(snapshot.cells);
    }

    // equals를 override해서 동등한 객체는 같은 hashCode를 가져야 하기 때문에 override함
    @Override
    public int hashCode() {
        return cells.hashCode();
    }
}

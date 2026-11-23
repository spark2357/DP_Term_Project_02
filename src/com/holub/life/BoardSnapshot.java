package com.holub.life;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * 보드 스냅샷 (특정 시점의 보드 상태)
 * 살아 있는 셀의 좌표(x = 열, y = 행)만 들고 있음
 */
public final class BoardSnapshot {
    private final Set<Point> cells;

    BoardSnapshot(Set<Point> cells) {
        Set<Point> copy = new HashSet<>();
        for (Point p : cells) {
            copy.add(new Point(p));
        }
        this.cells = copy;
    }

    // 복사본을 돌려줘서 받은 쪽에서 수정해도 저장된 스냅샷은 변경되지 않아서 여러 번 복원 가능
    Set<Point> cells() {
        Set<Point> copy = new HashSet<>();
        for (Point p : cells) {
            copy.add(new Point(p));
        }
        return copy;
    }

    // 살아 있는 셀이 같으면 같은 스냅샷. LifeGame이 변경이 있는 스냅샷인지 확인할 때 사용
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

package com.holub.life.model;

/**
 * 보드 스냅샷 (특정 시점의 보드 상태)
 * 살아 있는 셀의 좌표(x = 열, y = 행)와 그 셀의 색을 들고 있음
 */
// [수정] 2026-11-24: 셀 색 브랜치 병합. Undo/Redo 후에도 색이 유지되도록 Set<Point> 대신 색까지 저장
// [수정] 2026-11-24: Map<Point, CellColor> 대신 불변 LiveCells를 저장. 받거나 돌려줄 때 복사하지 않아도 됨
public final class BoardSnapshot {
    private final LiveCells cells;

    BoardSnapshot(LiveCells cells) {
        this.cells = cells;
    }

    // LiveCells는 불변이라 그대로 돌려줘도 저장된 스냅샷은 변경되지 않아서 여러 번 복원 가능
    LiveCells cells() {
        return cells;
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

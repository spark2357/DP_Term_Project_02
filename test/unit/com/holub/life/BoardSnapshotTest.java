package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * BoardSnapshot 단위 테스트 (Memento 패턴: Board가 Originator, BoardSnapshot이 Memento)
 * 보드 없이 LiveCells로 스냅샷을 직접 만들어 BoardSnapshot만 확인함
 *
 *   메서드
 *   - equals()   : 살아 있는 셀이 같으면 같은 스냅샷 (LifeGame이 바뀌지 않은 편집을 기록하지 않을 때 씀)
 *   - hashCode() : equals()가 같으면 같은 값
 *   - cells()    : 담은 LiveCells (Board.restore()가 씀)
 *
 * [테스트 시나리오]
 *   1. 비교: 살아 있는 셀이 같은 스냅샷은 같고, 셀 하나라도 다르면 다름
 *
 * [커버리지]
 *   - BoardSnapshotTest.java 테스트 대상: BoardSnapshot.equals, BoardSnapshot.hashCode
 *   - 테스트 제외 대상
 *     - BoardSnapshot.cells(): restore()가 쓰는 접근 메서드라 BoardTest의 restore() 테스트에서 함께 확인됨
 *     - Board.snapshot(), Board.restore(): 대상이 Board라서 BoardTest에서 확인
 *     - 복원 뒤 여러 세대 진행, 매 세대 스냅샷: 보드 전체의 세대 진행이 필요해서 통합 테스트 BoardSnapshotTickTest에서 확인
 *     - 같은 셀의 보드에서 만든 스냅샷이 같아 편집을 기록하지 않는 것: 통합 테스트 LifeGameUndoTest에서 확인
 *     - 스냅샷에 담긴 셀 색: 통합 테스트 CellColorBoardTest에서 확인
 *     - 스냅샷을 쌓고 꺼내는 것: BoardHistoryTest에서 확인
 */
@Tag("extension")
class BoardSnapshotTest {
	// 블록(8x8) 경계인 7|8열, 7|8행에 걸친 glider
	private static final Set<Point> GLIDER_ON_BLOCK_EDGE = Cells.fromRowColumn("6,7", "7,8", "8,6", "8,7", "8,8");

	//  1. 비교

	@Test
	@DisplayName("equals(): 살아 있는 셀이 같으면 따로 만든 스냅샷이라도 같고, hashCode도 같다")
	void snapshotsWithSameCellsAreEqual() {
		// Given
		BoardSnapshot first = new BoardSnapshot(LiveCells.of(GLIDER_ON_BLOCK_EDGE));

		// When
		BoardSnapshot sameCells = new BoardSnapshot(LiveCells.of(GLIDER_ON_BLOCK_EDGE));

		// Then
		assertEquals(first, sameCells);
		assertEquals(first.hashCode(), sameCells.hashCode());
	}

	@Test
	@DisplayName("equals(): 셀 하나만 달라도 다른 스냅샷이다")
	void snapshotsWithDifferentCellsAreNotEqual(){
		// Given
		BoardSnapshot before = new BoardSnapshot(LiveCells.of(GLIDER_ON_BLOCK_EDGE));

		// When: (30,30) 셀 하나를 더한 스냅샷
		Set<Point> oneMore = new HashSet<>(GLIDER_ON_BLOCK_EDGE);
		oneMore.add(new Point(30, 30));
		BoardSnapshot after = new BoardSnapshot(LiveCells.of(oneMore));

		// Then
		assertNotEquals(before, after);
	}
}

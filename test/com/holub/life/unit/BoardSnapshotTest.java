package com.holub.life.unit;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Board.snapshot, Board.restore 단위 테스트 (Memento 패턴: Board가 Originator, BoardSnapshot이 Memento)
 *
 *   메서드
 *   - snapshot()        : 지금 살아 있는 셀을 담은 BoardSnapshot을 생성. 보드는 바뀌지 않음
 *   - restore(snapshot) : 스냅샷을 생성한 시점의 셀로 되돌림 (Load와 같은 transfer(LOAD) 경로)
 *   - equals()          : 살아 있는 셀이 같으면 같은 스냅샷 (LifeGame이 바뀌지 않은 편집을 기록하지 않을 때 씀)
 *
 * [테스트 시나리오]
 *   1. 복원: 편집하고 진행한 뒤 복원하면 스냅샷을 생성한 시점의 보드
 *   2. 독립성: 스냅샷은 그 뒤의 보드 변경에 영향받지 않고, 같은 스냅샷으로 여러 번 복원할 수 있음
 *   3. 복원 뒤 진행: 블록 경계에 걸친 패턴을 복원한 뒤 진행해도 정답과 같음
 *      (복원할 때 옆 블록을 깨우지 않으면, 계산을 건너뛴 블록에서 셀이 빠짐)
 *   4. 매 세대 스냅샷: Go 중에는 세대마다 스냅샷을 생성하는데, 그래도 진행 결과가 정답과 같음
 *   5. 비교: 살아 있는 셀이 같은 보드의 스냅샷은 같고, 셀 하나라도 다르면 다름
 */
class BoardSnapshotTest {
	// 블록(8x8) 경계인 7|8열, 7|8행에 걸친 glider
	private static final Set<Point> GLIDER_ON_BLOCK_EDGE = Cells.fromRowColumn("6,7", "7,8", "8,6", "8,7", "8,8");

	//  1. 복원

	@Test
	@DisplayName("restore(): 셀을 토글하고 몇 세대 진행한 뒤 복원하면 스냅샷을 생성한 시점의 보드로 돌아간다")
	void restoreReturnsToTheSnapshot() {
		// Given
		Board board = boardWith(GLIDER_ON_BLOCK_EDGE);
		BoardSnapshot snapshot = board.snapshot();

		// When
		board.toggle(30, 30);
		board.step();
		board.step();
		board.restore(snapshot);

		// Then
		assertEquals(GLIDER_ON_BLOCK_EDGE, board.liveCells());
	}

	//  2. 독립성

	@Test
	@DisplayName("restore(): 빈 보드의 스냅샷으로 복원하면 셀이 모두 지워지고, 같은 스냅샷으로 다시 복원할 수 있다")
	void sameSnapshotCanBeRestoredTwice() {
		// Given
		Board board = new Board();
		BoardSnapshot empty = board.snapshot();
		board.toggle(1, 1);

		// When
		board.restore(empty);
		board.toggle(2, 2);
		board.restore(empty);

		// Then
		assertEquals(Set.of(), board.liveCells());
	}

	//  3. 복원 뒤 진행

	@Test
	@DisplayName("restore() 뒤 step(): 블록 경계에 걸친 glider를 복원하고 10세대 진행하면 정답 계산과 같다")
	void restoredPatternAdvancesCorrectly() {
		// Given: glider를 다른 곳으로 진행시켜 둔 보드
		Board board = boardWith(GLIDER_ON_BLOCK_EDGE);
		BoardSnapshot snapshot = board.snapshot();
		for (int i = 0; i < 5; i++) {
			board.step();
		}

		// When
		board.restore(snapshot);
		Set<Point> expected = GLIDER_ON_BLOCK_EDGE;
		for (int generation = 1; generation <= 10; generation++) {
			board.step();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);

			// Then
			assertEquals(expected, board.liveCells(), generation + "세대");
		}
	}

	//  4. 매 세대 스냅샷

	@Test
	@DisplayName("snapshot(): 세대마다 스냅샷을 생성하면서 30세대 진행해도 정답 계산과 같다")
	void snapshotEveryGenerationKeepsResults() {
		// Given: 행 경계(7|8, 15|16, ...)마다 걸친 세로 blinker 64개와 glider
		Set<Point> start = Cells.repeatEvery8(Cells.fromRowColumn("7,0", "8,0", "9,0"), 0, 3);
		start.addAll(GLIDER_ON_BLOCK_EDGE);
		Board board = boardWith(start);
		Set<Point> expected = start;

		for (int generation = 1; generation <= 30; generation++) {
			// When
			board.snapshot();
			board.step();
			expected = ExpectedGenerationCalculator.nextGeneration(expected);

			// Then
			assertEquals(expected, board.liveCells(), generation + "세대");
		}
	}

	//  5. 비교

	@Test
	@DisplayName("equals(): 살아 있는 셀이 같으면 다른 보드의 스냅샷이라도 같고(hashCode도 같음), 셀 하나만 달라도 다르다")
	void snapshotsWithSameCellsAreEqual() {
		// Given
		Board board = boardWith(GLIDER_ON_BLOCK_EDGE);
		BoardSnapshot before = board.snapshot();
		BoardSnapshot sameCells = boardWith(GLIDER_ON_BLOCK_EDGE).snapshot();

		// When
		board.toggle(30, 30);
		BoardSnapshot after = board.snapshot();

		// Then
		assertEquals(before, sameCells);
		assertEquals(before.hashCode(), sameCells.hashCode());
		assertNotEquals(before, after);
	}

	// ---- 헬퍼 메서드

	private static Board boardWith(Set<Point> cells) {
		Board board = new Board();
		for (Point p : cells) {
			board.toggle(p.y, p.x);
		}
		return board;
	}
}

package com.holub.life.game;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.model.Board;
import com.holub.life.model.BoardSnapshot;

/**
 * Board.snapshot, Board.restore 통합 테스트: 스냅샷과 복원을 거쳐도 여러 세대 진행 결과가 정답 계산과 같은지 (Memento 패턴, E. 되돌리기)
 * 진짜 Board를 여러 세대 진행하고, 매 세대를 정답 계산(ExpectedGenerationCalculator)과 비교함
 * 통합 테스트에 둔 이유: 블록 경계를 넘는 패턴의 여러 세대 진행은 Board 안의 블록 건너뛰기와 Resident의 세대 규칙까지 모두 맞아야 결과가 나옴.
 * 세대 규칙 전체를 보는 TickContractTest처럼 보드 전체 테스트로 둠
 *
 * [테스트 시나리오]
 *   1. 복원 뒤 진행: 블록 경계에 걸친 패턴을 복원한 뒤 진행해도 정답과 같음
 *      (복원할 때 옆 블록을 깨우지 않으면, 계산을 건너뛴 블록에서 셀이 빠짐)
 *   2. 매 세대 스냅샷: Go 중에는 세대마다 스냅샷을 생성하는데, 그래도 진행 결과가 정답과 같음
 *
 * [커버리지]
 *   - BoardSnapshotTickTest.java 테스트 대상: Board.snapshot, Board.restore, Board.step
 *   - 테스트 제외 대상
 *     - 복원하면 스냅샷 시점의 셀로 돌아가는 것, 같은 스냅샷으로 여러 번 복원: 단위 테스트 BoardTest에서 확인
 *     - BoardSnapshot.equals, hashCode: 단위 테스트 BoardSnapshotTest에서 확인
 *     - 스냅샷에 담긴 셀 색: CellColorBoardTest에서 확인
 */
@Tag("extension")
class BoardSnapshotTickTest {
	// 블록(8x8) 경계인 7|8열, 7|8행에 걸친 glider
	private static final Set<Point> GLIDER_ON_BLOCK_EDGE = Cells.fromRowColumn("6,7", "7,8", "8,6", "8,7", "8,8");

	//  1. 복원 뒤 진행

	@Test
	@DisplayName("restore(), step(): 블록 경계에 걸친 glider를 복원하고 10세대 진행하면 정답 계산과 같다")
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

	//  2. 매 세대 스냅샷

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

	// ---- 헬퍼 메서드

	private static Board boardWith(Set<Point> cells) {
		Board board = new Board();
		for (Point p : cells) {
			board.toggle(p.y, p.x);
		}
		return board;
	}
}

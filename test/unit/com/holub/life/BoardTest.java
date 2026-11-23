package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Board 단위 테스트: Universe에서 떼어 낸 64x64 보드 (셀 트리를 감싸는 모델)
 * 파일 형식은 가짜 PatternFormat으로 바꿔, 파일 없이 Board가 형식에 무엇을 넘기고 받는지만 확인함
 *
 *   메서드
 *   - step()               : 한 세대 진행. 바뀐 셀이 있으면 true
 *   - toggle(행, 열)        : 셀 하나를 뒤집음
 *   - clear()              : 모든 셀을 지움
 *   - load(in, format)     : 형식이 읽은 셀로 보드를 바꿈. 읽다가 실패하면 보드는 그대로
 *   - store(out, format)   : 산 셀을 형식에 넘겨 씀
 *   - liveCells()          : 산 셀 좌표의 복사본
 *   - widthInCells()       : 64
 *
 * [테스트 시나리오]
 *   1. 진행: blinker는 한 세대 뒤 세로가 되고 step()은 true. 빈 보드는 false
 *   2. 토글, Clear
 *   3. Load: 형식이 읽은 셀로 보드를 바꿈. 형식이 실패하면 보드는 그대로
 *   4. Store: 산 셀을 그대로 형식에 넘기고, 보드는 바뀌지 않음
 *   5. 복사: liveCells()로 받은 집합을 바꿔도 보드는 그대로
 *
 * [커버리지]
 *   - BoardTest.java 테스트 대상: step, toggle, clear, load, store, liveCells, widthInCells
 *   - 테스트 제외 대상
 *     - stamp(): BoardStampTest에서 확인
 *     - 세대 규칙 전체, 블록 경계: TickContractTest(BoardTickTest)에서 확인
 *     - redraw(): 화면이라서 제외 (셀 그리기는 CellRedrawTest)
 */
class BoardTest {
	private final Board board = new Board();

	@Test
	@Tag("original")
	@DisplayName("widthInCells(): 보드는 64칸이다")
	void boardIs64Wide() {
		assertEquals(LifeTestDriver.SIZE, board.widthInCells());
	}

	@Test
	@Tag("original")
	@DisplayName("step(): blinker는 한 세대 뒤 세로가 되고, 바뀐 셀이 있으니 true를 돌려준다")
	void stepAdvancesOneGeneration() {
		// Given
		place(Cells.fromRowColumn("3,2", "3,3", "3,4"));

		// When
		boolean changed = board.step();

		// Then
		assertTrue(changed);
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), board.liveCells());
	}

	@Test
	@Tag("original")
	@DisplayName("step(): 빈 보드는 바뀌는 셀이 없어 false를 돌려준다")
	void stepOnEmptyBoardReturnsFalse() {
		assertFalse(board.step());
	}

	@Test
	@Tag("original")
	@DisplayName("toggle(): 한 번 누르면 살고, 두 번 누르면 다시 죽는다")
	void toggleFlipsOneCell() {
		board.toggle(5, 6);
		assertEquals(Cells.fromRowColumn("5,6"), board.liveCells(), "한 번");

		board.toggle(5, 6);
		assertEquals(Set.of(), board.liveCells(), "두 번");
	}

	@Test
	@Tag("original")
	@DisplayName("clear(): 모든 셀이 지워진다")
	void clearRemovesAllCells() {
		// Given
		place(Cells.fromRowColumn("0,0", "63,63", "30,30"));

		// When
		board.clear();

		// Then
		assertEquals(Set.of(), board.liveCells());
	}

	@Test
	@Tag("original")
	@DisplayName("load(): 형식이 읽은 셀로 보드를 바꾼다")
	void loadReplacesBoardWithReadCells() throws IOException {
		// Given: 보드에 다른 셀이 있음
		place(Cells.fromRowColumn("1,1"));
		Set<Point> pattern = Cells.fromRowColumn("10,10", "10,11", "11,10");

		// When
		board.load(new ByteArrayInputStream(new byte[0]), new FakeFormat(pattern));

		// Then
		assertEquals(pattern, board.liveCells());
	}

	@Test
	@Tag("improvement")
	@Tag("defect-fix")
	@DisplayName("load(): 형식이 읽다가 실패하면 IOException이 나고 보드는 그대로 남는다")
	void failedLoadKeepsBoard() {
		// Given
		Set<Point> before = Cells.fromRowColumn("1,1", "1,2");
		place(before);

		// When, Then
		assertThrows(IOException.class,
				() -> board.load(new ByteArrayInputStream(new byte[0]), new FakeFormat(null)));
		assertEquals(before, board.liveCells());
	}

	@Test
	@Tag("original")
	@DisplayName("store(): 산 셀을 그대로 형식에 넘기고, 보드는 바뀌지 않는다")
	void storeHandsLiveCellsToFormat() throws IOException {
		// Given
		Set<Point> cells = Cells.fromRowColumn("0,0", "7,8", "63,63");
		place(cells);
		FakeFormat format = new FakeFormat(null);

		// When
		board.store(new ByteArrayOutputStream(), format);

		// Then
		assertEquals(cells, format.written);
		assertEquals(cells, board.liveCells());
	}

	@Test
	@Tag("original")
	@DisplayName("liveCells(): 받은 집합을 바꿔도 보드는 바뀌지 않는다")
	void liveCellsIsACopy() {
		// Given
		place(Cells.fromRowColumn("2,2"));

		// When
		board.liveCells().add(new Point(9, 9));

		// Then
		assertEquals(Cells.fromRowColumn("2,2"), board.liveCells());
	}

	// ---- 헬퍼 메서드

	private void place(Set<Point> cells) {
		for (Point cell : cells) {
			board.toggle(cell.y, cell.x);
		}
	}
}

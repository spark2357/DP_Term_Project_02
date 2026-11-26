package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Board 단위 테스트: Universe에서 떼어 낸 64x64 보드 (셀 트리를 감싸는 모델)
 * 파일 형식은 가짜 PatternFormat으로 바꿔, 파일 없이 Board가 형식에 무엇을 넘기고 받는지만 확인함
 * 셀 트리(Neighborhood, Resident)와 기본 출생 색 규칙(ClashRule)은 Board가 직접 만들어 갖는 부품이라 진짜를 씀
 *
 *   메서드
 *   - step()               : 한 세대 진행. 바뀐 셀이 있으면 true
 *   - toggle(행, 열)        : 셀 하나를 뒤집음
 *   - brush(행, 열, 색)     : 셀 하나를 그 색으로 칠함. 색이 null이면 NullPointerException
 *   - clear()              : 모든 셀을 지움
 *   - load(in, format)     : 형식이 읽은 셀로 보드를 바꿈. 읽다가 실패하면 보드는 그대로
 *   - store(out, format)   : 산 셀을 형식에 넘겨 씀
 *   - stamp(pattern, 행, 열, 색) : 패턴의 왼쪽 위를 (행, 열)에 맞추어, 패턴 사각형 안은 패턴대로(죽은 칸 포함) 덮어쓰고
 *                                 밖의 셀은 그대로 둠. 보드 밖으로 나가는 부분은 잘라냄
 *   - liveCells()          : 산 셀 좌표의 복사본
 *   - widthInCells()       : 64
 *   - birthColorRule()     : 지금 출생 색 규칙 (처음은 ClashRule)
 *   - setBirthColorRule(규칙) : 출생 색 규칙을 바꿈. null이면 NullPointerException. 산 셀의 위치와 색은 그대로
 *   - snapshot()           : 지금 살아 있는 셀을 담은 BoardSnapshot을 생성. 보드는 바뀌지 않음 (Memento 패턴의 Originator)
 *   - restore(snapshot)    : 스냅샷을 생성한 시점의 셀로 되돌림 (Load와 같은 transfer(LOAD) 경로)
 *
 * [테스트 시나리오]
 *   1. 진행: blinker는 한 세대 뒤 세로가 되고 step()은 true. 빈 보드는 false
 *   2. 토글, Clear
 *   3. Load: 형식이 읽은 셀로 보드를 바꿈. 형식이 실패하면 보드는 그대로
 *   4. Store: 산 셀을 그대로 형식에 넘기고, 보드는 바뀌지 않음
 *   5. 복사: liveCells()로 받은 집합을 바꿔도 보드는 그대로
 *   6. 출생 색 규칙: 처음은 Clash, null은 거부, 규칙을 바꿔도 산 셀의 위치와 색은 그대로
 *   7. 칠하기: 색 없이(null) 칠하면 거부하고 보드는 그대로
 *   8. 찍기 덮어쓰기: 사각형 안의 기존 셀은 패턴대로 바뀌고, 사각형 밖의 셀은 남음. 찍은 패턴은 다음 세대로 진행됨
 *   9. 찍기 잘라내기: 보드 가장자리를 넘는 부분만 버리고 나머지는 찍힘
 *   10. 복원: 편집하고 진행한 뒤 복원하면 스냅샷을 생성한 시점의 보드
 *   11. 독립성: 스냅샷은 그 뒤의 보드 변경에 영향받지 않고, 같은 스냅샷으로 여러 번 복원할 수 있음
 *
 * [커버리지]
 *   - BoardTest.java 테스트 대상: step, toggle, brush, clear, load, store, stamp, liveCells, widthInCells, birthColorRule, setBirthColorRule,
 *     snapshot, restore
 *   - 테스트 제외 대상
 *     - 복원 뒤 여러 세대 진행, 매 세대 스냅샷: 블록 경계를 넘는 여러 세대 진행이 필요해서 통합 테스트 BoardSnapshotTickTest에서 확인
 *     - BoardSnapshot.equals, hashCode: BoardSnapshotTest에서 확인
 *     - accept(): 셀 트리에 방문자를 넘기기만 함. 방문 순서는 NeighborhoodTest, 센 값은 통합 테스트 RunRecorderTest에서 확인
 *     - 세대 규칙 전체, 블록 경계: TickContractTest(BoardTickTest)에서 확인
 *     - 블록 경계에 걸친 찍은 패턴의 진행: stamp는 Load와 같은 transfer(LOAD) 경로를 써서 Store/Load 테스트에서 확인됨
 *     - 칠한 색이 세대 진행에서 물려지는 것: 통합 테스트 CellColorBoardTest에서 확인
 *     - redraw(): 화면이라서 제외 (셀 그리기는 CellRedrawTest)
 */
class BoardTest {
	private final Board board = new Board();

	// x = 3, y = 3 사각형 안의 glider
	private static final CellPattern GLIDER = new CellPattern(3, 3,
			Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));

	// 블록(8x8) 경계인 7|8열, 7|8행에 걸친 glider
	private static final Set<Point> GLIDER_ON_BLOCK_EDGE = Cells.fromRowColumn("6,7", "7,8", "8,6", "8,7", "8,8");

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
	@DisplayName("load(): 형식이 읽다가 실패하면 IOException이 나고 보드는 그대로 남는다 (결함)")
	void failedLoadKeepsBoard() {
		// Given: 원본(Universe.doLoad())은 파일을 읽기 전에 보드를 지워서, 읽다가 실패하면 빈 보드가 남음
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

	//  셀 색 기능: 출생 색 규칙(Strategy)과 칠하기

	@Test
	@Tag("extension")
	@DisplayName("birthColorRule(): 처음 출생 색 규칙은 Clash다")
	void defaultRuleIsClash(){
		assertInstanceOf(ClashRule.class, board.birthColorRule());
	}

	@Test
	@Tag("extension")
	@DisplayName("setBirthColorRule(): null은 NullPointerException이고 규칙은 그대로다")
	void nullRuleRejected(){
		// Given
		BirthColorRule before = board.birthColorRule();

		// When, Then
		assertThrows(NullPointerException.class, () -> board.setBirthColorRule(null));
		assertSame(before, board.birthColorRule());
	}

	@Test
	@Tag("extension")
	@DisplayName("setBirthColorRule(): 규칙을 바꿔도 산 셀의 위치와 색은 그대로다")
	void switchingRuleKeepsCellsAndColors(){
		// Given
		board.brush(3, 4, new CellColor(0, 0, 255));
		board.brush(10, 20, new CellColor(0, 128, 0));
		board.toggle(63, 63);
		Map<Point, CellColor> expected = new HashMap<>();
		expected.put(new Point(4, 3), new CellColor(0, 0, 255));
		expected.put(new Point(20, 10), new CellColor(0, 128, 0));
		expected.put(new Point(63, 63), CellColor.DEFAULT);

		// When
		BirthColorRule rule = new FixedRule();
		board.setBirthColorRule(rule);

		// Then
		assertSame(rule, board.birthColorRule());
		assertEquals(new LiveCells(expected), board.snapshot().cells());
	}

	@Test
	@Tag("extension")
	@DisplayName("brush(): 색 없이(null) 칠하면 NullPointerException이고 보드는 그대로다")
	void brushWithNullRejected(){
		// Given
		place(Cells.fromRowColumn("1,1"));

		// When, Then
		assertThrows(NullPointerException.class, () -> board.brush(5, 5, null));
		assertEquals(Cells.fromRowColumn("1,1"), board.liveCells());
	}

	//  찍기

	@Test
	@Tag("extension")
	@DisplayName("stamp(): 사각형 안의 기존 셀은 패턴대로 바뀌고(죽은 칸도 덮어씀), 밖의 셀은 그대로 남는다")
	void stampOverwritesOnlyInsideTheRectangle() {
		// Given: 사각형 안의 (10,10)은 패턴에서 죽은 칸, (9,9)는 사각형 밖
		board.toggle(10, 10);
		board.toggle(9, 9);

		// When
		board.stamp(GLIDER, 10, 10, CellColor.DEFAULT);

		// Then
		assertEquals(Cells.fromRowColumn("9,9", "10,11", "11,12", "12,10", "12,11", "12,12"), board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("stamp(), step(): 찍은 glider는 다음 세대로 진행된다")
	void stampedPatternAdvances() {
		// Given
		board.stamp(GLIDER, 10, 10, CellColor.DEFAULT);

		// When
		board.step();

		// Then
		assertEquals(Cells.fromRowColumn("11,10", "11,12", "12,11", "12,12", "13,11"), board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("stamp(): 오른쪽 아래 구석에 찍으면 보드 밖으로 나가는 부분만 버린다")
	void stampClipsAtTheEdge() {
		// Given
		int last = board.widthInCells() - 1;

		// When: 왼쪽 위가 (63, 62)이면 첫 행(0행)의 (63, 63)만 보드 안
		board.stamp(GLIDER, last, last - 1, CellColor.DEFAULT);

		// Then
		assertEquals(Set.of(new Point(last, last)), board.liveCells());
	}

	//  스냅샷과 복원

	@Test
	@Tag("extension")
	@DisplayName("restore(): 셀을 토글하고 몇 세대 진행한 뒤 복원하면 스냅샷을 생성한 시점의 보드로 돌아간다")
	void restoreReturnsToTheSnapshot() {
		// Given
		place(GLIDER_ON_BLOCK_EDGE);
		BoardSnapshot snapshot = board.snapshot();

		// When
		board.toggle(30, 30);
		board.step();
		board.step();
		board.restore(snapshot);

		// Then
		assertEquals(GLIDER_ON_BLOCK_EDGE, board.liveCells());
	}

	@Test
	@Tag("extension")
	@DisplayName("restore(): 빈 보드의 스냅샷으로 복원하면 셀이 모두 지워지고, 같은 스냅샷으로 다시 복원할 수 있다")
	void sameSnapshotCanBeRestoredTwice() {
		// Given
		BoardSnapshot empty = board.snapshot();
		board.toggle(1, 1);

		// When
		board.restore(empty);
		board.toggle(2, 2);
		board.restore(empty);

		// Then
		assertEquals(Set.of(), board.liveCells());
	}

	// ---- 헬퍼 메서드

	private void place(Set<Point> cells) {
		for (Point cell : cells) {
			board.toggle(cell.y, cell.x);
		}
	}

	// [수정] 2026-11-27: 진짜 BlendRule 대신 쓰는 가짜 출생 색 규칙. 규칙을 바꿔 끼웠는지만 보므로 무엇을 돌려주든 상관없음
	private static class FixedRule implements BirthColorRule{
		@Override
		public CellState newborn(List<CellColor> parents){
			return CellState.DEAD;
		}
	}
}

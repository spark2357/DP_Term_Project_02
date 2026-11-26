package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Neighborhood 단위 테스트: NxN개의 Cell 묶음 (Composite 패턴의 Composite)
 * Universe와 같은 구조인 Neighborhood(8, Neighborhood(8, Resident)), 즉 64x64 셀 보드로 테스트함
 * 하위 셀(Resident)은 Neighborhood가 원형(prototype)을 복제해 직접 만들어 갖는 부품이라 진짜를 씀. 출생 색 규칙은 가짜(RULE)
 * 여기에는 픽셀 → 하위 셀 전달, transfer 재귀, 방문 순서처럼 Neighborhood 자신의 일을 보는 테스트만 둠.
 * Resident는 켜고 끄고 기록하는 가장 단순한 하위 셀로만 쓰고, 결과가 Resident의 세대 규칙이나 칠하기 규칙에 달린 테스트는 통합 테스트로 둠
 *
 *   필드
 *   - grid, gridSize          : 하위 셀 배열과 한 변의 칸 수
 *   - amActive                : 지난 계산에서 바뀐 셀이 있었는지 (없으면 다음 계산을 건너뜀)
 *   - activeEdges             : 바뀐 셀이 있는 가장자리 방향 (이웃 블록을 깨울 때 씀)
 *   - oneLastRefreshRequired  : 안정된 직후 한 번 더 그려야 하는지
 *   메서드
 *   - userClicked(점, 영역)    : 픽셀 좌표로 하위 셀을 찾아 클릭을 전달하고, 가장자리 표시를 켬
 *   - userBrushed(점, 영역, 색) : 클릭(userClicked)과 같은 길로 하위 셀을 찾아 칠하고, 블록을 활성으로 표시함
 *   - clear()                  : 모든 하위 셀과 activeEdges, amActive를 지움
 *   - isDisruptiveTo()         : activeEdges 반환
 *   - isActive()               : amActive 반환 (통계의 활성 블록 수에 씀)
 *   - figureNextState(이웃 8개) : 활성 블록이거나 이웃이 이쪽 가장자리를 깨우면 하위 셀의 다음 상태를 계산
 *   - transition()             : 하위 셀에 다음 상태를 적용하고 activeEdges를 갱신
 *   - edge(행, 열)             : 가장자리 셀 반환 (이웃 블록이 경계 너머 셀을 볼 때 씀)
 *   - transfer(memento, 좌표, LOAD/STORE) : 하위 셀을 재귀로 돌며 저장하거나 읽음
 *   - createMemento()          : 새 NeighborhoodState를 만들고 transfer(STORE)까지 수행해 반환
 *   - accept(방문자)            : 블록에 들어감 → 하위 셀을 행 순서로 → 블록에서 나옴 순서로 방문자를 넘김 (Visitor 패턴)
 *   - redraw(), create(), widthInCells(), isAlive() : 그리기, 복제(Prototype), Cell 가로 크기(gridSize x 하위 셀 크기), 항상 true
 *
 * [테스트 시나리오]
 *   1. 클릭: 픽셀 좌표가 올바른 셀로 바뀜 (꼭짓점, 셀 경계 픽셀, 블록 경계), 두 번 클릭하면 되돌아감
 *   2. Clear: 셀, 가장자리 표시, 다음 계산까지 모두 비워짐
 *   3. createMemento(): 만들면서 이미 산 셀을 기록함 (원본 동작: Universe.doStore()의 중복 저장 원인)
 *   4. 칠하기 위치: 픽셀 좌표 아래의 셀만 그 색으로 칠해짐 (꼭짓점, 셀 경계 픽셀, 블록 경계)
 *   5. 활성 여부: transfer(LOAD)로 산 셀을 놓으면 활성, 놓을 셀이 없으면 활성이 아님
 *   6. 방문: 블록에 들어감 → 하위 셀을 행 순서로 → 나옴. 블록이 겹쳐 있으면 바깥 블록 → 안쪽 블록 → 셀 순서로 재귀 방문
 *
 * [커버리지]
 *   - NeighborhoodTest.java 테스트 대상: userClicked, userBrushed, clear, isDisruptiveTo, isActive, createMemento, accept,
 *     figureNextState(clear 직후 계산할 것이 없는 경우), transfer(LOAD 뒤 활성 여부)
 *   - 테스트 제외 대상
 *     - 칠하기 활성 표시, 같은 색으로 두 번 칠하기, 한 세대 계산 뒤 활성 여부: 결과가 진짜 Resident의 세대 규칙과 칠하기 규칙에 달려 있어서
 *       통합 테스트 NeighborhoodResidentTest에서 확인
 *     - figureNextState, transition, edge의 세대 계산: 패턴과 경계별로 보드 전체가 필요해서 유닛 테스트에서 제외
 *     - transfer(LOAD)로 놓은 셀의 배치: Store/Load 순서 전체가 필요해서 유닛 테스트에서 제외
 *     - redraw(): CellRedrawTest에서 확인
 *     - 칠한 셀의 색이 세대 진행에서 물려지는 것: 통합 테스트 CellColorBoardTest에서 확인
 *     - create(), widthInCells(), isAlive(): 한 줄짜리 고정 반환이라 생략
 */
class NeighborhoodTest {
	private static final int CELLS = 64;
	private static final Rectangle SCREEN = new Rectangle(0, 0, 512, 512);
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	// [수정] 2026-11-27: 진짜 ClashRule 대신 가짜 출생 색 규칙. 부모 색과 상관없이 기본색(빨강)으로 태어남
	private static final Supplier<BirthColorRule> RULE = () -> parents -> CellState.alive(CellColor.DEFAULT);

	@Test
	@Tag("original")
	@DisplayName("userClicked(): 클릭한 픽셀 좌표가 올바른 셀로 바뀐다")
	void clickSelectsTheRightCell() {
		// 픽셀 (x, y) → 셀 (행, 열). 셀 하나는 8픽셀
		assertClickTurnsOn(0, 0, 0, 0);			// 왼쪽 위 꼭짓점
		assertClickTurnsOn(511, 511, 63, 63);	// 오른쪽 아래 꼭짓점
		assertClickTurnsOn(7, 0, 0, 0);			// 0열의 마지막 픽셀
		assertClickTurnsOn(8, 0, 0, 1);			// 1열의 첫 픽셀
		assertClickTurnsOn(0, 64, 8, 0);		// 블록 경계 아래쪽 첫 셀
	}

	@Test
	@Tag("original")
	@DisplayName("userClicked(): 같은 셀을 두 번 클릭하면 원래대로 돌아간다")
	void clickingTwiceTogglesBack() {
		// Given
		Neighborhood board = newBoard();

		// When
		clickPixel(board, 100, 200);
		clickPixel(board, 100, 200);

		// Then
		assertEquals(0, liveCount(board));
	}

	@Test
	@Tag("original")
	@DisplayName("clear(): 셀, 가장자리 표시, 다음 계산까지 모두 지워서 초기화한다")
	void clearResetsEverything() {
		// Given: 보드 가장자리와 안쪽에 셀을 살림
		Neighborhood board = newBoard();
		clickPixel(board, 0, 0);		// 셀 (0,0)
		clickPixel(board, 300, 100);	// 셀 (12,37)
		assertFalse(board.isDisruptiveTo().equals(Direction.NONE), "clear() 전에는 가장자리 표시가 있어야 함");

		// When
		board.clear();

		// Then: 가장자리 표시가 지워짐
		// Direction.equals(Direction)은 equals(Object)를 재정의하지 않으므로 assertEquals 대신 직접 호출
		assertTrue(board.isDisruptiveTo().equals(Direction.NONE));

		// Then: 다음 세대를 계산해도 바뀔 것이 없음
		boolean willChange = board.figureNextState(
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		assertFalse(willChange);

		// Then: 산 셀이 없음
		assertEquals(0, liveCount(board));
	}

	@Test
	@Tag("original")
	@DisplayName("createMemento(): 만들면서 이미 산 셀을 기록한다. Universe.doStore()에서 중복 저장 발생 (원본 동작)")
	void createMementoAlreadyStoresLiveCells() {
		// Given: 셀 (2,3)을 살림
		Neighborhood board = newBoard();
		clickPixel(board, 3 * 8, 2 * 8);

		// When: transfer(STORE)는 호출하지 않음
		Cell.Memento memento = (Cell.Memento) board.createMemento();

		// Then: 이미 기록되어 있음. Universe.doStore()는 여기에 transfer(STORE)를 한 번 더 해서 셀이 두 번 저장됨
		// memento의 좌표는 Point(x=열, y=행)
		assertTrue(memento.isAlive(new Point(3, 2)));
	}

	//  칠하기

	@ParameterizedTest(name = "픽셀 ({0}, {1}) → {2}행 {3}열")
	@CsvSource({
		"0, 0, 0, 0",
		"511, 511, 63, 63",
		"7, 0, 0, 0",
		"8, 0, 0, 1",
		"0, 64, 8, 0",
		"300, 100, 12, 37"
	})
	@Tag("extension")
	@DisplayName("userBrushed(): 픽셀 좌표 아래의 셀 하나만 그 색으로 칠해진다")
	void brushReachesCellUnderPixel(int x, int y, int row, int column) throws Exception{
		// Given
		Neighborhood board = newBoard();

		// When
		board.userBrushed(new Point(x, y), SCREEN, BLUE);

		// Then
		Cell.Memento stored = (Cell.Memento) board.createMemento();
		assertEquals(List.of(new Point(column, row)), List.copyOf(storedPoints(stored)));
		assertEquals(BLUE, stored.colorAt(new Point(column, row)));
	}

	//  활성 여부

	@Test
	@Tag("extension")
	@DisplayName("isActive(): transfer(LOAD)로 산 셀을 놓은 블록은 활성이다")
	void loadingLiveCellMakesBlockActive(){
		// Given: (3,3)이 살아 있다고 기록된 memento와 새 블록
		Cell.Memento memento = Neighborhood.createEmptyMemento();
		memento.markAsAlive(new Point(3, 3));
		Neighborhood block = new Neighborhood(8, new Resident(RULE));

		// When
		block.transfer(memento, new Point(0, 0), Cell.LOAD);

		// Then
		assertTrue(block.isActive());
	}

	@Test
	@Tag("extension")
	@DisplayName("isActive(): transfer(LOAD)로 놓을 산 셀이 없으면 활성이 아니다")
	void loadingNoCellLeavesBlockInactive(){
		// Given: 아무 셀도 기록되지 않은 memento와 새 블록
		Cell.Memento memento = Neighborhood.createEmptyMemento();
		Neighborhood block = new Neighborhood(8, new Resident(RULE));

		// When
		block.transfer(memento, new Point(0, 0), Cell.LOAD);

		// Then
		assertFalse(block.isActive());
	}

	//  방문

	@Test
	@Tag("extension")
	@DisplayName("accept(): 블록에 들어감을 알리고, 하위 셀을 행 순서로 방문한 뒤, 나옴을 알린다")
	void acceptVisitsCellsInRowOrderBetweenEnterAndLeave(){
		// Given: 2x2 블록에서 0행 1열 셀만 살아 있음
		Neighborhood block = new Neighborhood(2, new Resident(RULE));
		block.userClicked(new Point(1, 0), new Rectangle(0, 0, 2, 2));
		RecordingVisitor visitor = new RecordingVisitor();

		// When
		block.accept(visitor);

		// Then
		assertEquals(List.of("들어감", "죽은 셀", "산 셀", "죽은 셀", "죽은 셀", "나옴"), visitor.events);
		assertEquals(List.of(block, block), visitor.blocks);
	}

	@Test
	@Tag("extension")
	@DisplayName("accept(): 블록이 겹쳐 있으면 바깥 블록 → 안쪽 블록 → 셀 순서로 재귀 방문한다")
	void acceptVisitsNestedBlocksRecursively(){
		// Given: 2x2 블록 4개를 담은 바깥 블록
		Neighborhood outer = new Neighborhood(2, new Neighborhood(2, new Resident(RULE)));
		RecordingVisitor visitor = new RecordingVisitor();

		// When
		outer.accept(visitor);

		// Then: 바깥 블록에 들어가고, 안쪽 블록마다 들어감 → 셀 4개 → 나옴, 마지막에 바깥 블록에서 나옴
		List<String> expected = new ArrayList<>();
		expected.add("들어감");
		for (int block = 0; block < 4; block++) {
			expected.addAll(List.of("들어감", "죽은 셀", "죽은 셀", "죽은 셀", "죽은 셀", "나옴"));
		}
		expected.add("나옴");
		assertEquals(expected, visitor.events);
		assertSame(outer, visitor.blocks.get(0), "처음 들어간 블록");
		assertSame(outer, visitor.blocks.get(visitor.blocks.size() - 1), "마지막에 나온 블록");
	}

	// Universe와 같은 구조: 8x8 블록 안에 8x8 셀
	private static Neighborhood newBoard() {
		return new Neighborhood(8, new Neighborhood(8, new Resident(RULE)));
	}

	private static void clickPixel(Neighborhood board, int x, int y) {
		board.userClicked(new Point(x, y), SCREEN);
	}

	private static boolean isAlive(Neighborhood board, int row, int column) {
		Cell.Memento memento = (Cell.Memento) board.createMemento();
		return memento.isAlive(new Point(column, row));
	}

	private static int liveCount(Neighborhood board) {
		Cell.Memento memento = (Cell.Memento) board.createMemento();
		int count = 0;
		for (int row = 0; row < CELLS; row++) {
			for (int column = 0; column < CELLS; column++) {
				if (memento.isAlive(new Point(column, row))) {
					count++;
				}
			}
		}
		return count;
	}

	// 새 보드에서 픽셀 (x, y)를 클릭하면 셀 (row, column) 하나만 살아나는지 확인
	private static void assertClickTurnsOn(int x, int y, int row, int column) {
		Neighborhood board = newBoard();
		clickPixel(board, x, y);

		String where = "픽셀 (" + x + "," + y + ")";
		assertTrue(isAlive(board, row, column), where + " → 셀 (" + row + "," + column + ")");
		assertEquals(1, liveCount(board), where + "을 클릭하면 셀 하나만 살아야 함");
	}

	// memento에는 기록된 좌표 목록을 묻는 메서드가 없으므로, 저장한 내용의 첫 객체(산 셀 좌표)를 다시 읽음
	private static Collection<?> storedPoints(Cell.Memento memento) throws IOException, ClassNotFoundException{
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		memento.flush(file);

		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(file.toByteArray()));
		return (Collection<?>) in.readObject();
	}

	// 방문받은 순서를 글자로 기록하고, 들어가고 나온 블록을 차례로 기록하는 가짜 방문자
	private static class RecordingVisitor implements CellVisitor{
		private final List<String> events = new ArrayList<>();
		private final List<Neighborhood> blocks = new ArrayList<>();

		@Override
		public void enterNeighborhood(Neighborhood block){
			events.add("들어감");
			blocks.add(block);
		}

		@Override
		public void visitResident(Resident cell){
			if (cell.isAlive()) {
				events.add("산 셀");
			} else {
				events.add("죽은 셀");
			}
		}

		@Override
		public void leaveNeighborhood(Neighborhood block){
			events.add("나옴");
			blocks.add(block);
		}
	}
}

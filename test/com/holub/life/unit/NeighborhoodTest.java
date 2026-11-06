package com.holub.life.unit;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Neighborhood 단위 테스트: NxN개의 Cell 묶음 (Composite 패턴의 Composite)
 * Universe와 같은 구조인 Neighborhood(8, Neighborhood(8, Resident)), 즉 64x64 셀 보드로 테스트함
 *
 *   필드
 *   - grid, gridSize          : 하위 셀 배열과 한 변의 칸 수
 *   - amActive                : 지난 계산에서 바뀐 셀이 있었는지 (없으면 다음 계산을 건너뜀)
 *   - activeEdges             : 바뀐 셀이 있는 가장자리 방향 (이웃 블록을 깨울 때 씀)
 *   - oneLastRefreshRequired  : 안정된 직후 한 번 더 그려야 하는지
 *   - readingPermitted, nestingLevel : static. transition() 중에 그리기를 막는 잠금
 *   메서드
 *   - userClicked(점, 영역)    : 픽셀 좌표로 하위 셀을 찾아 클릭을 전달하고, 가장자리 표시를 켬
 *   - clear()                  : 모든 하위 셀과 activeEdges, amActive를 지움
 *   - isDisruptiveTo()         : activeEdges 반환
 *   - figureNextState(이웃 8개) : 활성 블록이거나 이웃이 이쪽 가장자리를 깨우면 하위 셀의 다음 상태를 계산
 *   - transition()             : 하위 셀에 다음 상태를 적용하고 activeEdges를 갱신
 *   - edge(행, 열)             : 가장자리 셀 반환 (이웃 블록이 경계 너머 셀을 볼 때 씀)
 *   - transfer(memento, 좌표, LOAD/STORE) : 하위 셀을 재귀로 돌며 저장하거나 읽음
 *   - createMemento()          : 새 NeighborhoodState를 만들고 transfer(STORE)까지 수행해 반환
 *   - redraw(), create(), widthInCells(), isAlive() : 그리기, 복제(Prototype), Cell 가로 크기(gridSize x 하위 셀 크기), 항상 true
 *
 * [테스트 시나리오]
 *   1. 클릭: 픽셀 좌표가 올바른 셀로 바뀜 (꼭짓점, 셀 경계 픽셀, 블록 경계), 두 번 클릭하면 되돌아감
 *   2. Clear: 셀, 가장자리 표시, 다음 계산까지 모두 비워짐
 *   3. createMemento(): 만들면서 이미 산 셀을 기록함 (원본 동작: Universe.doStore()의 중복 저장 원인)
 *
 * [커버리지]
 *   - NeighborhoodTest.java 테스트 대상: userClicked, clear, isDisruptiveTo, createMemento, figureNextState(clear 직후 계산할 것이 없는 경우만)
 *   - 테스트 제외 대상
 *     - figureNextState, transition, edge의 세대 계산: 패턴과 경계별로 보드 전체가 필요해서 유닛 테스트에서 제외
 *     - transfer(LOAD): Store/Load 순서 전체가 필요해서 유닛 테스트에서 제외
 *     - redraw(): 화면 그리기라 유닛 테스트에서 제외
 *     - create(), widthInCells(), isAlive(): 한 줄짜리 고정 반환이라 생략
 */
class NeighborhoodTest {
	private static final int CELLS = 64;
	private static final Rectangle SCREEN = new Rectangle(0, 0, 512, 512);

	@Test
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
	@DisplayName("createMemento() (원본 동작): 만들면서 이미 산 셀을 기록한다. Universe.doStore()에서 중복 저장 발생")
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

	// Universe와 같은 구조: 8x8 블록 안에 8x8 셀
	private static Neighborhood newBoard() {
		return new Neighborhood(8, new Neighborhood(8, new Resident()));
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
}

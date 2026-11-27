package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Neighborhood 통합 테스트: 진짜 Resident를 담은 블록의 세대 계산 뒤 활성 여부와 칠하기 (Composite 패턴, 셀 색 기능)
 * 8x8 블록 하나 또는 Universe와 같은 구조인 Neighborhood(8, Neighborhood(8, Resident))로 테스트함. 출생 색 규칙은 가짜(RULE)
 * 통합 테스트에 둔 이유: 결과가 Neighborhood만이 아니라 진짜 Resident의 규칙에 달려 있음
 * (blinker는 한 세대 뒤 바뀌고 2x2 block은 안정하다는 세대 규칙, 같은 색으로 두 번 칠하면 죽는다는 칠하기 규칙)
 *
 * [테스트 시나리오]
 *   1. 칠하기 활성 표시: 생사는 그대로이고 색만 바뀐 셀이 있어도 블록이 활성이 되어 다시 그려짐
 *   2. 같은 색으로 두 번 칠하면 셀이 죽음
 *   3. 활성 여부: 한 세대 계산 뒤 바뀐 셀이 있으면 활성, 없으면 활성이 아님
 *
 * [커버리지]
 *   - NeighborhoodResidentTest.java 테스트 대상: Neighborhood.userBrushed, isActive, figureNextState, transition (진짜 Resident와 함께)
 *   - 테스트 제외 대상
 *     - 픽셀 → 하위 셀 전달, Clear, createMemento, transfer(LOAD) 뒤 활성 여부, 방문 순서: 단위 테스트 NeighborhoodTest에서 확인
 *     - 칠하기 규칙 자체(빈 칸, 다른 색, 같은 색): 단위 테스트 ResidentTest에서 확인
 *     - 블록 경계를 넘는 세대 계산: TickContractTest(BoardTickTest)에서 확인
 */
class NeighborhoodResidentTest {
	private static final Rectangle SCREEN = new Rectangle(0, 0, 512, 512);
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	// [수정] 2026-11-27: 진짜 ClashRule 대신 가짜 출생 색 규칙. 부모 색과 상관없이 기본색(빨강)으로 태어남
	private static final Supplier<BirthColorRule> RULE = () -> parents -> CellState.alive(CellColor.DEFAULT);

	//  칠하기

	@Test
	@Tag("extension")
	@DisplayName("userBrushed(): 산 셀의 색만 바꿔도 블록이 활성이 된다 (그래야 다시 그려짐)")
	void recolorMarksBlockActive(){
		// Given: 빨강 2x2 block(안정 패턴)을 놓고 한 세대 계산해서 블록이 쉬는 상태
		Neighborhood block = new Neighborhood(8, new Resident(RULE));
		Rectangle surface = new Rectangle(0, 0, 64, 64);
		for (Point pixel : List.of(new Point(20, 20), new Point(28, 20), new Point(20, 28), new Point(28, 28))) {
			block.userBrushed(pixel, surface, CellColor.DEFAULT);
		}
		block.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
							  Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		block.transition();
		assertFalse(block.isActive(), "칠하기 전");

		// When: 산 셀 하나를 파랑으로 칠함 (생사는 그대로)
		block.userBrushed(new Point(20, 20), surface, BLUE);

		// Then
		assertTrue(block.isActive(), "칠한 뒤");
	}

	@Test
	@Tag("extension")
	@DisplayName("userBrushed(): 같은 색으로 두 번 칠하면 셀이 죽는다")
	void sameColorTwiceKills() throws Exception{
		// Given
		Neighborhood board = newBoard();
		board.userBrushed(new Point(100, 100), SCREEN, BLUE);

		// When
		board.userBrushed(new Point(100, 100), SCREEN, BLUE);

		// Then
		assertEquals(List.of(), List.copyOf(storedPoints((Cell.Memento) board.createMemento())));
	}

	//  활성 여부

	@Test
	@Tag("extension")
	@DisplayName("isActive(): 한 세대 계산에서 바뀐 셀이 있으면 활성이다")
	void blockWithChangedCellsIsActiveAfterGeneration(){
		// Given: 가로 blinker (한 세대 뒤 세로로 바뀜)
		Neighborhood block = blockWith(new Point(2, 3), new Point(3, 3), new Point(4, 3));

		// When
		advanceOneGeneration(block);

		// Then
		assertTrue(block.isActive());
	}

	@Test
	@Tag("extension")
	@DisplayName("isActive(): 한 세대 계산에서 바뀐 셀이 없으면 활성이 아니다")
	void blockWithoutChangesIsInactiveAfterGeneration(){
		// Given: 2x2 block (안정 패턴). 클릭으로 놓았으므로 계산 전에는 활성
		Neighborhood block = blockWith(new Point(3, 3), new Point(4, 3), new Point(3, 4), new Point(4, 4));
		assertTrue(block.isActive(), "계산 전");

		// When
		advanceOneGeneration(block);

		// Then
		assertFalse(block.isActive(), "계산 뒤");
	}

	// ---- 헬퍼 메서드

	// Universe와 같은 구조: 8x8 블록 안에 8x8 셀
	private static Neighborhood newBoard() {
		return new Neighborhood(8, new Neighborhood(8, new Resident(RULE)));
	}

	// memento에는 기록된 좌표 목록을 묻는 메서드가 없으므로, 저장한 내용의 첫 객체(산 셀 좌표)를 다시 읽음
	private static Collection<?> storedPoints(Cell.Memento memento) throws IOException, ClassNotFoundException{
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		memento.flush(file);

		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(file.toByteArray()));
		return (Collection<?>) in.readObject();
	}

	// 8x8 블록 하나에 셀(x = 열, y = 행)을 클릭으로 놓음. 셀 하나가 1픽셀인 영역을 씀
	private static Neighborhood blockWith(Point... cells){
		Neighborhood block = new Neighborhood(8, new Resident(RULE));
		for (Point cell : cells) {
			block.userClicked(cell, new Rectangle(0, 0, 8, 8));
		}
		return block;
	}

	// 블록 하나를 보드 밖(Cell.DUMMY)으로 둘러싸고 한 세대 진행
	private static void advanceOneGeneration(Neighborhood block){
		block.figureNextState(Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
							  Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		block.transition();
	}
}

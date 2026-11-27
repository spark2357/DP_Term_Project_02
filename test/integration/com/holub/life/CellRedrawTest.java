package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/**
 * Neighborhood.redraw() 통합 테스트: 진짜 Resident를 담은 블록이 CellPainter에게 "무엇을" 그릴지 제대로 넘기는지 화면 없이 확인함 (이슈 #6)
 * 호출을 순서대로 기록하는 가짜 Painter를 redraw()에 넘기고, 기록된 호출을 비교함. 출생 색 규칙도 가짜(RULE)
 * 통합 테스트에 둔 이유: 그린 결과(어느 셀을 alive로 그리는지)가 Neighborhood만이 아니라 안에 넣은 진짜 Resident의 상태와 redraw()에 달려 있음
 * 설계 개선 ③ 구조 변경 후 동작 확인으로 분류: Graphics 대신 CellPainter로 그리게 바뀐 구조에서 그리기 동작이 맞는지 확인함
 * [수정] 2026-11-27: Resident.redraw() 테스트 2개는 단위 테스트 ResidentTest로 옮기고, Neighborhood.redraw() 테스트만 남겨 test/unit에서 test/integration으로 옮김
 *
 * [테스트 시나리오]
 *   1. 블록: 산 셀만 alive=true로 그리고, 셀을 다 그린 뒤 활성 테두리를 그림
 *   2. 활성 블록: drawAll=false여도 active=true 테두리까지 그림
 *   3. 비활성 블록: drawAll=true면 active=false 테두리를 그리고, drawAll=false면 아무것도 그리지 않음
 *   4. 중첩 블록: drawAll=false일 때 바뀐 안쪽 블록만 그림
 *
 * [커버리지]
 *   - CellRedrawTest.java 테스트 대상: Neighborhood.redraw (진짜 Resident와 함께)
 *   - 테스트 제외 대상
 *     - SwingCellPainter: Swing 화면이라 제외 (색이나 도형을 실제로 그리는 일)
 *     - 셀 하나의 그리기 Resident.redraw(생사와 색): 단위 테스트 ResidentTest에서 확인
 */
@Tag("improvement")
class CellRedrawTest {
	private static final Rectangle SCREEN = new Rectangle(0, 0, 40, 40);
	// [수정] 2026-11-27: 진짜 ClashRule 대신 가짜 출생 색 규칙. 이 테스트에서는 아무도 태어나지 않으므로 쓰이지 않음
	private static final Supplier<BirthColorRule> RULE = () -> parents -> CellState.alive(CellColor.DEFAULT);

	private static Call cell(int x, int y, int size, boolean alive) {
		return new Call("cell", new Rectangle(x, y, size, size), alive);
	}

	private static Call border(int x, int y, int size, boolean active) {
		return new Call("border", new Rectangle(x, y, size, size), active);
	}

	@Test
	@DisplayName("Neighborhood.redraw(): 살아 있는 셀만 alive=true로 그리고, 셀을 다 그린 뒤 활성 테두리를 그린다")
	void onlyLiveCellsArePaintedAliveThenActiveBorder() {
		// Given: 2x2 블록의 0행 1열 셀만 살아 있음 (클릭하면 블록도 활성 상태가 됨)
		Neighborhood block = new Neighborhood(2, new Resident(RULE));
		block.userClicked(new Point(25, 5), SCREEN);
		RecordingPainter painter = new RecordingPainter();

		// When
		block.redraw(painter, SCREEN, true);

		// Then
		assertEquals(List.of(
				cell(0, 0, 20, false),
				cell(20, 0, 20, true),
				cell(0, 20, 20, false),
				cell(20, 20, 20, false),
				border(0, 0, 40, true)), painter.calls);
	}

	@Test
	@DisplayName("Neighborhood.redraw(): 활성 블록은 drawAll=false여도 active=true 테두리까지 그린다")
	void activeBlockIsPaintedWithoutDrawAll() {
		// Given
		Neighborhood block = new Neighborhood(2, new Resident(RULE));
		block.userClicked(new Point(5, 5), SCREEN);
		RecordingPainter painter = new RecordingPainter();

		// When
		block.redraw(painter, SCREEN, false);

		// Then
		assertEquals(border(0, 0, 40, true), painter.calls.get(painter.calls.size() - 1));
	}

	@Test
	@DisplayName("Neighborhood.redraw(): 비활성 블록을 drawAll=true로 그리면 active=false 테두리를 그린다")
	void inactiveBlockGetsInactiveBorderOnDrawAll() {
		// Given
		Neighborhood block = new Neighborhood(2, new Resident(RULE));
		RecordingPainter painter = new RecordingPainter();

		// When
		block.redraw(painter, SCREEN, true);

		// Then
		assertEquals(border(0, 0, 40, false), painter.calls.get(painter.calls.size() - 1));
	}

	@Test
	@DisplayName("Neighborhood.redraw(): 비활성 블록은 drawAll=false면 아무것도 그리지 않는다")
	void inactiveBlockPaintsNothingWithoutDrawAll() {
		// Given
		Neighborhood block = new Neighborhood(2, new Resident(RULE));
		RecordingPainter painter = new RecordingPainter();

		// When
		block.redraw(painter, SCREEN, false);

		// Then
		assertTrue(painter.calls.isEmpty());
	}

	@Test
	@DisplayName("Neighborhood.redraw(): 블록이 중첩돼 있으면 drawAll=false일 때 바뀐 안쪽 블록만 그린다")
	void nestedRedrawPaintsOnlyChangedBlock() {
		// Given: 2x2 블록 4개 중 오른쪽 위 블록의 왼쪽 위 셀만 클릭함
		Neighborhood outer = new Neighborhood(2, new Neighborhood(2, new Resident(RULE)));
		outer.userClicked(new Point(25, 5), SCREEN);
		RecordingPainter painter = new RecordingPainter();

		// When
		outer.redraw(painter, SCREEN, false);

		// Then: 오른쪽 위 블록의 셀 4개와 테두리, 바깥 테두리만 그림
		assertEquals(List.of(
				cell(20, 0, 10, true),
				cell(30, 0, 10, false),
				cell(20, 10, 10, false),
				cell(30, 10, 10, false),
				border(20, 0, 20, true),
				border(0, 0, 40, true)), painter.calls);
	}

	/** 기록된 호출 하나. flag는 paintCell의 alive 또는 paintBorder의 active임 */
	private record Call(String kind, Rectangle here, boolean flag) {
	}

	/** 호출을 순서대로 기록함. Neighborhood가 Rectangle을 옮겨 가며 재사용하므로 복사해서 저장함 */
	private static class RecordingPainter implements CellPainter {
		final List<Call> calls = new ArrayList<>();

		public void paintCell(Rectangle here, boolean alive) {
			calls.add(new Call("cell", new Rectangle(here), alive));
		}

		public void paintBorder(Rectangle here, boolean active) {
			calls.add(new Call("border", new Rectangle(here), active));
		}
	}
}

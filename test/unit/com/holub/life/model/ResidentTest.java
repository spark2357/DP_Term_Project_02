package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Resident 단위 테스트: 보드의 셀 하나 (Composite 패턴의 Leaf, 셀 색 기능에서는 State 패턴의 Context)
 * 출생 색 규칙과 Memento, Painter, 방문자는 가짜를 씀. 이웃 셀은 같은 Resident이거나 보드 밖(Cell.DUMMY)
 *
 *   필드
 *   - state          : 지금 상태 (CellState: Dead.INSTANCE 또는 색이 있는 산 상태)
 *   - nextState      : 다음 세대 상태 (figureNextState()가 계산한 값, 보드 전체 계산 후 transition()에서 반영)
 *   - birthColorRule : 지금 출생 색 규칙을 알려 주는 출처 (Supplier<BirthColorRule>, Board가 들고 있는 규칙)
 *   메서드
 *   - figureNextState(이웃 8개) : B3/S23 규칙으로 nextState를 계산. 죽을 셀은 Dead.INSTANCE, 살아남는 셀은 지금 상태 그대로,
 *                                 태어나는 셀은 산 이웃(부모)의 색을 출생 색 규칙에 넘겨 받은 상태. 바뀔 셀이면 true
 *   - transition()              : state = nextState 로 적용. 상태가 바뀌었으면 true
 *   - isAlive(), getColor()     : 지금 상태의 생사와 색 (죽은 셀은 빈 Optional)
 *   - userClicked(점, 영역)     : 기본색(빨강)으로 칠하기와 같음 (좌표는 쓰지 않음)
 *   - userBrushed(점, 영역, 색) : 다음 상태는 지금 상태가 정함 (빈 칸 → 그 색으로 삶, 다른 색 → 색만 바뀜, 같은 색 → 죽음)
 *   - clear()                   : state, nextState 모두 Dead.INSTANCE
 *   - isDisruptiveTo()          : 바뀔 셀이면 Direction.ALL, 아니면 Direction.NONE
 *   - transfer(memento, 좌표, LOAD/STORE) : memento의 색으로 살리거나(LOAD) 산 셀을 색과 함께 기록(STORE)
 *   - redraw(painter, 영역, drawAll) : Painter에게 색까지 든 지금 상태를 넘김
 *   - create()                  : 같은 출생 색 규칙 출처를 쓰는 새 셀 (Prototype)
 *   - accept(방문자)             : 방문자에게 자기 자신을 셀로 알림 (Visitor 패턴)
 *   - createMemento()           : 항상 UnsupportedOperationException (memento는 Neighborhood가 만듦)
 *   - edge(), widthInCells()    : 이웃 조회, Cell 가로 크기(Resident는 Leaf라서 1)
 *   - verify(), isStable(), newborn() : private 보조 메서드
 *
 * [테스트 시나리오]
 *   1. B3/S23 규칙: 죽은 셀과 산 셀 각각에 대해 이웃 0~8개를 모두 넣어 탄생, 생존, 사망을 확인
 *   2. 두 단계 갱신: figureNextState()만으로는 상태가 바뀌지 않고 transition()에서 바뀜
 *   3. transition()의 반환값: 바뀐 셀만 true여야 하지만 원본은 반대로 반환함 (결함: 블록 건너뛰기 최적화가 동작하지 않는 원인)
 *   4. 보드 밖: Cell.DUMMY가 죽은 이웃으로 세어짐
 *   5. Clear: 계산해 둔 다음 상태까지 지워짐
 *   6. Store/Load: 산 셀만 기록되고, Load한 셀은 안정 상태로 들어감 (원본 동작)
 *   7. 칠하기와 클릭: 빈 칸, 다른 색, 같은 색에 칠한 결과. 다른 색 셀을 클릭하면 기본색(빨강)으로 바뀜
 *   8. 출생 색: 태어나는 셀은 산 이웃의 색을 규칙에 넘기고 규칙이 정한 색으로 태어남. 규칙이 Dead.INSTANCE면 태어나지 않음.
 *      살아남는 셀은 규칙에 묻지 않음. 규칙은 출생마다 다시 읽고, 복제한 셀(create())도 같은 출처를 씀
 *   9. 색 저장/불러오기: Load한 셀은 memento의 색, Store하면 산 셀의 색도 기록
 *  10. 그리기: redraw()는 Painter에게 색까지 든 지금 상태를 넘김
 *  11. 방문: accept()는 방문자에게 자기 자신을 셀로 알림
 *  12. 그리기(생사): 산 셀은 산 상태로, 죽은 셀은 죽은 상태로 자기 영역을 그림 (CellRedrawTest에서 옮김)
 *
 * [커버리지]
 *   - ResidentTest.java 테스트 대상: figureNextState, transition(상태 변화와 반환값), isAlive, getColor, userClicked, userBrushed,
 *     clear, isDisruptiveTo, transfer, redraw, create, accept
 *   - 테스트 제외 대상
 *     - edge(), widthInCells(), createMemento(): 한 줄짜리 고정 반환이라 생략
 */
class ResidentTest {
	// [수정] 2026-11-27: 진짜 ClashRule 대신 가짜 출생 색 규칙. 부모 색과 상관없이 기본색(빨강)으로 태어남
	private static final Supplier<BirthColorRule> RULE = () -> parents -> new Alive(CellColor.DEFAULT);

	@Test
	@Tag("original")
	@DisplayName("figureNextState(), transition(): 죽은 셀은 이웃이 정확히 3개일 때만 태어난다")
	void deadCellIsBornOnlyWithThreeNeighbors() {
		for (int liveNeighbors = 0; liveNeighbors <= 8; liveNeighbors++) {
			// Given
			Resident cell = new Resident(RULE);

			// When
			figureNextState(cell, neighbors(liveNeighbors));
			cell.transition();

			// Then
			boolean expected = (liveNeighbors == 3);
			assertEquals(expected, cell.isAlive(), "liveNeighbors " + liveNeighbors + "개");
		}
	}

	@Test
	@Tag("original")
	@DisplayName("figureNextState(), transition(): 산 셀은 이웃이 2~3개면 살고, 나머지는 죽는다")
	void liveCellSurvivesOnlyWithTwoOrThreeNeighbors() {
		for (int liveNeighbors = 0; liveNeighbors <= 8; liveNeighbors++) {
			// Given
			Resident cell = liveCell();

			// When
			figureNextState(cell, neighbors(liveNeighbors));
			cell.transition();

			// Then
			boolean expected = (liveNeighbors == 2 || liveNeighbors == 3);
			assertEquals(expected, cell.isAlive(), "liveNeighbors " + liveNeighbors + "개");
		}
	}

	@Test
	@Tag("original")
	@DisplayName("figureNextState(), transition(): 계산만으로는 상태가 바뀌지 않고, transition()에서 바뀐다")
	void stateChangesOnlyOnTransition() {
		// Given
		Resident cell = new Resident(RULE);

		// When: 다음 상태만 계산
		figureNextState(cell, neighbors(3));

		// Then: 아직 죽어 있음
		assertFalse(cell.isAlive(), "figureNextState() 직후");

		// When: 다음 상태 적용
		cell.transition();

		// Then: 이제 살아 있음
		assertTrue(cell.isAlive(), "transition() 직후");
	}

	@Test
	@Tag("improvement")
	@DisplayName("transition(): 상태가 바뀐 셀만 true를 반환해야 한다 (결함)")
	void transitionReturnsTrueOnlyWhenChanged() {
		// Given: 그대로 죽어 있을 셀과, 태어날 셀
		// 원본은 반대로 반환해 블록 건너뛰기 최적화가 동작하지 않음
		Resident unchanged = new Resident(RULE);
		figureNextState(unchanged, neighbors(0));

		Resident changed = new Resident(RULE);
		figureNextState(changed, neighbors(3));

		// When
		boolean unchangedResult = unchanged.transition();
		boolean changedResult = changed.transition();

		// Then
		// 원본은 바꾸기 전의 isStable()을 반환해서 결과가 반대로 나옴 (Resident.java:81)
		// Neighborhood.transition()은 이 값으로 바뀐 가장자리를 표시하므로,
		// 바뀌지 않은 셀 때문에 매번 모든 가장자리가 표시되어 블록 건너뛰기가 동작하지 않음
		assertFalse(unchangedResult, "바뀌지 않은 셀");
		assertTrue(changedResult, "태어난 셀");
	}

	@Test
	@Tag("original")
	@DisplayName("figureNextState(): 보드 밖을 나타내는 Cell.DUMMY는 항상 죽은 이웃으로 계산한다")
	void dummyCountsAsDeadNeighbor() {
		// Given: 산 이웃 2개 + DUMMY 6개
		Resident cell = new Resident(RULE);
		Cell[] around = {
			liveCell(), liveCell(),
			Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY
		};

		// When
		figureNextState(cell, around);
		cell.transition();
	
		// Then: DUMMY는 liveNeighbors가 아님
		assertFalse(Cell.DUMMY.isAlive());
		assertFalse(cell.isAlive());
	}

	@Test
	@Tag("original")
	@DisplayName("clear(): 미리 계산해 둔 다음 상태까지 지워서 초기화한다")
	void clearAlsoForgetsNextState() {
		// Given: 다음 세대에 태어나야하는 셀
		Resident cell = new Resident(RULE);
		figureNextState(cell, neighbors(3));

		// When
		cell.clear();
		cell.transition();

		// Then
		assertFalse(cell.isAlive());
	}

	@Test
	@Tag("original")
	@DisplayName("transfer(LOAD), isDisruptiveTo(): Load한 셀은 변화가 없는 셀이라 Go 전에는 이웃을 깨우지 않는다. Neighborhood.transfer()에서 문제 발생 (원본 동작)")
	void loadedCellIsNotDisruptive() {
		// Given: (0,0)이 살아 있다고 기록된 memento
		Cell.Memento memento = emptyMemento();
		memento.markAsAlive(new Point(0, 0));

		Resident clicked = new Resident(RULE);
		Resident loaded = new Resident(RULE);

		// When: 하나는 클릭으로, 하나는 Load로 살림
		clicked.userClicked(new Point(0, 0), new Rectangle(0, 0, 8, 8));
		loaded.transfer(memento, new Point(0, 0), Cell.LOAD);

		// Then: 둘 다 살아 있지만, 클릭한 셀은 ALL, Load한 셀은 NONE을 반환
		// Direction.equals(Direction)은 equals(Object)를 재정의하지 않으므로 assertEquals 대신 직접 호출
		assertTrue(clicked.isAlive());
		assertTrue(loaded.isAlive());
		assertTrue(clicked.isDisruptiveTo().equals(Direction.ALL));
		assertTrue(loaded.isDisruptiveTo().equals(Direction.NONE));
	}

	@Test
	@Tag("original")
	@DisplayName("transfer(STORE): 산 셀만 memento에 기록한다")
	void storeRecordsOnlyLiveCells() throws Exception {
		// Given
		FakeMemento memento = emptyMemento();
		Resident live = liveCell();
		Resident dead = new Resident(RULE);

		// When
		live.transfer(memento, new Point(3, 5), Cell.STORE);
		dead.transfer(memento, new Point(4, 5), Cell.STORE);

		// Then: memento에 기록된 좌표는 산 셀의 (3,5) 하나뿐
		Collection<Point> stored = storedPoints(memento);
		assertEquals(1, stored.size());
		assertTrue(stored.contains(new Point(3, 5)));
	}

	// [수정] 2026-11-27: CellRedrawTest에서 옮김. Resident 하나만 보는 단위 테스트라서 가짜 Painter(StateRecordingPainter)로 확인
	@Test
	@Tag("original")
	@DisplayName("redraw(): 살아 있으면 산 상태로 자기 영역을 그린다")
	void liveResidentIsPaintedAlive(){
		// Given: 클릭으로 살린 셀
		Resident cell = liveCell();
		StateRecordingPainter painter = new StateRecordingPainter();
		Rectangle here = new Rectangle(0, 0, 8, 8);

		// When
		cell.redraw(painter, here, false);

		// Then: 자기 영역을 한 번, 산 상태로 그림
		assertEquals(List.of(here), painter.areas);
		assertTrue(painter.states.get(0).isAlive());
	}

	@Test
	@Tag("original")
	@DisplayName("redraw(): 죽어 있으면 죽은 상태로 자기 영역을 그린다")
	void deadResidentIsPaintedDead(){
		// Given
		Resident cell = new Resident(RULE);
		StateRecordingPainter painter = new StateRecordingPainter();
		Rectangle here = new Rectangle(0, 0, 8, 8);

		// When
		cell.redraw(painter, here, false);

		// Then: 자기 영역을 한 번, 죽은 상태로 그림
		assertEquals(List.of(here), painter.areas);
		assertFalse(painter.states.get(0).isAlive());
	}

	private static Resident liveCell() {
		Resident cell = new Resident(RULE);
		cell.userClicked(new Point(0, 0), new Rectangle(0, 0, 8, 8));
		return cell;
	}

	// 이웃 8개: 앞의 liveCount개는 산 셀, 나머지는 죽은 셀
	private static Cell[] neighbors(int liveCount) {
		Cell[] around = new Cell[8];
		for (int i = 0; i < 8; i++) {
			if (i < liveCount) {
				around[i] = liveCell();
			} else {
				around[i] = new Resident(RULE);
			}
		}
		return around;
	}

	private static boolean figureNextState(Resident cell, Cell[] around) {
		return cell.figureNextState(around[0], around[1], around[2], around[3],
									around[4], around[5], around[6], around[7]);
	}

	// Resident는 memento를 만들 수 없으므로 Neighborhood에서 생성
	// [수정] 2026-11-27: 진짜 NeighborhoodState 대신 가짜 Memento(FakeMemento)를 씀
	private static FakeMemento emptyMemento() {
		return new FakeMemento();
	}

	// memento에는 기록된 좌표 개수를 묻는 메서드가 없으므로, 저장한 내용을 다시 읽어 좌표 목록을 꺼냄
	// [수정] 2026-11-27: 가짜 Memento는 기록한 좌표를 바로 보여 주므로 파일로 쓰고 다시 읽지 않음
	private static Collection<Point> storedPoints(FakeMemento memento) {
		return memento.cells.keySet();
	}

	//  셀 색 기능 (State 패턴의 Context, 출생 색 규칙 Strategy 사용)

	private static final CellColor RED = CellColor.DEFAULT;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	@ParameterizedTest(name = "{0}")
	@MethodSource("brushCases")
	@Tag("extension")
	@DisplayName("userBrushed(): 다음 상태는 지금 상태가 정한다 (빈 칸 → 그 색으로 삶, 다른 색 → 색만 바뀜, 같은 색 → 죽음)")
	void brushingFollowsCellState(String name, List<CellColor> before, CellColor brush, Optional<CellColor> expected){
		// Given
		Resident cell = new Resident(RULE);
		for (CellColor color : before) {
			brush(cell, color);
		}

		// When
		brush(cell, brush);

		// Then
		assertEquals(expected.isPresent(), cell.isAlive());
		assertEquals(expected, cell.getColor());
	}

	private static Stream<Arguments> brushCases(){
		return Stream.of(
				Arguments.of("빈 칸", List.of(), BLUE, Optional.of(BLUE)),
				Arguments.of("다른 색 셀", List.of(RED), BLUE, Optional.of(BLUE)),
				Arguments.of("같은 색 셀", List.of(BLUE), BLUE, Optional.empty()));
	}

	@Test
	@Tag("extension")
	@DisplayName("userClicked(): 다른 색으로 산 셀을 클릭하면 죽지 않고 기본색(빨강)으로 바뀐다")
	void clickingColoredCellMakesItDefault(){
		// Given
		Resident cell = new Resident(RULE);
		brush(cell, BLUE);

		// When
		cell.userClicked(new Point(0, 0), new Rectangle(0, 0, 8, 8));

		// Then
		assertTrue(cell.isAlive());
		assertEquals(Optional.of(CellColor.DEFAULT), cell.getColor());
	}

	@Test
	@Tag("extension")
	@DisplayName("figureNextState(): 태어나는 셀은 산 이웃의 색을 규칙에 넘기고, 규칙이 정한 색으로 태어난다")
	void newbornGetsParentsAndColorFromRule(){
		// Given: 파랑, 파랑, 빨강 이웃과, 받은 부모를 기록하고 초록을 돌려주는 규칙
		RecordingRule rule = new RecordingRule(new Alive(GREEN));
		Resident cell = new Resident(() -> rule);
		Cell[] around = coloredNeighbors(BLUE, BLUE, RED);

		// When
		figureNextState(cell, around);
		cell.transition();

		// Then: 부모는 순서와 상관없이 파랑 2, 빨강 1
		assertEquals(1, rule.calls.size());
		assertEquals(countColors(List.of(BLUE, BLUE, RED)), countColors(rule.calls.get(0)));
		assertTrue(cell.isAlive());
		assertEquals(Optional.of(GREEN), cell.getColor());
	}

	@Test
	@Tag("extension")
	@DisplayName("figureNextState(): 규칙이 Dead.INSTANCE를 돌려주면 이웃이 3개여도 태어나지 않는다")
	void ruleReturningDeadMeansNotBorn(){
		// Given
		RecordingRule rule = new RecordingRule(Dead.INSTANCE);
		Resident cell = new Resident(() -> rule);

		// When
		figureNextState(cell, coloredNeighbors(BLUE, GREEN, YELLOW));
		cell.transition();

		// Then
		assertFalse(cell.isAlive());
		assertEquals(Optional.empty(), cell.getColor());
	}

	@Test
	@Tag("extension")
	@DisplayName("figureNextState(): 살아남는 셀은 규칙에 묻지 않고 자기 색을 그대로 가진다")
	void survivorKeepsColorWithoutAskingRule(){
		// Given: 파랑으로 산 셀과 빨강 이웃 3개 (살아남는 수)
		RecordingRule rule = new RecordingRule(new Alive(GREEN));
		Resident cell = new Resident(() -> rule);
		brush(cell, BLUE);

		// When
		figureNextState(cell, coloredNeighbors(RED, RED, RED));
		cell.transition();

		// Then
		assertEquals(Optional.of(BLUE), cell.getColor());
		assertEquals(List.of(), rule.calls, "규칙을 부른 기록");
	}

	@Test
	@Tag("extension")
	@DisplayName("figureNextState(): 규칙은 태어날 때마다 다시 읽으므로, 중간에 바꾼 규칙이 다음 출생부터 쓰인다")
	void ruleIsReadAtEachBirth(){
		// Given: 지금 규칙을 바꿀 수 있는 출처
		RuleHolder holder = new RuleHolder(new RecordingRule(new Alive(GREEN)));
		Resident cell = new Resident(holder);
		figureNextState(cell, coloredNeighbors(BLUE, BLUE, BLUE));
		cell.transition();
		assertEquals(Optional.of(GREEN), cell.getColor(), "첫 출생");
		cell.clear();

		// When: 규칙을 바꾼 뒤 다시 태어남
		holder.rule = new RecordingRule(new Alive(YELLOW));
		figureNextState(cell, coloredNeighbors(BLUE, BLUE, BLUE));
		cell.transition();

		// Then
		assertEquals(Optional.of(YELLOW), cell.getColor(), "두 번째 출생");
	}

	@Test
	@Tag("extension")
	@DisplayName("create(): 복제한 셀도 같은 규칙 출처를 써서, 나중에 바꾼 규칙을 따른다")
	void createdCellSharesRule(){
		// Given
		RuleHolder holder = new RuleHolder(new RecordingRule(new Alive(GREEN)));
		Resident original = new Resident(holder);
		Resident copy = (Resident) original.create();

		// When: 복제한 뒤 규칙을 바꾸고 복제본이 태어남
		holder.rule = new RecordingRule(new Alive(YELLOW));
		figureNextState(copy, coloredNeighbors(BLUE, BLUE, BLUE));
		copy.transition();

		// Then
		assertEquals(Optional.of(YELLOW), copy.getColor());
	}

	@Test
	@Tag("extension")
	@DisplayName("transfer(LOAD): memento에 기록된 색으로 살아난다")
	void loadUsesMementoColor(){
		// Given
		Cell.Memento memento = emptyMemento();
		memento.markAsAlive(new Point(2, 3), BLUE);
		Resident cell = new Resident(RULE);

		// When
		cell.transfer(memento, new Point(2, 3), Cell.LOAD);

		// Then
		assertTrue(cell.isAlive());
		assertEquals(Optional.of(BLUE), cell.getColor());
	}

	@Test
	@Tag("extension")
	@DisplayName("transfer(STORE): 산 셀의 색도 memento에 기록한다")
	void storeWritesColor(){
		// Given
		Cell.Memento memento = emptyMemento();
		Resident cell = new Resident(RULE);
		brush(cell, BLUE);

		// When
		cell.transfer(memento, new Point(2, 3), Cell.STORE);

		// Then
		assertTrue(memento.isAlive(new Point(2, 3)));
		assertEquals(BLUE, memento.colorAt(new Point(2, 3)));
	}

	@Test
	@Tag("extension")
	@DisplayName("redraw(): Painter에게 색까지 든 지금 상태를 넘긴다")
	void redrawPassesColoredState(){
		// Given
		Resident cell = new Resident(RULE);
		brush(cell, BLUE);
		StateRecordingPainter painter = new StateRecordingPainter();
		Rectangle here = new Rectangle(0, 0, 8, 8);

		// When
		cell.redraw(painter, here, false);

		// Then
		assertEquals(1, painter.states.size());
		assertTrue(painter.states.get(0).isAlive());
		assertEquals(Optional.of(BLUE), painter.states.get(0).getColor());
		assertEquals(here, painter.areas.get(0));
	}

	@Test
	@Tag("extension")
	@DisplayName("accept(): 방문자에게 자기 자신을 셀로 한 번 알린다")
	void acceptTellsVisitorAboutItself(){
		// Given
		Resident cell = new Resident(RULE);
		RecordingVisitor visitor = new RecordingVisitor();

		// When
		cell.accept(visitor);

		// Then
		assertEquals(List.of(cell), visitor.residents);
	}

	private static void brush(Resident cell, CellColor color){
		cell.userBrushed(new Point(0, 0), new Rectangle(0, 0, 8, 8), color);
	}

	// 앞 칸은 주어진 색으로 산 Resident, 나머지는 보드 밖(Cell.DUMMY). verify()가 Resident와 DUMMY만 받으므로 가짜 셀은 쓰지 않음
	private static Cell[] coloredNeighbors(CellColor... colors){
		Cell[] around = new Cell[8];
		for (int i = 0; i < 8; i++) {
			if (i < colors.length) {
				Resident neighbor = new Resident(RULE);
				brush(neighbor, colors[i]);
				around[i] = neighbor;
			} else {
				around[i] = Cell.DUMMY;
			}
		}
		return around;
	}

	private static Map<CellColor, Integer> countColors(List<CellColor> colors){
		Map<CellColor, Integer> counts = new HashMap<>();
		for (CellColor color : colors) {
			counts.merge(color, 1, Integer::sum);
		}
		return counts;
	}

	// 받은 부모 목록을 기록하고, 정해 둔 상태를 돌려주는 가짜 규칙
	private static class RecordingRule implements BirthColorRule{
		private final CellState result;
		private final List<List<CellColor>> calls = new ArrayList<>();

		RecordingRule(CellState result){
			this.result = result;
		}

		@Override
		public CellState newborn(List<CellColor> parents){
			calls.add(new ArrayList<>(parents));
			return result;
		}
	}

	// Board처럼 지금 규칙을 들고 있다가 물어보면 알려 주는 출처. 테스트 중에 rule을 바꿈
	private static class RuleHolder implements Supplier<BirthColorRule>{
		private BirthColorRule rule;

		RuleHolder(BirthColorRule rule){
			this.rule = rule;
		}

		@Override
		public BirthColorRule get(){
			return rule;
		}
	}

	// paintCell(영역, 상태)로 받은 상태를 기록하는 가짜 Painter
	private static class StateRecordingPainter implements CellPainter{
		private final List<CellState> states = new ArrayList<>();
		private final List<Rectangle> areas = new ArrayList<>();

		@Override
		public void paintCell(Rectangle here, boolean alive){
			fail("색을 모르는 paintCell(영역, alive)이 불림");
		}

		@Override
		public void paintCell(Rectangle here, CellState state){
			areas.add(new Rectangle(here));
			states.add(state);
		}

		@Override
		public void paintBorder(Rectangle here, boolean active){
		}
	}

	// 기록한 좌표와 색을 Map에 담기만 하는 가짜 Memento. 파일 읽기·쓰기는 쓰지 않음
	private static class FakeMemento implements Cell.Memento{
		private final Map<Point, CellColor> cells = new HashMap<>();

		@Override
		public void markAsAlive(Point location, CellColor color){
			cells.put(new Point(location), color);
		}

		@Override
		public boolean isAlive(Point location){
			return cells.containsKey(location);
		}

		@Override
		public CellColor colorAt(Point location){
			return cells.getOrDefault(location, CellColor.DEFAULT);
		}

		@Override
		public void load(InputStream in){
			throw new UnsupportedOperationException();
		}

		@Override
		public void flush(OutputStream out){
			throw new UnsupportedOperationException();
		}
	}

	// 셀을 알려 온 순서대로 기록하는 가짜 방문자. 셀은 블록에 들어가거나 나오지 않음
	private static class RecordingVisitor implements CellVisitor{
		private final List<Resident> residents = new ArrayList<>();

		@Override
		public void enterNeighborhood(Neighborhood block){
			fail("셀이 블록에 들어감을 알림");
		}

		@Override
		public void visitResident(Resident cell){
			residents.add(cell);
		}

		@Override
		public void leaveNeighborhood(Neighborhood block){
			fail("셀이 블록에서 나옴을 알림");
		}
	}
}

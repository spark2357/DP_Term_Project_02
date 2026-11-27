package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/**
 * 블록 건너뛰기 테스트: 바뀐 것이 없는 8x8 블록을 계산하지 않고 건너뛰는지, 세대마다 계산된 블록을 세어 확인
 * 블록 구조(Neighborhood 안에 Neighborhood)를 직접 감싸야 하므로 Contract 테스트가 아니라 현재 코드용. 원본은 OriginalBlockSkippingCountTest(known-defect)
 *
 *   계산된 블록을 알아내는 방법
 *   - 블록은 계산할 때만 이웃 블록의 가장자리 셀을 edge()로 읽고, 건너뛸 때는 읽지 않음
 *   - 그래서 8x8 블록마다 CountingBlock을 씌우고, 그 블록에 넘기는 이웃을 EdgeProbe로 감싸 edge() 호출을 기록함
 *
 * [테스트 시나리오]
 *   1. 블록 안 blinker 하나만 움직이면 그 블록만 계산되어야 함. 원본은 2세대부터 64블록을 모두 계산 (결함)
 *   2. Undo 스냅샷처럼 세대마다 보드를 읽어도(transfer(STORE)) 계산되는 블록은 1과 같음
 *   원본 결함의 원인: 원본 Resident.transition()이 바뀌지 않은 셀에 true를 반환했음 (ResidentTest에서 확인).
 *         바깥 Neighborhood.transition()은 계산하지 않은 블록까지 모든 블록에 transition()을 부르므로,
 *         1세대 적용 때 64블록 모두 가장자리 표시가 켜지고 2세대부터 전부 계산되었음
 *
 * [커버리지]
 *   - BlockSkippingCountTest.java 테스트 대상: 세대마다 계산되는 블록 (성능)
 *   - 테스트 제외 대상
 *     - 계산 결과가 맞는지: TickContractTest에서 확인 (원본은 결과는 맞음)
 *   - 보드를 읽으면(transfer(STORE)) 클릭한 뒤 아직 세대가 진행되지 않은 셀이 있는 블록에 가장자리 표시가 늘어남.
 *     세대가 진행된 직후에는 늘지 않으므로 시나리오 2는 한 세대 진행한 뒤부터 확인하고, 1에서는 보드를 읽지 않음
 */
@Tag("improvement")
class BlockSkippingCountTest {
	private static final Rectangle SCREEN = new Rectangle(0, 0, 512, 512);

	@Test
	@DisplayName("figureNextState(): 블록 안 blinker 하나만 움직이면 그 블록만 계산된다 (결함)")
	void onlyTheBlockWithBlinkerIsComputed() {
		// Given: 블록 (0,0) 안쪽(가장자리가 아닌 곳)에 blinker. 원본은 2세대부터 64블록을 모두 계산했음
		Set<String> computed = new TreeSet<>();
		Neighborhood board = countingBoard(computed);
		click(board, 3, 2);
		click(board, 3, 3);
		click(board, 3, 4);

		for (int generation = 1; generation <= 10; generation++) {
			// When
			computed.clear();
			tick(board);

			// Then: 계산된 블록은 (0,0) 하나뿐이어야 함
			assertEquals(Set.of("(0,0)"), computed, generation + "세대에 계산된 블록");
		}
	}

	@Test
	@DisplayName("transfer(STORE): 세대마다 보드를 읽어도(Undo 스냅샷) 블록 안 blinker의 블록만 계산된다")
	void readingTheBoardEveryGenerationKeepsSkipping() {
		// Given
		Set<String> computed = new TreeSet<>();
		Neighborhood board = countingBoard(computed);
		click(board, 3, 2);
		click(board, 3, 3);
		click(board, 3, 4);
		tick(board);	// 클릭한 셀은 아직 안정 상태가 아니므로, 한 세대 진행한 뒤부터 확인

		for (int generation = 2; generation <= 10; generation++) {
			// When: Board.snapshot()과 같은 방법으로 보드 전체를 읽고 진행
			board.transfer(new CollectingMemento(), new Point(0, 0), Cell.STORE);
			computed.clear();
			tick(board);

			// Then
			assertEquals(Set.of("(0,0)"), computed, generation + "세대에 계산된 블록");
		}
	}

	// ---- 헬퍼 메서드

	// Universe와 같은 64x64 보드를 만들되, 8x8 블록마다 CountingBlock을 씌움
	private static Neighborhood countingBoard(Set<String> computed) {
		CountingBlock prototype = new CountingBlock(new Neighborhood(8, new Resident(ClashRule::new)), computed);
		return new Neighborhood(8, prototype);
	}

	private static void click(Neighborhood board, int row, int column) {
		board.userClicked(new Point(column * 8 + 4, row * 8 + 4), SCREEN);
	}

	// Universe의 clock 리스너와 같은 순서로 한 세대 진행
	private static void tick(Neighborhood board) {
		boolean willChange = board.figureNextState(
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		if (willChange) {
			board.transition();
		}
	}

	/**
	 * 8x8 블록 하나를 감싸는 Cell (Decorator). 블록에 넘기는 이웃을 EdgeProbe로 감싸는 것 말고는 그대로 위임
	 * 바깥 Neighborhood가 생성자에서 prototype.create()를 행 순서대로 64번 부르므로, 만든 순서로 블록 위치를 정함
	 */
	private static class CountingBlock implements Cell {
		private final Cell block;
		private final Set<String> computed;
		private final String name;		// 블록 위치 "(행,열)"
		private int created = 0;		// 프로토타입이 지금까지 만든 블록 수

		CountingBlock(Cell block, Set<String> computed) {
			this(block, computed, "prototype");
		}

		private CountingBlock(Cell block, Set<String> computed, String name) {
			this.block = block;
			this.computed = computed;
			this.name = name;
		}

		public boolean figureNextState(Cell north, Cell south, Cell east, Cell west,
									   Cell northeast, Cell northwest, Cell southeast, Cell southwest) {
			return block.figureNextState(
					probe(north), probe(south), probe(east), probe(west),
					probe(northeast), probe(northwest), probe(southeast), probe(southwest));
		}

		private Cell probe(Cell neighbor) {
			return new EdgeProbe(neighbor, computed, name);
		}

		public Cell create() {
			String position = "(" + (created / 8) + "," + (created % 8) + ")";
			created++;
			return new CountingBlock(block.create(), computed, position);
		}

		public Cell edge(int row, int column)						{ return block.edge(row, column); }
		public boolean transition()									{ return block.transition(); }
		public void redraw(CellPainter p, Rectangle here, boolean all)	{ block.redraw(p, here, all); }
		public void userClicked(Point here, Rectangle surface)		{ block.userClicked(here, surface); }
		public boolean isAlive()									{ return block.isAlive(); }
		public int widthInCells()									{ return block.widthInCells(); }
		public Direction isDisruptiveTo()							{ return block.isDisruptiveTo(); }
		public void clear()											{ block.clear(); }
		public boolean transfer(Storable memento, Point upperLeft, boolean load) { return block.transfer(memento, upperLeft, load); }
		public Storable createMemento()								{ return block.createMemento(); }
	}

	// 살아 있는 셀을 모으기만 하는 Memento (Board.snapshot()이 쓰는 것과 같은 역할)
	private static class CollectingMemento implements Cell.Memento {
		private final Set<Point> cells = new HashSet<>();

		public void markAsAlive(Point location, CellColor color)	{ cells.add(new Point(location)); }
		public boolean isAlive(Point location)	{ return cells.contains(location); }
		public void load(InputStream in)	{ throw new UnsupportedOperationException(); }
		public void flush(OutputStream out)	{ throw new UnsupportedOperationException(); }
	}

	/**
	 * 블록 하나가 보는 이웃 블록. edge()가 불리면 그 블록이 이번 세대에 계산되는 중이라는 뜻이므로 기록함
	 * 블록은 이웃에게 edge()와 isDisruptiveTo()만 물으므로 나머지 메서드는 쓰이지 않음
	 */
	private static class EdgeProbe implements Cell {
		private final Cell neighbor;
		private final Set<String> computed;
		private final String computingBlock;

		EdgeProbe(Cell neighbor, Set<String> computed, String computingBlock) {
			this.neighbor = neighbor;
			this.computed = computed;
			this.computingBlock = computingBlock;
		}

		public Cell edge(int row, int column) {
			computed.add(computingBlock);
			return neighbor.edge(row, column);
		}

		public Direction isDisruptiveTo()	{ return neighbor.isDisruptiveTo(); }
		public boolean isAlive()			{ return neighbor.isAlive(); }

		public boolean figureNextState(Cell n, Cell s, Cell e, Cell w, Cell ne, Cell nw, Cell se, Cell sw) { throw new UnsupportedOperationException(); }
		public boolean transition()									{ throw new UnsupportedOperationException(); }
		public void redraw(CellPainter p, Rectangle here, boolean all)	{ throw new UnsupportedOperationException(); }
		public void userClicked(Point here, Rectangle surface)		{ throw new UnsupportedOperationException(); }
		public int widthInCells()									{ throw new UnsupportedOperationException(); }
		public Cell create()										{ throw new UnsupportedOperationException(); }
		public void clear()											{ throw new UnsupportedOperationException(); }
		public boolean transfer(Storable memento, Point upperLeft, boolean load) { throw new UnsupportedOperationException(); }
		public Storable createMemento()								{ throw new UnsupportedOperationException(); }
	}
}

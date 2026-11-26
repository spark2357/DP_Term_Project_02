package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LifeGame의 Undo/Redo 테스트 (화면 없이, 가짜 TickSource)
 * 보드를 바꾸는 메서드(toggle, stamp, step, clear, load)는 바뀌기 전 스냅샷을 한 기록(BoardHistory)에 남김.
 * 보드가 그대로인 호출은 남기지 않음
 *
 *   메서드
 *   - undo() : 멈추고, 마지막 변경 전의 보드로 되돌림. 되돌릴 것이 없으면 false
 *   - redo() : 멈추고, Undo로 되돌린 변경을 다시 실행. 다시 실행할 것이 없으면 false
 *
 * [테스트 시나리오]
 *   1. 마우스 편집: 토글, 스탬프(덮어쓴 칸까지 복원)
 *   2. Go: Tick 3번을 Undo 3번으로 되돌림. 바뀌지 않은 세대는 기록하지 않음
 *   3. Grid: Clear, Load를 되돌림. 실패한 Load는 기록하지 않음
 *   3-1. 바뀌지 않은 편집: 빈 보드 Clear, 같은 자리에 같은 패턴 Stamp, 같은 내용 Load는 기록하지 않음
 *   4. 한 기록: 토글 → Tick → 스탬프를 Undo 3번으로 처음까지 되돌림. Undo 뒤 새로 편집하면 Redo 불가.
 *      기록은 최대 UNDO_LIMIT(100)개
 *   5. 실행 중 Undo: 자동으로 멈춤. 되돌릴 것이 없어도 멈춤
 *   6. 화면 알림: Undo, Redo 뒤에 보드 전체를 다시 그리라고 알림
 *
 * [커버리지]
 *   - 되돌린 뒤 진행해도 결과가 맞는지는 Board.restore()의 몫이라 BoardSnapshotTest에서 확인
 */
@Tag("unclassified")
class LifeGameUndoTest {
	private static final Set<Point> GLIDER = Cells.fromRowColumn("6,7", "7,8", "8,6", "8,7", "8,8");	// 블록 경계(7|8)에 걸침
	private static final Set<Point> BLOCK = Cells.fromRowColumn("20,20", "20,21", "21,20", "21,21");	// 바뀌지 않는 패턴

	private final FakeTicker ticker = new FakeTicker();
	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, ticker);

	//  1. 마우스 편집

	@Test
	@DisplayName("toggle(): Undo하면 토글 전 보드, 다시 Redo하면 토글한 보드")
	void undoAndRedoToggle() {
		// Given
		game.toggle(5, 5);

		// When, Then
		assertTrue(game.undo());
		assertEquals(Set.of(), board.liveCells());

		assertTrue(game.redo());
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	@Test
	@DisplayName("stamp(): 기존 셀 위에 찍은 뒤 Undo하면 덮어쓴 칸까지 원래대로 돌아온다")
	void undoStampRestoresOverwrittenCells() {
		// Given: (10,10)은 패턴 사각형 안의 죽은 칸이라 찍으면 지워짐
		place(Cells.fromRowColumn("10,10"));
		CellPattern glider = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));
		game.stamp(glider, 10, 10, CellColor.DEFAULT);
		assertFalse(board.liveCells().contains(new Point(10, 10)));

		// When
		game.undo();

		// Then
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
	}

	//  2. Go

	@Test
	@DisplayName("tick(): Tick 3번을 Undo 3번 하면 처음 보드로 돌아온다")
	void undoThreeTicks() {
		// Given
		place(GLIDER);
		game.tick();
		game.tick();
		game.tick();

		// When
		game.undo();
		game.undo();
		game.undo();

		// Then
		assertEquals(GLIDER, board.liveCells());
	}

	@Test
	@DisplayName("step(): 보드가 바뀌지 않은 세대는 기록하지 않는다 (고정 패턴을 실행해도 기록이 차지 않음)")
	void unchangedGenerationIsNotRecorded() {
		// Given: 바뀌지 않는 block
		place(BLOCK);

		// When
		for (int i = 0; i < 5; i++) {
			game.step();
		}

		// Then: 되돌릴 것이 없음
		assertFalse(game.undo());
	}

	//  3. Grid

	@Test
	@DisplayName("clear(): Undo하면 지우기 전 보드로 돌아온다")
	void undoClear() {
		// Given
		place(GLIDER);
		game.clear();

		// When
		game.undo();

		// Then
		assertEquals(GLIDER, board.liveCells());
	}

	@Test
	@DisplayName("load(): Undo하면 Load 전 보드로 돌아온다")
	void undoLoad() throws IOException {
		// Given
		place(GLIDER);
		game.load(new ByteArrayInputStream(new byte[0]), new FixedFormat(BLOCK));
		assertEquals(BLOCK, board.liveCells());

		// When
		game.undo();

		// Then
		assertEquals(GLIDER, board.liveCells());
	}

	@Test
	@DisplayName("load(): 읽기에 실패한 Load는 기록하지 않는다 (Undo하면 그 전의 편집이 되돌려짐)")
	void failedLoadIsNotRecorded() {
		// Given
		game.toggle(5, 5);

		// When
		assertThrows(IOException.class,
				() -> game.load(new ByteArrayInputStream(new byte[0]), new BrokenFormat()));

		// Then: 보드는 그대로이고, Undo는 토글을 되돌림
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
		game.undo();
		assertEquals(Set.of(), board.liveCells());
		assertFalse(game.undo());
	}

	//  3-1. 바뀌지 않은 편집

	@Test
	@DisplayName("clear(): 빈 보드를 Clear하면 기록하지 않는다")
	void clearingEmptyBoardIsNotRecorded() {
		// When
		game.clear();

		// Then: 되돌릴 것이 없음
		assertFalse(game.undo());
	}

	@Test
	@DisplayName("stamp(): 같은 자리에 같은 패턴을 다시 찍으면 두 번째는 기록하지 않는다 (Undo 한 번에 찍기 전 보드)")
	void sameStampTwiceIsRecordedOnce() {
		// Given
		CellPattern glider = new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2"));
		game.stamp(glider, 10, 10, CellColor.DEFAULT);

		// When
		game.stamp(glider, 10, 10, CellColor.DEFAULT);

		// Then
		assertTrue(game.undo());
		assertEquals(Set.of(), board.liveCells());
		assertFalse(game.undo());
	}

	@Test
	@DisplayName("load(): 지금 보드와 같은 내용을 Load하면 기록하지 않는다")
	void loadingSameBoardIsNotRecorded() throws IOException {
		// Given
		place(GLIDER);

		// When
		game.load(new ByteArrayInputStream(new byte[0]), new FixedFormat(GLIDER));

		// Then: 되돌릴 것이 없고 보드는 그대로
		assertFalse(game.undo());
		assertEquals(GLIDER, board.liveCells());
	}

	//  4. 한 기록

	@Test
	@DisplayName("undo(): 토글 → Tick → 스탬프를 Undo 3번 하면 처음 보드로 돌아온다 (마우스 편집과 Go가 한 기록)")
	void oneHistoryForEditsAndGenerations() {
		// Given
		place(GLIDER);
		Set<Point> start = board.liveCells();
		game.toggle(30, 30);
		game.tick();
		game.stamp(new CellPattern(2, 2, Cells.fromRowColumn("0,0", "1,1")), 40, 40, CellColor.DEFAULT);

		// When
		game.undo();
		game.undo();
		game.undo();

		// Then
		assertEquals(start, board.liveCells());
	}

	@Test
	@DisplayName("redo(): Undo한 뒤 새로 편집하면 Redo할 것이 없다")
	void newEditAfterUndoClearsRedo() {
		// Given
		game.toggle(1, 1);
		game.undo();

		// When
		game.toggle(2, 2);

		// Then
		assertFalse(game.redo());
		assertEquals(Cells.fromRowColumn("2,2"), board.liveCells());
	}

	@Test
	@DisplayName("undo(): 기록은 최대 UNDO_LIMIT개라, 그보다 한 번 더 편집하면 가장 오래된 편집은 되돌릴 수 없다")
	void keepsAtMostUndoLimitEdits() {
		// Given: (0,0)을 UNDO_LIMIT + 1번 토글. 마지막에 살아 있는 셀이 됨 (101은 홀수)
		for (int i = 0; i < LifeGame.UNDO_LIMIT + 1; i++) {
			game.toggle(0, 0);
		}

		// When, Then: UNDO_LIMIT번까지 되돌릴 수 있음
		for (int i = 0; i < LifeGame.UNDO_LIMIT; i++) {
			assertTrue(game.undo(), (i + 1) + "번째 Undo");
		}
		assertFalse(game.undo());

		// Then: 첫 토글은 되돌리지 못해 (0,0)이 살아 있는 채로 남음
		assertEquals(Cells.fromRowColumn("0,0"), board.liveCells());
	}

	//  5. 실행 중 Undo

	@Test
	@DisplayName("undo(): 실행 중에 Undo하면 멈춘 뒤 되돌린다")
	void undoWhileRunningStops() {
		// Given
		game.toggle(5, 5);
		game.start(30);

		// When
		game.undo();

		// Then
		assertFalse(ticker.running);
		assertEquals(Set.of(), board.liveCells());
	}

	@Test
	@DisplayName("undo(), redo(): 되돌릴 것이 없으면 false이고 보드는 그대로지만, 실행 중이었으면 멈춘다")
	void nothingToUndoStillStops() {
		// Given: 기록이 없는 게임을 실행 중
		place(GLIDER);
		game.start(30);

		// When, Then: Undo
		assertFalse(game.undo());
		assertFalse(ticker.running);
		assertEquals(GLIDER, board.liveCells());

		// When, Then: Redo
		game.start(30);
		assertFalse(game.redo());
		assertFalse(ticker.running);
		assertEquals(GLIDER, board.liveCells());
	}

	//  6. 화면 알림

	@Test
	@DisplayName("undo(), redo(): 되돌린 뒤 화면에 보드 전체를 다시 그리라고 알린다")
	void undoTellsViewToRedrawAll() {
		// Given
		List<String> events = new ArrayList<>();
		game.toggle(5, 5);
		game.addGameListener(new LifeGame.Listener() {
			public void boardChanged() {
				events.add("changed");
			}

			public void boardEdited() {
				events.add("edited");
			}
		});

		// When
		game.undo();
		game.redo();
		game.undo();
		game.undo();	// 되돌릴 것이 없으므로 알리지 않음

		// Then
		assertEquals(List.of("edited", "edited", "edited"), events);
	}

	// ---- 헬퍼 메서드

	// 기록에 남기지 않고 보드에 바로 놓음 (Given 준비용)
	private void place(Set<Point> cells) {
		for (Point p : cells) {
			board.toggle(p.y, p.x);
		}
	}

	// tick()이 오면 등록된 리스너(LifeGame)에 바로 전달하고, 실행 중인지 기억하는 가짜 시계
	private static class FakeTicker implements TickSource {
		private final List<Listener> listeners = new ArrayList<>();
		boolean running;

		public void startTicking(int millisecondsBetweenTicks) {
			running = millisecondsBetweenTicks > 0;
		}

		public void stop() {
			running = false;
		}

		public void tick() {
			for (Listener listener : listeners) {
				listener.tick();
			}
		}

		public void addTickListener(Listener listener) {
			listeners.add(listener);
		}
	}

	// 무엇을 읽든 정해 둔 셀을 돌려주는 형식
	private static class FixedFormat implements PatternFormat {
		private final Set<Point> cells;

		FixedFormat(Set<Point> cells) {
			this.cells = cells;
		}

		public LiveCells read(InputStream in) throws IOException {
			return LiveCells.of(cells);
		}

		public void write(LiveCells cells, OutputStream out) {
		}

		public String extension() {
			return "fixed";
		}

		public String description() {
			return "Fixed";
		}
	}

	// 항상 읽기에 실패하는 형식
	private static class BrokenFormat extends FixedFormat {
		BrokenFormat() {
			super(Set.of());
		}

		@Override
		public LiveCells read(InputStream in) throws IOException {
			throw new IOException("broken");
		}
	}
}

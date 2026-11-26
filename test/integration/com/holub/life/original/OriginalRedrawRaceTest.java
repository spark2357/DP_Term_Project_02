package com.holub.life.original;

import com.holub.life.support.Cells;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 원본 그리기 스레드 경합 테스트: 원본 사본(com.holub.life.original)에서 그리기(EDT)와 세대 계산(Clock 스레드)이 겹칠 때 생기는 문제를 재현
 * 원본은 Clock이 별도 스레드(java.util.Timer)에서 tick을 돌리고, 그리기는 EDT에서 하므로 둘이 겹칠 수 있음
 *
 *   겹치는 순간을 매번 똑같이 만드는 방법 (실제 시간에 기대지 않음)
 *   - 8x8 블록마다 HookBlock(Decorator)을 씌워, 정해 둔 블록을 그리기 직전·계산(transition) 직전에 테스트 코드(Hooks)를 끼워 넣음
 *   - 끼워 넣은 코드가 다른 스레드에서 한 세대를 진행하고 CountDownLatch로 기다림. 기다림에는 모두 시간 제한(5초)을 둠
 *   - 그린 결과는 BufferedImage에 그리고, 셀 가운데 픽셀이 빨강(원본 LIVE_COLOR)인지로 화면의 산 셀을 읽음
 *
 * [테스트 시나리오]
 *   1. 두 세대가 섞인 화면: 전체를 그리는 도중에 한 세대가 진행되어도 화면은 한 세대로만 그려져야 함.
 *      원본은 그리기 시작할 때만 잠금(readingPermitted)을 확인하므로, 위쪽 블록은 N세대, 아래쪽 블록은 N+1세대로 그림 (결함)
 *   2. 마지막 그리기 요청 소실: 막 안정된 블록을 그리러 왔을 때 transition() 중이면, 다음 그리기에서라도 그 블록이 그려져야 함.
 *      원본 redraw()는 oneLastRefreshRequired를 먼저 끄고 잠금을 보고 그냥 나가므로, 그 블록은 이전 모습으로 굳음 (결함)
 *
 * [커버리지]
 *   - OriginalRedrawRaceTest.java 테스트 대상: 원본 Neighborhood.redraw()와 transition()이 겹칠 때 그려지는 화면
 *   - 테스트 제외 대상
 *     - 파란 테두리 깜빡임, 실행 직후 회색 영역: 눈으로만 보이는 문제(테두리 색이 맞지 않는 순간, 메뉴가 닫힌 자리의 바탕색)라 자동 테스트로 만들지 않음
 *     - 클릭 소실, stop()이 진행 중인 tick을 기다리지 않음, Swing 스레드 밖 Swing 호출: 그리기가 아니라 Clock 스레드의 문제라 OriginalClockThreadTest에서 확인
 *     - 현재 코드에서 고쳐졌는지: tick이 EDT에서 실행되는지는 SwingTickTimerTest, Neighborhood에 static 잠금이 없는지는 ModelArchitectureTest에서 확인
 *   - 원본 Neighborhood의 잠금(readingPermitted, nestingLevel)은 static이라 모든 보드가 함께 씀.
 *     테스트가 실패해도 멈춰 둔 스레드를 finally에서 풀고 끝날 때까지 기다려, 다른 원본 테스트에 잠금 상태가 남지 않게 함
 */
@Tag("known-defect")
class OriginalRedrawRaceTest{
	private static final int CELL_PIXELS = 8;
	private static final Rectangle SCREEN = new Rectangle(0, 0, 64 * CELL_PIXELS, 64 * CELL_PIXELS);
	private static final long TIMEOUT_SECONDS = 5;

	@Test
	@DisplayName("redraw(): 그리는 도중에 한 세대가 진행되어도 화면은 한 세대로만 그려져야 한다 (결함)")
	void screenDrawnDuringGenerationShowsOneGeneration() throws Exception{
		// Given: 두 세대가 섞인 화면. 맨 위 블록 (0,0)과 맨 아래 블록 (7,7)에 가로 blinker. 원본은 위쪽은 N세대, 아래쪽은 N+1세대로 그림
		Set<Point> top = Cells.fromRowColumn("3,2", "3,3", "3,4");
		Set<Point> bottom = Cells.fromRowColumn("59,58", "59,59", "59,60");
		Hooks hooks = new Hooks();
		Neighborhood board = hookedBoard(hooks);
		place(board, top);
		place(board, bottom);
		Set<Point> generation0 = union(top, bottom);
		Set<Point> generation1 = union(Cells.fromRowColumn("2,3", "3,3", "4,3"), Cells.fromRowColumn("58,59", "59,59", "60,59"));

		// When: Universe.paint()처럼 전체를 그림. 가운데 줄 블록 (4,0)을 그리기 직전에 멈추고,
		// 그동안 다른 스레드(Clock 스레드 역할)가 한 세대를 진행함
		BufferedImage image = newScreen();
		Graphics g = image.getGraphics();
		hooks.beforeRedraw("(4,0)", () -> runOnOtherThreadAndWait(() -> tick(board)));
		try {
			board.redraw(g, new Rectangle(SCREEN), true);
		} finally {
			g.dispose();
		}

		// Then: 화면은 0세대 아니면 1세대여야 함. 원본은 위쪽 blinker는 가로(0세대), 아래쪽은 세로(1세대)로 그림
		Set<Point> screen = liveCellsOnScreen(image);
		assertTrue(screen.equals(generation0) || screen.equals(generation1),
				() -> "화면에 그려진 산 셀이 한 세대가 아님: " + screen);
	}

	@Test
	@DisplayName("redraw(): 막 안정된 블록을 그릴 때 계산 중이었어도, 다음 그리기에서는 그 블록이 그려져야 한다 (결함)")
	void blockThatJustBecameStableIsDrawnEvenIfRedrawMeetsTransition() throws Exception{
		// Given: 마지막 그리기 요청 소실. 블록 (1,1)에 1세대에 2x2 블록이 되어 멈추는 셀 3개, 블록 (0,0)에 계속 움직이는 blinker. 원본은 그 블록이 이전 모습으로 굳음
		Hooks hooks = new Hooks();
		Neighborhood board = hookedBoard(hooks);
		place(board, Cells.fromRowColumn("10,10", "10,11", "11,10"));
		place(board, Cells.fromRowColumn("3,2", "3,3", "3,4"));
		BufferedImage image = newScreen();
		redraw(board, image, true);

		// 1세대: (1,1) 블록이 2x2 블록으로 바뀜. 이 세대의 그리기 요청(invokeLater)은 EDT가 바빠 아직 실행되지 않음
		tick(board);
		// 2세대 계산: (1,1) 블록은 안정되어 oneLastRefreshRequired가 켜짐
		boolean willChange = board.figureNextState(
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		assertTrue(willChange, "blinker가 있으므로 2세대도 바뀌어야 함");

		// When: 밀려 있던 1세대 그리기(redraw(false))가 블록 (0,0)부터 그리기 시작한 순간,
		// 다른 스레드가 2세대 transition()에 들어가 잠금(readingPermitted)을 false로 둔 채 멈춤
		CountDownLatch inTransition = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		hooks.beforeTransition("(0,0)", () -> {
			inTransition.countDown();
			await(release, "transition()을 다시 진행하라는 신호");
		});
		Thread transition = new Thread(board::transition, "clock-thread");
		hooks.beforeRedraw("(0,0)", () -> {
			transition.start();
			await(inTransition, "다른 스레드가 transition()에 들어감");
		});
		try {
			redraw(board, image, false);
		} finally {
			release.countDown();
			transition.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
		}
		assertFalse(transition.isAlive(), "2세대 transition()이 끝나야 함");

		// 2세대 그리기 요청은 정상으로 실행됨
		redraw(board, image, false);

		// Then: 화면과 보드가 같아야 함. 원본은 (1,1) 블록이 0세대 모습(셀 3개)으로 남아 (11,11)이 빠짐
		assertEquals(liveCellsOnBoard(board), liveCellsOnScreen(image));
	}

	// ---- 헬퍼 메서드

	// Universe와 같은 64x64 보드를 만들되, 8x8 블록마다 HookBlock을 씌움
	private static Neighborhood hookedBoard(Hooks hooks){
		HookBlock prototype = new HookBlock(new Neighborhood(8, new Resident()), hooks, "prototype");
		return new Neighborhood(8, prototype);
	}

	private static void place(Neighborhood board, Set<Point> cells){
		for (Point cell : cells) {
			board.userClicked(new Point(cell.x * CELL_PIXELS + CELL_PIXELS / 2, cell.y * CELL_PIXELS + CELL_PIXELS / 2), new Rectangle(SCREEN));
		}
	}

	// Universe의 clock 리스너와 같은 순서로 한 세대 진행
	private static void tick(Neighborhood board){
		boolean willChange = board.figureNextState(
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		if (willChange) {
			board.transition();
		}
	}

	private static BufferedImage newScreen(){
		return new BufferedImage(SCREEN.width, SCREEN.height, BufferedImage.TYPE_INT_RGB);
	}

	private static void redraw(Neighborhood board, BufferedImage image, boolean drawAll){
		Graphics g = image.getGraphics();
		try {
			board.redraw(g, new Rectangle(SCREEN), drawAll);
		} finally {
			g.dispose();
		}
	}

	// 셀 가운데 픽셀이 원본 Resident의 LIVE_COLOR(빨강)이면 화면에 산 셀로 그려진 것
	private static Set<Point> liveCellsOnScreen(BufferedImage image){
		Set<Point> live = new HashSet<>();
		for (int row = 0; row < 64; row++) {
			for (int column = 0; column < 64; column++) {
				int rgb = image.getRGB(column * CELL_PIXELS + CELL_PIXELS / 2, row * CELL_PIXELS + CELL_PIXELS / 2);
				if (rgb == Color.RED.getRGB()) {
					live.add(new Point(column, row));
				}
			}
		}
		return live;
	}

	// OriginalDriver.liveCells()와 같은 방법으로 보드를 읽음
	private static Set<Point> liveCellsOnBoard(Neighborhood board){
		Cell.Memento memento = (Cell.Memento) board.createMemento();
		Set<Point> live = new HashSet<>();
		for (int row = 0; row < 64; row++) {
			for (int column = 0; column < 64; column++) {
				Point cell = new Point(column, row);
				if (memento.isAlive(cell)) {
					live.add(cell);
				}
			}
		}
		return live;
	}

	private static Set<Point> union(Set<Point> a, Set<Point> b){
		Set<Point> all = new HashSet<>(a);
		all.addAll(b);
		return all;
	}

	// 다른 스레드에서 action을 실행하고 끝날 때까지 기다림 (멈춘 채로 남지 않게 시간 제한)
	private static void runOnOtherThreadAndWait(Runnable action){
		CountDownLatch done = new CountDownLatch(1);
		Thread thread = new Thread(() -> {
			try {
				action.run();
			} finally {
				done.countDown();
			}
		}, "clock-thread");
		thread.start();
		await(done, "다른 스레드의 한 세대 진행");
	}

	private static void await(CountDownLatch latch, String what){
		try {
			if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new AssertionError(what + "을(를) " + TIMEOUT_SECONDS + "초 안에 기다리지 못함");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(what + "을(를) 기다리다 인터럽트됨", e);
		}
	}

	/**
	 * 블록 위치마다 한 번만 끼워 넣을 코드. 등록한 뒤 그 블록에서 한 번 실행되면 지워짐
	 *   - beforeRedraw     : 그 블록의 redraw() 직전
	 *   - beforeTransition : 그 블록의 transition() 직전 (바깥 Neighborhood가 잠금을 false로 바꾼 뒤임)
	 */
	private static class Hooks{
		private final AtomicReference<String> redrawBlock = new AtomicReference<>();
		private final AtomicReference<Runnable> redrawAction = new AtomicReference<>();
		private final AtomicReference<String> transitionBlock = new AtomicReference<>();
		private final AtomicReference<Runnable> transitionAction = new AtomicReference<>();

		void beforeRedraw(String block, Runnable action){
			redrawAction.set(action);
			redrawBlock.set(block);
		}

		void beforeTransition(String block, Runnable action){
			transitionAction.set(action);
			transitionBlock.set(block);
		}

		void redrawing(String block){
			runOnce(redrawBlock, redrawAction, block);
		}

		void transitioning(String block){
			runOnce(transitionBlock, transitionAction, block);
		}

		// AtomicReference.compareAndSet()은 ==로 비교하므로 블록 이름은 equals()로 따로 비교함
		private static void runOnce(AtomicReference<String> target, AtomicReference<Runnable> action, String block){
			String expected = target.get();
			if (block.equals(expected) && target.compareAndSet(expected, null)) {
				action.getAndSet(null).run();
			}
		}
	}

	/**
	 * 8x8 블록 하나를 감싸는 Cell (Decorator). redraw()와 transition() 직전에 Hooks에 알리는 것 말고는 그대로 위임
	 * 바깥 Neighborhood가 생성자에서 prototype.create()를 행 순서대로 64번 부르므로, 만든 순서로 블록 위치를 정함
	 */
	private static class HookBlock implements Cell{
		private final Cell block;
		private final Hooks hooks;
		private final String name;		// 블록 위치 "(행,열)"
		private int created = 0;		// 프로토타입이 지금까지 만든 블록 수

		HookBlock(Cell block, Hooks hooks, String name){
			this.block = block;
			this.hooks = hooks;
			this.name = name;
		}

		public void redraw(Graphics g, Rectangle here, boolean drawAll){
			hooks.redrawing(name);
			block.redraw(g, here, drawAll);
		}

		public boolean transition(){
			hooks.transitioning(name);
			return block.transition();
		}

		public Cell create(){
			String position = "(" + (created / 8) + "," + (created % 8) + ")";
			created++;
			return new HookBlock(block.create(), hooks, position);
		}

		public boolean figureNextState(Cell north, Cell south, Cell east, Cell west,
									   Cell northeast, Cell northwest, Cell southeast, Cell southwest){
			return block.figureNextState(north, south, east, west, northeast, northwest, southeast, southwest);
		}

		public Cell edge(int row, int column)						{ return block.edge(row, column); }
		public void userClicked(Point here, Rectangle surface)		{ block.userClicked(here, surface); }
		public boolean isAlive()									{ return block.isAlive(); }
		public int widthInCells()									{ return block.widthInCells(); }
		public Direction isDisruptiveTo()							{ return block.isDisruptiveTo(); }
		public void clear()											{ block.clear(); }
		public boolean transfer(Storable memento, Point upperLeft, boolean load) { return block.transfer(memento, upperLeft, load); }
		public Storable createMemento()								{ return block.createMemento(); }
	}
}

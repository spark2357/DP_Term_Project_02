package com.holub.life.original;

import com.holub.life.support.Cells;
import com.holub.life.support.MenuSiteSetup;
import com.holub.ui.MenuSite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 원본 Clock 스레드 결함 테스트: 원본 사본(com.holub.life.original)에서 Clock이 별도 스레드(java.util.Timer)로 tick을 돌릴 때 생기는 문제를 재현
 * 원본은 tick(세대 계산)을 Timer 스레드에서, 마우스 클릭과 메뉴 동작을 EDT에서 실행하고 둘 사이에 잠금이 없음
 *
 *   겹치는 순간을 매번 똑같이 만드는 방법 (실제 시간에 기대지 않음, OriginalRedrawRaceTest와 같은 방식)
 *   - 다른 스레드(Clock 스레드 역할)나 진짜 원본 Clock의 tick 리스너 안에서 CountDownLatch로 멈추고, 그동안 테스트 스레드(EDT 역할)가 할 일을 함
 *   - 기다림에는 모두 시간 제한(5초)을 둠
 *
 * [테스트 시나리오]
 *   1. 클릭 소실: tick이 다음 상태를 계산한 뒤 적용하기 전에 클릭한 셀은 살아 있어야 함.
 *      원본 Resident.transition()은 클릭을 모르고 계산해 둔 상태(willBeAlive)로 덮어써서 클릭이 사라짐 (결함)
 *   2. stop()이 진행 중인 tick을 기다리지 않음: stop()이 돌아왔을 때는 진행 중이던 tick이 끝나 있어야 함.
 *      원본 Clock.stop()은 TimerTask.cancel()만 부르고 바로 돌아오므로, Load가 stop() 뒤에 보드를 지우는 동안에도 tick이 보드를 바꿀 수 있음 (결함)
 *   3. Swing 스레드 밖 Swing 호출: 타이머가 부르는 tick은 EDT에서 실행되어야 함.
 *      원본 Clock.tick()은 Timer 스레드에서 menuIsActive()(MenuSelectionManager, Swing)를 부르고 리스너를 실행함 (결함)
 *
 * [커버리지]
 *   - OriginalClockThreadTest.java 테스트 대상: 원본 Clock.startTicking(), stop(), tick()의 스레드, 원본 Neighborhood·Resident의 클릭과 transition()이 겹칠 때
 *   - 테스트 제외 대상
 *     - 현재 코드에서 고쳐졌는지: tick이 EDT에서 실행되는지는 SwingTickTimerTest에서 확인.
 *       tick, 클릭, stop()이 모두 EDT에서 하나씩 실행되므로 1번과 2번이 생길 수 없음
 *     - 원본 Clock의 Go 메뉴(createMenus): GoMenuTest가 맡음. 여기서는 원본 Clock을 만들 때 생기는 Go 메뉴를 바로 지움
 *   - 원본 Clock은 생성자에서 MenuSite에 메뉴를 붙이므로 MenuSite가 필요함(JFrame). 화면이 없는 환경에서는 2번과 3번을 건너뜀
 *   - 원본 Clock은 싱글톤이라 리스너를 한 번만 붙이고 테스트마다 할 일(onTick)만 바꿈.
 *     원본 Clock의 Timer 스레드는 데몬 스레드가 아니므로 끝날 때 리플렉션으로 Timer를 취소함
 */
@Tag("known-defect")
class OriginalClockThreadTest{
	private static final int CELL_PIXELS = 8;
	private static final Rectangle SCREEN = new Rectangle(0, 0, 64 * CELL_PIXELS, 64 * CELL_PIXELS);
	private static final long TIMEOUT_SECONDS = 5;

	private static Clock clock;
	private static final AtomicReference<Runnable> onTick = new AtomicReference<>(() -> {
	});

	@Test
	@DisplayName("transition(): tick이 다음 상태를 계산한 뒤 적용하기 전에 클릭한 셀도 살아 있어야 한다 (결함)")
	void cellClickedDuringTickStaysAlive() throws Exception{
		// Given: 클릭 소실. 보드가 계속 바뀌도록 블록 (0,0)에 blinker. 클릭할 (20,20)은 죽은 셀
		Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
		for (Point cell : Cells.fromRowColumn("3,2", "3,3", "3,4")) {
			click(board, cell);
		}
		Point clicked = new Point(20, 20);

		// When: Clock 스레드가 figureNextState()를 끝내고 transition() 직전에 멈춘 사이에 EDT(테스트 스레드)가 클릭함
		CountDownLatch figured = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread clockThread = new Thread(() -> {
			boolean willChange = board.figureNextState(
					Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
					Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
			figured.countDown();
			await(release, "클릭이 끝났다는 신호");
			if (willChange) {
				board.transition();
			}
		}, "clock-thread");
		clockThread.start();
		try {
			await(figured, "Clock 스레드의 figureNextState()");
			click(board, clicked);
		} finally {
			release.countDown();
			clockThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
		}
		assertFalse(clockThread.isAlive(), "Clock 스레드의 transition()이 끝나야 함");

		// Then: 원본은 transition()이 계산해 둔 상태(죽음)로 덮어써서 (20,20)이 죽어 있음
		assertTrue(liveCells(board).contains(clicked), () -> "클릭한 셀이 사라짐: " + liveCells(board));
	}

	@Test
	@DisplayName("stop(): 돌아왔을 때는 진행 중이던 tick이 끝나 있어야 한다 (결함)")
	void stopReturnsAfterRunningTickFinishes() throws Exception{
		// Given: stop()이 진행 중인 tick을 기다리지 않음. tick 리스너가 실행 중에 멈춰 있는 원본 Clock
		Clock original = originalClock();
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean tickFinished = new AtomicBoolean(false);
		onTick.set(() -> {
			entered.countDown();
			await(release, "tick을 끝내라는 신호");
			tickFinished.set(true);
		});

		// When: tick이 실행 중일 때 다른 스레드(EDT 역할)에서 stop()을 부름
		AtomicReference<Boolean> tickFinishedWhenStopReturned = new AtomicReference<>();
		CountDownLatch stopReturned = new CountDownLatch(1);
		Thread stopper = new Thread(() -> {
			original.stop();
			tickFinishedWhenStopReturned.set(tickFinished.get());
			stopReturned.countDown();
		}, "edt-role");
		try {
			original.startTicking(1000);		// 첫 tick은 바로, 다음 tick은 1초 뒤
			await(entered, "원본 Clock의 첫 tick");
			stopper.start();
			// 원본 stop()은 바로 돌아옴. 진행 중인 tick을 기다리는 구현이면 여기서 시간이 다 지난 뒤에 tick을 풀어 줌
			stopReturned.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			stopper.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
			original.stop();
			onTick.set(() -> {
			});
		}

		// Then: 원본은 tick이 아직 멈춰 있는 동안 stop()이 돌아옴
		assertEquals(Boolean.TRUE, tickFinishedWhenStopReturned.get());
	}

	@Test
	@DisplayName("startTicking(): 타이머가 부르는 tick은 Swing 이벤트 스레드(EDT)에서 실행되어야 한다 (결함)")
	void timerTickRunsOnEventDispatchThread() throws Exception{
		// Given: Swing 스레드 밖 Swing 호출. 원본 Clock.tick()은 리스너를 부르기 전에 같은 스레드에서 menuIsActive()(Swing)를 부름
		Clock original = originalClock();
		AtomicReference<Boolean> firstTickOnEdt = new AtomicReference<>();
		CountDownLatch ticked = new CountDownLatch(1);
		onTick.set(() -> {
			firstTickOnEdt.compareAndSet(null, SwingUtilities.isEventDispatchThread());
			ticked.countDown();
		});

		// When
		try {
			original.startTicking(10);
			assertTrue(ticked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "tick이 " + TIMEOUT_SECONDS + "초 안에 와야 함");
		} finally {
			original.stop();
			onTick.set(() -> {
			});
		}

		// Then: 원본은 java.util.Timer 스레드에서 실행됨
		assertEquals(Boolean.TRUE, firstTickOnEdt.get());
	}

	// 원본 Timer 스레드는 데몬이 아니어서 JVM이 끝나지 않게 붙잡으므로 취소함
	@AfterAll
	static void cancelOriginalTimer() throws Exception{
		if (clock == null) {
			return;
		}
		clock.stop();
		Field timer = Clock.class.getDeclaredField("clock");
		timer.setAccessible(true);
		((Timer) timer.get(clock)).cancel();
	}

	// ---- 헬퍼 메서드

	// 원본 Clock 싱글톤. 생성자가 붙인 Go 메뉴는 다른 테스트(GoMenuTest)와 섞이지 않게 바로 지움
	private static synchronized Clock originalClock(){
		assumeFalse(MenuSiteSetup.isHeadless(), "원본 Clock은 생성자에서 MenuSite(JFrame)가 필요함");
		if (clock == null) {
			MenuSiteSetup.establishOnce();
			clock = Clock.instance();
			MenuSite.removeMyMenus(clock);
			clock.addClockListener(() -> onTick.get().run());
		}
		return clock;
	}

	// Universe의 마우스 클릭과 같음. 셀 가운데 픽셀로 userClicked()
	private static void click(Neighborhood board, Point cell){
		board.userClicked(new Point(cell.x * CELL_PIXELS + CELL_PIXELS / 2, cell.y * CELL_PIXELS + CELL_PIXELS / 2), new Rectangle(SCREEN));
	}

	// OriginalDriver.liveCells()와 같은 방법으로 보드를 읽음
	private static Set<Point> liveCells(Neighborhood board){
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
}

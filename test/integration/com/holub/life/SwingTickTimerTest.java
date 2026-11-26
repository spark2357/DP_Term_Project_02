package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * SwingTickTimer 통합 테스트: 실제 javax.swing.Timer로 tick이 Swing 이벤트 스레드(EDT)에서 실행되는지, 메뉴가 열려 있으면 tick을 건너뛰는지 확인
 * 원본은 Clock이 java.util.Timer의 별도 스레드에서 tick을 돌려, EDT의 그리기·클릭과 겹쳤음
 * tick이 EDT에서 실행되면 계산, 그리기, 클릭, 메뉴 동작이 한 줄로 하나씩 실행되므로 겹칠 수 없음
 *
 * [테스트 시나리오]
 *   1. Clock(SwingTickTimer)로 실행하면 tick 리스너가 EDT에서 불림 (결함: 원본은 별도 스레드라
 *      두 세대가 섞인 화면, 마지막 그리기 요청 소실, 클릭 소실, Swing 스레드 밖 Swing 호출이 생겼음)
 *   2. 메뉴가 열려 있는 동안 타이머가 울리면 tick을 건너뜀 (원본 Clock.menuIsActive()와 같은 동작)
 *
 *   메뉴가 열린 동안 타이머가 울렸다는 것을 실제 시간에 기대지 않고 아는 방법
 *   - 메뉴 열기, SwingTickTimer 시작(첫 실행은 바로), 확인용 타이머 시작(50ms 뒤 한 번)을 모두 EDT에서 차례로 함
 *   - Swing 타이머들은 한 스레드(TimerQueue)가 울릴 시각 순서대로 EDT에 넣으므로, 확인용 타이머가 실행될 때는
 *     먼저 울린 SwingTickTimer의 실행이 이미 끝나 있음. 확인용 타이머에서 tick 수를 읽고 메뉴를 닫음
 *   - 메뉴는 화면에 띄우지 않은 JMenuBar, JMenu를 MenuSelectionManager의 선택 경로에 넣어 "열린 상태"로 만듦 (JFrame이 필요 없음)
 *
 * [커버리지]
 *   - SwingTickTimerTest.java 테스트 대상: start() (Clock.startTicking()을 거쳐 실제 타이머로 실행), 타이머가 울릴 때의 열린 메뉴 검사
 *   - 테스트 제외 대상
 *     - 원본에서 겹칠 때 생기는 화면 문제(두 세대가 섞인 화면, 마지막 그리기 요청 소실): OriginalRedrawRaceTest에서 확인 (known-defect)
 *     - 원본 Clock 스레드의 클릭 소실, stop()이 진행 중인 tick을 기다리지 않음, Swing 스레드 밖 Swing 호출:
 *       OriginalClockThreadTest에서 확인 (known-defect)
 *     - 현재 코드의 stop()이 진행 중인 tick을 기다리는지: tick과 stop()(메뉴 Command)이 모두 EDT에서 하나씩 실행되므로
 *       stop()이 불리는 순간 진행 중인 tick이 있을 수 없음. 따로 확인할 동작이 없어 제외
 *     - Neighborhood에 그리기 잠금(static)이 남아 있지 않은지: ModelArchitectureTest에서 확인
 *     - 메뉴를 닫은 뒤 tick이 다시 오는지: 1번에서 타이머가 tick을 보내는 것을 확인하므로 제외
 *   - 실제 시간을 쓰므로 간격은 10ms로 짧게 두고, 기다림에는 시간 제한(5초)을 둠
 */
class SwingTickTimerTest{
	private static final long TIMEOUT_SECONDS = 5;

	@Test
	@Tag("improvement")
	@DisplayName("start(): tick은 Swing 이벤트 스레드(EDT)에서 실행되어 그리기와 겹치지 않는다 (결함)")
	void tickRunsOnEventDispatchThread() throws InterruptedException{
		// Given: 앱과 같이 SwingTickTimer로 만든 Clock. 원본은 별도 스레드라 두 세대가 섞인 화면이 생겼음
		Clock clock = new Clock(new SwingTickTimer());
		AtomicReference<Boolean> firstTickOnEdt = new AtomicReference<>();
		CountDownLatch ticked = new CountDownLatch(1);
		clock.addTickListener(() -> {
			firstTickOnEdt.compareAndSet(null, SwingUtilities.isEventDispatchThread());
			ticked.countDown();
		});

		// When
		try {
			clock.startTicking(10);
			assertTrue(ticked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "tick이 " + TIMEOUT_SECONDS + "초 안에 와야 함");
		} finally {
			clock.stop();
		}

		// Then
		assertEquals(Boolean.TRUE, firstTickOnEdt.get());
	}

	@Test
	@Tag("original")
	@DisplayName("start(): 메뉴가 열려 있는 동안에는 타이머가 울려도 tick을 건너뛴다")
	void tickIsSkippedWhileMenuIsOpen() throws Exception{
		// Given: tick 수를 세는 타이머와, 50ms 뒤에 그때까지의 tick 수를 읽고 메뉴를 닫는 확인용 타이머
		SwingTickTimer timer = new SwingTickTimer();
		AtomicInteger ticks = new AtomicInteger();
		AtomicInteger ticksWhileMenuOpen = new AtomicInteger(-1);
		CountDownLatch checked = new CountDownLatch(1);
		Timer checker = new Timer(50, event -> {
			ticksWhileMenuOpen.set(ticks.get());
			MenuSelectionManager.defaultManager().clearSelectedPath();
			checked.countDown();
		});
		checker.setRepeats(false);

		// When: 메뉴 바의 Go 메뉴를 연 채로 타이머를 시작함. 첫 실행은 바로 울림
		try {
			SwingUtilities.invokeAndWait(() -> {
				JMenuBar menuBar = new JMenuBar();
				JMenu go = new JMenu("Go");
				menuBar.add(go);
				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { menuBar, go });
				timer.start(10, ticks::incrementAndGet);
				checker.start();
			});
			assertTrue(checked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "확인용 타이머가 " + TIMEOUT_SECONDS + "초 안에 와야 함");
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				timer.stop();
				checker.stop();
				MenuSelectionManager.defaultManager().clearSelectedPath();
			});
		}

		// Then: 메뉴가 열려 있던 동안 tick은 한 번도 실행되지 않음
		assertEquals(0, ticksWhileMenuOpen.get());
	}
}

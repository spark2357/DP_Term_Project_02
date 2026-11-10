package com.holub.life.clock;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Clock 단위 테스트: 일정 간격으로 tick을 보내는 시계 (Singleton, Observer의 Subject)
 * 창은 띄우지 않고, 보드나 Universe 없이 Clock만 검증함
 *
 *   필드
 *   - clock     : java.util.Timer. tick을 일정 간격으로 실행
 *   - tick      : 지금 예약된 TimerTask. null이면 멈춘 상태
 *   - publisher : 리스너 목록 (com.holub.tools.Publisher)
 *   메서드
 *   - instance()                 : Singleton 접근. 처음 부를 때 private 생성자로 만듦
 *   - startTicking(ms)           : 이전 TimerTask를 취소하고, ms > 0이면 ms 간격으로 새로 예약 (0이면 멈춤)
 *   - stop()                     : startTicking(0). Go → Halt
 *   - tick()                     : 리스너에게 tick을 한 번 보냄. Go → Tick (Single Step). 메뉴가 열려 있으면 건너뜀
 *   - addClockListener(listener) : 리스너 등록
 *   - menuIsActive()             : 메뉴가 열려 있는지 Swing에 물어봄 (private)
 *
 * [테스트 시나리오]
 *   1. Single Step: tick() 한 번에 리스너가 각각 정확히 한 번 호출됨
 *   2. Halt: 시작 뒤에는 tick이 계속 오고, stop() 뒤에는 오지 않음. 멈춘 상태에서 Halt해도 아무 일 없음
 *   3. 속도 변경: 속도를 바꾸면 이전 속도의 타이머가 멈춤
 *   4. 메뉴가 열려 있을 때: tick이 미뤄지지 않고 사라짐 (원본 동작)
 *   5. 리스너 예외: 리스너 하나가 예외를 던지면 시계가 영구히 멈춤. 다른 리스너는 계속 tick을 받고 다시 시작할 수 있어야 함 (결함)
 *
 * [커버리지]
 *   - ClockTest.java 테스트 대상: startTicking, stop, tick, addClockListener, menuIsActive
 *   - 테스트 제외 대상
 *     - Go 메뉴: GoMenu가 만들므로 GoMenuTest, GoMenuInstallTest에서 확인
 *     - instance(): Singleton을 테스트끼리 공유하지 않도록 reflection으로 테스트마다 새 Clock을 만들어 씀
 *     - 속도별 실제 간격(500/150/70/30ms): 시간 측정이 컴퓨터 상태에 따라 흔들려서 "시작된다"까지만 확인
 *   - 시간이 걸리는 테스트는 실제 Timer를 쓰므로, 느린 컴퓨터에서도 실패하지 않게 기다리는 시간을 넉넉히 두고
 *     "정확히 몇 번"이 아니라 "오는가, 멈추는가"로 확인함
 */
class ClockTest {
	private Clock clock;

	@BeforeEach
	void setUp() throws Exception {
		clock = newClock();
	}

	@AfterEach
	void tearDown() {
		clock.stop();
		MenuSelectionManager.defaultManager().clearSelectedPath();
	}

	@Test
	@DisplayName("tick(): 한 번 부르면 등록된 리스너가 각각 정확히 한 번 호출된다 (Go → Tick)")
	void tickDeliversOnceToEachListener() {
		// Given
		CountingListener first = new CountingListener();
		CountingListener second = new CountingListener();
		clock.addClockListener(first);
		clock.addClockListener(second);

		// When
		clock.tick();

		// Then
		assertEquals(1, first.count());
		assertEquals(1, second.count());
	}

	@Test
	@DisplayName("startTicking(), stop(): 시작하면 tick이 계속 오고, Halt하면 더 이상 오지 않는다")
	void haltStopsTicking() throws InterruptedException {
		// Given: 20ms 간격으로 시작
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);
		clock.startTicking(20);
		assertTrue(waitForTicks(listener, 3), "시작한 뒤 tick이 와야 함");

		// When
		clock.stop();
		pause(100);		// 이미 실행 중이던 tick이 끝나기를 기다림

		// Then: 그 뒤로는 tick이 오지 않음
		int afterHalt = listener.count();
		pause(300);
		assertEquals(afterHalt, listener.count());
	}

	@Test
	@DisplayName("stop(), startTicking(0): 멈춘 상태에서 Halt해도 아무 일도 일어나지 않는다")
	void haltWhenStoppedDoesNothing() throws InterruptedException {
		// Given: 시작하지 않은 시계
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);

		// When
		clock.stop();
		clock.startTicking(0);
		pause(200);

		// Then: 예외 없이 tick도 오지 않음
		assertEquals(0, listener.count());
	}

	@Test
	@DisplayName("startTicking(): 속도를 바꾸면 이전 속도의 타이머는 멈춘다")
	void changingSpeedCancelsPreviousTimer() throws InterruptedException {
		// Given: 20ms 간격으로 시작
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);
		clock.startTicking(20);
		assertTrue(waitForTicks(listener, 3), "시작한 뒤 tick이 와야 함");

		// When: 아주 긴 간격으로 바꿈. 새 타이머는 바로 한 번 tick한 뒤 한참 동안 tick하지 않음
		clock.startTicking(1_000_000);
		pause(100);

		// Then: 이전 20ms 타이머가 남아 있다면 tick이 계속 왔을 것
		int afterChange = listener.count();
		pause(300);
		assertEquals(afterChange, listener.count());
	}

	@Test
	@DisplayName("tick() (원본 동작): 메뉴가 열려 있으면 tick이 미뤄지지 않고 사라진다. menuIsActive()로 Swing 상태에 의존")
	void tickIsDroppedWhileMenuIsOpen() {
		// Given: 메뉴가 열려 있는 상태를 만듦 (Swing의 메뉴 선택 경로가 비어 있지 않음)
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);
		MenuElement[] openMenu = { new JMenuItem("열린 메뉴") };
		MenuSelectionManager.defaultManager().setSelectedPath(openMenu);

		// When: 메뉴가 열린 동안 tick
		clock.tick();

		// Then: 리스너가 호출되지 않음
		assertEquals(0, listener.count(), "메뉴가 열린 동안");

		// When: 메뉴를 닫음
		MenuSelectionManager.defaultManager().clearSelectedPath();

		// Then: 건너뛴 tick은 나중에 전달되지도 않음. 다음 tick부터 다시 전달됨
		assertEquals(0, listener.count(), "메뉴를 닫은 직후");
		clock.tick();
		assertEquals(1, listener.count(), "메뉴를 닫은 뒤 tick");
	}

	@Test
	@Tag("known-defect")
	@DisplayName("startTicking() (결함): 리스너 하나가 예외를 던져도 시계가 멈추면 안 된다 (원본: Timer 스레드가 죽어 다른 리스너도 tick을 못 받고, 다시 시작할 수도 없음)")
	void listenerExceptionShouldNotStopClock() throws InterruptedException {
		// Given: 정상 리스너와 예외를 던지는 리스너. Publisher는 나중에 등록한 리스너부터 부르므로 예외 리스너가 먼저 호출됨
		CountingListener good = new CountingListener();
		clock.addClockListener(good);
		clock.addClockListener(new ThrowingListener());

		// When: 시작 (원본은 첫 tick에서 예외가 Timer 스레드를 죽임. 콘솔에 예외가 찍힘)
		clock.startTicking(20);
		pause(200);

		// Then: 다시 시작할 수 있어야 함. 원본은 Timer가 취소되어 IllegalStateException
		try {
			clock.startTicking(20);
		} catch (IllegalStateException e) {
			fail("리스너 예외로 Timer가 취소되어 다시 시작할 수 없음: " + e.getMessage());
		}

		// Then: 정상 리스너는 계속 tick을 받아야 함
		assertTrue(waitForTicks(good, 3), "예외를 던지지 않는 리스너는 tick을 계속 받아야 함");
	}

	// ---- 헬퍼

	// Singleton(Clock.instance())을 쓰면 테스트끼리 리스너와 타이머를 공유하므로, private 생성자를 직접 불러 새로 만듦
	private static Clock newClock() throws Exception {
		Constructor<Clock> constructor = Clock.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		return constructor.newInstance();
	}

	// 리스너가 atLeast번 이상 tick을 받을 때까지 최대 3초 기다림. 받으면 true
	private static boolean waitForTicks(CountingListener listener, int atLeast) throws InterruptedException {
		for (int waited = 0; waited < 3000; waited += 10) {
			if (listener.count() >= atLeast) {
				return true;
			}
			pause(10);
		}
		return false;
	}

	private static void pause(long milliseconds) throws InterruptedException {
		Thread.sleep(milliseconds);
	}

	// tick을 받은 횟수를 세는 리스너. tick은 Timer 스레드에서 오므로 여러 스레드에서 안전한 AtomicInteger를 씀
	private static class CountingListener implements Clock.Listener {
		private final AtomicInteger ticks = new AtomicInteger();

		public void tick() {
			ticks.incrementAndGet();
		}

		int count() {
			return ticks.get();
		}
	}

	// tick을 받으면 항상 예외를 던지는 리스너
	private static class ThrowingListener implements Clock.Listener {
		public void tick() {
			throw new RuntimeException("리스너 오류 (테스트용)");
		}
	}
}

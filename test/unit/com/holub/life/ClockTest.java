package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Clock 단위 테스트: tick을 리스너에게 보내는 시계 (Observer의 Subject, TickTimer Adapter의 Client)
 * 진짜 Swing Timer 대신 가짜 타이머(FakeTickTimer)를 넣어, 시간을 기다리지 않고 검증함
 * 원본 Clock이 리팩터링으로 구조가 많이 달라져(원본은 싱글톤이고 java.util.Timer를 직접 만들어 가짜 타이머를 넣을 수 없음), 원본과 비교하는 계약 테스트로 만들 수 없음 → 설계 개선(improvement)으로 분류
 *
 *   메서드
 *   - startTicking(ms) : ms > 0이면 타이머를 ms 간격으로 시작, 0이면 멈춤 (Go → 속도 메뉴, Halt)
 *   - stop()           : startTicking(0)
 *   - tick()           : 리스너에게 tick을 한 번 보냄 (Go → Tick)
 *   - addClockListener(listener) / addTickListener(listener) : 리스너 등록
 *
 * [테스트 시나리오]
 *   1. Single Step: tick() 한 번에 리스너가 각각 정확히 한 번 호출됨
 *   2. 시작: startTicking(ms)는 타이머를 그 간격으로 시작하고, 타이머가 울릴 때마다 tick이 감
 *   3. Halt: stop()하면 타이머가 멈추고 tick이 오지 않음. 멈춘 상태에서 Halt해도 아무 일 없음
 *   4. 속도 변경: 속도를 바꾸면 타이머가 새 간격으로 다시 시작됨
 *   (1~4 모두 설계 개선: Adapter)
 *
 * [커버리지]
 *   - ClockTest.java 테스트 대상: startTicking, stop, tick, addClockListener, addTickListener
 *   - 테스트 제외 대상
 *     - 메뉴가 열려 있으면 tick을 건너뛰는 동작: SwingTickTimer(Swing)로 옮겨져서 단위 테스트에서 제외
 *     - 실제 간격(500/150/70/30ms)대로 울리는지: 타이머 구현(SwingTickTimer)의 일이라 제외
 *     - Go 메뉴에서 속도 고르기: 통합 테스트 GoMenuTest, SetSpeedCommandTest에서 확인
 */
class ClockTest {
	private FakeTickTimer timer;
	private Clock clock;

	@BeforeEach
	void setUp() {
		timer = new FakeTickTimer();
		clock = new Clock(timer);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("tick(): 한 번 부르면 등록된 리스너가 각각 정확히 한 번 호출된다 (Go → Tick)")
	void tickDeliversOnceToEachListener() {
		// Given
		CountingListener first = new CountingListener();
		CountingListener second = new CountingListener();
		clock.addClockListener(first);
		clock.addTickListener(second);

		// When
		clock.tick();

		// Then
		assertEquals(1, first.count);
		assertEquals(1, second.count);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("startTicking(): 타이머를 그 간격으로 시작하고, 타이머가 울릴 때마다 tick이 간다")
	void startTickingStartsTimerAndDeliversTicks() {
		// Given
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);

		// When
		clock.startTicking(150);
		timer.fire();
		timer.fire();
		timer.fire();

		// Then
		assertTrue(timer.running);
		assertEquals(150, timer.millisecondsBetweenTicks);
		assertEquals(3, listener.count);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("stop(): Halt하면 타이머가 멈추고 더 이상 tick이 오지 않는다")
	void haltStopsTicking() {
		// Given: 실행 중인 시계
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);
		clock.startTicking(30);
		timer.fire();

		// When
		clock.stop();
		timer.fire();

		// Then: 멈추기 전의 한 번만 옴
		assertFalse(timer.running);
		assertEquals(1, listener.count);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("stop(), startTicking(0): 멈춘 상태에서 Halt해도 아무 일도 일어나지 않는다")
	void haltWhenStoppedDoesNothing() {
		// Given: 시작하지 않은 시계
		CountingListener listener = new CountingListener();
		clock.addClockListener(listener);

		// When
		clock.stop();
		clock.startTicking(0);
		timer.fire();

		// Then: 예외 없이 tick도 오지 않음
		assertFalse(timer.running);
		assertEquals(0, listener.count);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("startTicking(): 속도를 바꾸면 타이머가 새 간격으로 다시 시작된다")
	void changingSpeedRestartsTimerWithNewInterval() {
		// Given: Agonizing(500ms)로 실행 중
		clock.startTicking(500);

		// When: Fast(30ms)로 바꿈
		clock.startTicking(30);

		// Then
		assertTrue(timer.running);
		assertEquals(30, timer.millisecondsBetweenTicks);
	}

	// ---- 헬퍼

	// tick을 받은 횟수를 세는 리스너
	private static class CountingListener implements Clock.Listener {
		int count;

		@Override
		public void tick() {
			count++;
		}
	}
}

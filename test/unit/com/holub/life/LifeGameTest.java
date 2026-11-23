package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LifeGame 단위 테스트: 보드와 시계를 묶는 Facade. 보드가 바뀌면 구독자(화면)에게 알림 (Observer의 Subject)
 * 시계는 가짜(FakeTicker)로, 화면은 알림을 기록하는 가짜 리스너로 바꿔 화면·시간 없이 검증함
 *
 *   메서드
 *   - tick()             : 시계를 한 번 tick → 한 세대 진행 (Go → Tick)
 *   - start(ms), stop()  : 시계 시작·멈춤 (Go → 속도, Halt)
 *   - toggle(), clear(), stamp() : 보드를 바꾸고 boardEdited 알림
 *   - load(), store()    : 시계를 멈추고 보드를 읽기·쓰기
 *   - 알림: 세대 진행으로 바뀌면 boardChanged, 사용자가 바꾸면 boardEdited
 *
 * [테스트 시나리오]
 *   1. 진행: 시계가 tick하면 한 세대 진행하고 boardChanged를 알림. 바뀐 게 없으면 알리지 않음
 *   2. 사용자 편집: 클릭, Clear, Stamp, Load 뒤에는 boardEdited를 알림
 *   3. 시계: start/stop은 시계에 그대로 전달. Load와 Store는 시계를 먼저 멈춤
 *   4. Load 실패: IOException이 나고 보드와 알림은 그대로
 *
 * [커버리지]
 *   - LifeGameTest.java 테스트 대상: tick, start, stop, toggle, clear, stamp, load, store, liveCells, 알림
 *   - 테스트 제외 대상
 *     - 진짜 시계(Clock)와 타이머: ClockTest에서 확인
 *     - 메뉴에서 실행: Command 테스트에서 확인
 */
@Tag("original")
class LifeGameTest {
	private FakeTicker ticker;
	private LifeGame game;
	private RecordingListener view;

	@BeforeEach
	void setUp() {
		ticker = new FakeTicker();
		game = new LifeGame(new Board(), ticker);
		view = new RecordingListener();
		game.addGameListener(view);
	}

	@Test
	@DisplayName("tick(): 시계가 tick하면 한 세대 진행하고 boardChanged를 알린다")
	void tickAdvancesAndNotifiesChanged() {
		// Given: blinker
		game.toggle(3, 2);
		game.toggle(3, 3);
		game.toggle(3, 4);
		view.events.clear();

		// When
		game.tick();

		// Then
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), game.liveCells());
		assertEquals(List.of("changed"), view.events);
	}

	@Test
	@DisplayName("tick(): 바뀐 셀이 없으면 알리지 않는다")
	void tickWithoutChangeNotifiesNothing() {
		// When
		game.tick();

		// Then
		assertEquals(List.of(), view.events);
	}

	@Test
	@DisplayName("toggle(), clear(): 사용자가 보드를 바꾸면 boardEdited를 알린다")
	void userEditsNotifyEdited() {
		// When
		game.toggle(5, 5);
		game.clear();

		// Then
		assertEquals(List.of("edited", "edited"), view.events);
		assertTrue(game.liveCells().isEmpty());
	}

	@Test
	@DisplayName("stamp(): 패턴을 찍고 boardEdited를 알린다")
	void stampNotifiesEdited() {
		// Given
		CellPattern dot = new CellPattern(1, 1, Cells.fromRowColumn("0,0"));

		// When
		game.stamp(dot, 10, 20);

		// Then
		assertEquals(Cells.fromRowColumn("10,20"), game.liveCells());
		assertEquals(List.of("edited"), view.events);
	}

	@Test
	@DisplayName("start(), stop(): 시계에 그대로 전달한다")
	void startAndStopGoToTicker() {
		game.start(70);
		assertTrue(ticker.running);
		assertEquals(70, ticker.millisecondsBetweenTicks);

		game.stop();
		assertFalse(ticker.running);
	}

	@Test
	@DisplayName("load(): 시계를 멈추고, 읽은 패턴으로 보드를 바꾸고, boardEdited를 알린다")
	void loadStopsClockReplacesBoardAndNotifies() throws IOException {
		// Given: 실행 중
		game.start(30);

		// When
		game.load(rle("x = 1, y = 1\no!"), new RleFormat(LifeTestDriver.SIZE));

		// Then: 1x1 패턴은 보드 가운데(31, 31)
		assertFalse(ticker.running);
		assertEquals(Cells.fromRowColumn("31,31"), game.liveCells());
		assertEquals(List.of("edited"), view.events);
	}

	@Test
	@DisplayName("load(): 읽다가 실패하면 IOException이 나고, 보드는 그대로이며 알리지 않는다")
	void failedLoadKeepsBoardAndNotifiesNothing() {
		// Given
		game.toggle(1, 1);
		view.events.clear();

		// When, Then
		assertThrows(IOException.class, () -> game.load(rle("not rle"), new RleFormat(LifeTestDriver.SIZE)));
		assertEquals(Cells.fromRowColumn("1,1"), game.liveCells());
		assertEquals(List.of(), view.events);
	}

	@Test
	@DisplayName("store(): 시계를 멈추고 보드를 쓴다. 보드가 바뀌지 않으니 알리지 않는다")
	void storeStopsClockAndWrites() throws IOException {
		// Given
		game.toggle(1, 1);
		game.start(30);
		view.events.clear();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		// When
		game.store(out, new RleFormat(LifeTestDriver.SIZE));

		// Then
		assertFalse(ticker.running);
		assertTrue(out.size() > 0);
		assertEquals(List.of(), view.events);
	}

	// ---- 헬퍼 메서드

	private static ByteArrayInputStream rle(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	// 받은 알림을 순서대로 기록하는 가짜 화면
	private static class RecordingListener implements LifeGame.Listener {
		final List<String> events = new ArrayList<>();

		@Override
		public void boardChanged() {
			events.add("changed");
		}

		@Override
		public void boardEdited() {
			events.add("edited");
		}
	}
}

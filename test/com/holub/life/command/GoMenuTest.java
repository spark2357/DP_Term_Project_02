package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.support.*;
import com.holub.ui.MenuSite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Go 메뉴 테스트: Halt, Tick, 속도 Command와 메뉴 등록 (가짜 TickSource)
 *
 * [테스트 시나리오]
 *   1. Command: Halt는 멈추고, Tick은 한 번 tick하고, 속도 Command는 정해진 간격으로 시작함
 *   2. 메뉴: 항목 이름과 순서가 원본과 같고(원래 기능 보존), 항목마다 단축키(Space, T, A, S, D, F)가 붙고,
 *      누르면 짝지은 Command가 실행됨
 *
 * [커버리지]
 *   - 2는 MenuSite가 필요해 JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 *   - 실제 키 입력이 메뉴로 전달되는 것은 Swing이 맡으므로 단축키가 붙었는지까지만 확인함
 *   - 속도별 실제 간격은 ClockTest(시계) 범위라 여기서는 넘긴 값만 확인함
 */
class GoMenuTest {
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private GoMenu menu;

	@AfterEach
	void removeMenu() {
		if (menu != null) {
			MenuSite.removeMyMenus(menu);
		}
	}

	//  1. Command

	@Test
	@DisplayName("HaltCommand, TickCommand, SetSpeedCommand: 멈추고, 한 번 tick하고, 정해진 간격으로 시작한다")
	void goCommands() {
		// When, Then
		new SetSpeedCommand(game, 150).execute();
		assertEquals(List.of("start 150"), ticker.calls);

		new HaltCommand(game).execute();
		assertEquals(List.of("start 150", "stop"), ticker.calls);

		new TickCommand(game).execute();
		assertEquals(List.of("start 150", "stop", "tick"), ticker.calls);
	}

	@Test
	@DisplayName("SetSpeedCommand: 간격이 0 이하이면 IllegalArgumentException")
	void speedMustBePositive() {
		assertThrows(IllegalArgumentException.class, () -> new SetSpeedCommand(game, 0));
	}

	//  2. 메뉴

	@Test
	@Tag("gui")
	@DisplayName("register(): 원본과 같은 이름·순서로 등록하고, 단축키(Space, T, A, S, D, F)를 누르면 짝지은 동작을 한다")
	void registersItemsWithShortcuts() {
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given
		MenuSiteSetup.establishOnce();
		menu = new GoMenu(game);
		menu.register();

		// Then: 이름과 순서 (원본 Clock.createMenus와 같음)
		List<String> names = new ArrayList<>();
		for (Component item : ((JMenu) MenuSite.getMyMenuItem(menu, "Go", null)).getMenuComponents()) {
			names.add(((JMenuItem) item).getName());
		}
		assertEquals(List.of("Halt", "Tick (Single Step)", "Agonizing", "Slow", "Medium", "Fast"), names);

		// Then: 단축키와 동작
		assertItem("Halt", KeyEvent.VK_SPACE, "stop");
		assertItem("Tick (Single Step)", KeyEvent.VK_T, "tick");
		assertItem("Agonizing", KeyEvent.VK_A, "start 500");
		assertItem("Slow", KeyEvent.VK_S, "start 150");
		assertItem("Medium", KeyEvent.VK_D, "start 70");
		assertItem("Fast", KeyEvent.VK_F, "start 30");
	}

	// ---- 헬퍼 메서드

	// 항목의 단축키가 수식키 없는 key이고, 누르면 시계에 expectedCall이 한 번 전달되는지 확인
	private void assertItem(String name, int key, String expectedCall) {
		JMenuItem item = MenuSite.getMyMenuItem(menu, "Go", name);
		assertEquals(KeyStroke.getKeyStroke(key, 0), item.getAccelerator(), name + " 단축키");

		ticker.calls.clear();
		item.doClick();
		assertEquals(List.of(expectedCall), ticker.calls, name + " 동작");
	}

	// 받은 호출을 기록하는 가짜 시계
	private static class FakeTicker implements TickSource {
		final List<String> calls = new ArrayList<>();

		public void startTicking(int millisecondsBetweenTicks) {
			calls.add("start " + millisecondsBetweenTicks);
		}

		public void stop() {
			calls.add("stop");
		}

		public void tick() {
			calls.add("tick");
		}

		public void addTickListener(Listener listener) {
		}
	}
}

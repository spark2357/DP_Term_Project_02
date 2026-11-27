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
import java.util.stream.Stream;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Go 메뉴 통합 테스트: Go 메뉴에 등록한 항목과, 항목을 눌렀을 때의 동작
 * 진짜 LifeGame·Board와 MenuSite에 가짜 시계(FakeTicker)를 넣어 시간 없이 실행함
 * 원본 Clock이 리팩터링으로 구조가 많이 달라지고 원본 Go 메뉴는 테스트하기 까다로워, 원본과 비교하는 계약 테스트로 만들 수 없음 → 설계 개선(improvement)으로 분류
 *
 * [테스트 시나리오]
 *   1. 이름과 순서: Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast (원본 Clock.createMenus와 같음) (설계 개선: Command)
 *   2. 누르면: Halt는 멈추고, Tick은 한 세대만 진행하고, 속도 항목은 500·150·70·30ms 간격으로 시작함 (원본과 같음) (설계 개선: Command)
 *   3. 단축키: 항목마다 수식키 없는 키 하나(Space, T, A, S, D, F)가 붙음 (원본에는 단축키가 없음) (기능 확장)
 *
 * [커버리지]
 *   - GoMenuTest.java 테스트 대상: register
 *   - 테스트 제외 대상
 *     - MenuSite: Swing 메뉴 바라 메뉴 등록 결과로만 확인함. JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 *     - 실제 키 입력이 메뉴로 전달되는 것: Swing이 맡으므로 단축키가 붙었는지까지만 확인함
 *     - 속도별 실제 간격: 타이머(SwingTickTimer)의 일이라 넘긴 값만 확인함
 *     - 0 이하의 간격: 메뉴가 정해진 4개 속도로만 만들어 사용자가 넣을 수 없어 테스트하지 않음
 */
class GoMenuTest {
	private final FakeTicker ticker = new FakeTicker();
	private final LifeGame game = new LifeGame(new Board(), ticker);
	private GoMenu menu;

	@BeforeEach
	void registerMenu(){
		assumeFalse(MenuSiteSetup.isHeadless());
		MenuSiteSetup.establishOnce();
		menu = new GoMenu(game);
		menu.register();
	}

	@AfterEach
	void removeMenu() {
		if (menu != null) {
			MenuSite.removeMyMenus(menu);
		}
	}

	//  1. 이름과 순서

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("register(): 원본과 같은 이름과 순서로 항목을 등록한다")
	void registersItemsWithOriginalNamesInOrder(){
		// When
		List<String> names = new ArrayList<>();
		for (Component item : ((JMenu) MenuSite.getMyMenuItem(menu, "Go", null)).getMenuComponents()) {
			names.add(((JMenuItem) item).getName());
		}

		// Then
		assertEquals(List.of("Halt", "Tick (Single Step)", "Agonizing", "Slow", "Medium", "Fast"), names);
	}

	//  2. 누르면

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("register(): Halt를 누르면 실행 중인 게임이 멈춘다")
	void clickingHaltStopsGame(){
		// Given
		game.start(70);

		// When
		item("Halt").doClick();

		// Then
		assertFalse(ticker.running);
	}

	@Test
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@DisplayName("register(): Tick (Single Step)을 누르면 한 세대만 진행하고 시계는 시작하지 않는다")
	void clickingTickAdvancesOneGeneration(){
		// Given: blinker
		game.toggle(3, 2);
		game.toggle(3, 3);
		game.toggle(3, 4);

		// When
		item("Tick (Single Step)").doClick();

		// Then
		assertEquals(Cells.fromRowColumn("2,3", "3,3", "4,3"), game.liveCells());
		assertFalse(ticker.running);
	}

	@ParameterizedTest(name = "{0}: {1}ms")
	// [수정] 2026-11-27: 원본 Clock 구조가 달라 원본과 비교할 수 없어 original → improvement
	@Tag("improvement")
	@CsvSource({ "Agonizing, 500", "Slow, 150", "Medium, 70", "Fast, 30" })
	@DisplayName("register(): 속도 항목을 누르면 그 항목의 간격으로 게임을 시작한다")
	void clickingSpeedStartsGameAtItsInterval(String name, int milliseconds){
		// When
		item(name).doClick();

		// Then
		assertTrue(ticker.running);
		assertEquals(milliseconds, ticker.millisecondsBetweenTicks);
	}

	//  3. 단축키

	@ParameterizedTest(name = "{0}")
	@Tag("extension")
	@MethodSource("shortcuts")
	@DisplayName("register(): 항목마다 수식키 없는 단축키를 붙인다")
	void eachItemHasShortcutWithoutModifier(String name, int key){
		// When
		KeyStroke shortcut = item(name).getAccelerator();

		// Then
		assertEquals(KeyStroke.getKeyStroke(key, 0), shortcut);
	}

	static Stream<Arguments> shortcuts(){
		return Stream.of(
				Arguments.of("Halt", KeyEvent.VK_SPACE),
				Arguments.of("Tick (Single Step)", KeyEvent.VK_T),
				Arguments.of("Agonizing", KeyEvent.VK_A),
				Arguments.of("Slow", KeyEvent.VK_S),
				Arguments.of("Medium", KeyEvent.VK_D),
				Arguments.of("Fast", KeyEvent.VK_F));
	}

	// ---- 헬퍼 메서드

	private JMenuItem item(String name){
		return MenuSite.getMyMenuItem(menu, "Go", name);
	}
}

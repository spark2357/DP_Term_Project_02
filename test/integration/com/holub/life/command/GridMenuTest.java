package com.holub.life.command;

import com.holub.life.*;
import com.holub.life.mouse.*;
import com.holub.life.support.*;
import com.holub.ui.MenuSite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Grid 메뉴 통합 테스트: Grid 메뉴에 등록한 항목의 이름과 순서
 * 진짜 LifeGame·Board와 MenuSite에 가짜 시계(FakeTicker)와 가짜 파일 창(FakeDialogs)을 넣어 실행함
 *
 * [테스트 시나리오]
 *   1. 원본 항목: Stamp를 빼면 Clear, Load, Store, Exit 순서 (원본 Universe 생성자와 같음)
 *   2. Stamp 위치: 새 항목 Stamp는 Store와 Exit 사이
 *
 * [커버리지]
 *   - GridMenuTest.java 테스트 대상: register
 *   - 테스트 제외 대상
 *     - MenuSite: Swing 메뉴 바라 메뉴 등록 결과로만 확인함. JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 *     - 항목을 눌렀을 때의 동작: 각 Command 테스트(ClearCommandTest, LoadCommandTest, StoreCommandTest, GridCommandTest 등)에서 확인
 *     - Exit 항목의 동작: System.exit을 부름
 */
class GridMenuTest{
	private final LifeGame game = new LifeGame(new Board(), new FakeTicker());
	private final RleFormat rle = new RleFormat(LifeTestDriver.SIZE);
	private final PatternFormats formats = new PatternFormats(List.of(new LifeFormatAdapter(LifeTestDriver.SIZE), rle));
	private final Pen pen = new Pen(CellColor.DEFAULT);
	private final MouseController mouse = new MouseController(new BrushMode(game, pen));
	private GridMenu menu;

	@BeforeEach
	void registerMenu(){
		assumeFalse(MenuSiteSetup.isHeadless());
		MenuSiteSetup.establishOnce();
		menu = new GridMenu(game, formats, rle, mouse, pen, new FakeDialogs());
		menu.register();
	}

	@AfterEach
	void removeMenu(){
		if (menu != null) {
			MenuSite.removeMyMenus(menu);
		}
	}

	@Test
	@Tag("original")
	@DisplayName("register(): Stamp를 빼면 원본과 같은 이름과 순서로 항목을 등록한다")
	void registersOriginalItemsInOriginalOrder(){
		// When
		List<String> names = itemNames();
		names.remove("Stamp");

		// Then
		assertEquals(List.of("Clear", "Load", "Store", "Exit"), names);
	}

	@Test
	@Tag("extension")
	@DisplayName("register(): Stamp 항목을 Store와 Exit 사이에 등록한다")
	void registersStampBetweenStoreAndExit(){
		// When
		List<String> names = itemNames();

		// Then
		assertEquals(List.of("Clear", "Load", "Store", "Stamp", "Exit"), names);
	}

	// ---- 헬퍼 메서드

	private List<String> itemNames(){
		List<String> names = new ArrayList<>();
		for (Component item : ((JMenu) MenuSite.getMyMenuItem(menu, "Grid", null)).getMenuComponents()) {
			names.add(((JMenuItem) item).getName());
		}
		return names;
	}
}

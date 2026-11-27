package com.holub.life.command;

import com.holub.life.support.*;
import com.holub.ui.MenuSite;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.game.LifeGame;
import com.holub.life.model.Board;

/**
 * Edit 메뉴 테스트: Undo, Redo Command와 메뉴 등록 (가짜 TickSource)
 *
 * [테스트 시나리오]
 *   1. Command: 실행 중에 UndoCommand를 실행하면 멈추고 되돌림. RedoCommand를 실행하면 멈추고 다시 실행함
 *   2. 메뉴: Edit 메뉴에 Undo, Redo가 운영체제의 메뉴 단축키(Ctrl 또는 Cmd)+Z, +Shift+Z로 등록됨.
 *      Undo 항목을 누르면 되돌리고, Redo 항목을 누르면 다시 실행함
 *
 * [커버리지]
 *   - EditMenuTest.java 테스트 대상: UndoCommand.execute, RedoCommand.execute, EditMenu.register
 *   - 테스트 제외 대상
 *     - 실제 키 입력이 메뉴로 전달되는 것: Swing이 맡으므로 단축키가 붙었는지까지만 확인함
 *     - 되돌릴 것이 없을 때, 기록 한도 등 Undo/Redo의 세부: LifeGameUndoTest에서 확인
 *   - 2는 MenuSite가 필요해 JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 */
@Tag("extension")
class EditMenuTest {
	private final FakeTicker ticker = new FakeTicker();
	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, ticker);
	private EditMenu menu;

	@AfterEach
	void removeMenu() {
		if (menu != null) {
			MenuSite.removeMyMenus(menu);
		}
	}

	//  1. Command

	@Test
	@DisplayName("execute(): 실행 중에 UndoCommand를 실행하면 멈추고 마지막 변경을 되돌린다")
	void undoCommandStopsAndRevertsLastEdit(){
		// Given
		game.toggle(5, 5);
		game.start(30);

		// When
		new UndoCommand(game).execute();

		// Then
		assertFalse(ticker.running);
		assertEquals(Set.of(), board.liveCells());
	}

	@Test
	@DisplayName("execute(): 실행 중에 RedoCommand를 실행하면 멈추고 되돌린 변경을 다시 실행한다")
	void redoCommandStopsAndReappliesUndoneEdit(){
		// Given
		game.toggle(5, 5);
		game.undo();
		game.start(30);

		// When
		new RedoCommand(game).execute();

		// Then
		assertFalse(ticker.running);
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	//  2. 메뉴

	@Test
	@DisplayName("register(): Edit 메뉴에 Undo는 메뉴 단축키+Z, Redo는 메뉴 단축키+Shift+Z로 등록한다")
	void registersUndoAndRedoWithShortcuts() {
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given
		int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

		// When
		registerMenu();

		// Then
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut), menuItem("Undo").getAccelerator());
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK), menuItem("Redo").getAccelerator());
	}

	@Test
	@DisplayName("register(): 등록한 Undo 메뉴 항목을 누르면 마지막 변경을 되돌린다")
	void undoMenuItemRevertsLastEdit(){
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given
		registerMenu();
		game.toggle(5, 5);

		// When
		menuItem("Undo").doClick();

		// Then
		assertEquals(Set.of(), board.liveCells());
	}

	@Test
	@DisplayName("register(): 등록한 Redo 메뉴 항목을 누르면 되돌린 변경을 다시 실행한다")
	void redoMenuItemReappliesUndoneEdit(){
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given
		registerMenu();
		game.toggle(5, 5);
		game.undo();

		// When
		menuItem("Redo").doClick();

		// Then
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	// [수정] 2026-11-27: 메뉴 테스트를 나누면서 등록과 항목 찾기를 헬퍼로 뺌
	private void registerMenu(){
		MenuSiteSetup.establishOnce();
		menu = new EditMenu(game);
		menu.register();
	}

	private JMenuItem menuItem(String name){
		return MenuSite.getMyMenuItem(menu, "Edit", name);
	}
}

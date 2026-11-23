package com.holub.life.command;

import com.holub.life.*;
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

/**
 * Edit 메뉴 테스트: Undo, Redo Command와 메뉴 등록 (가짜 TickSource)
 *
 * [테스트 시나리오]
 *   1. Command: UndoCommand, RedoCommand가 게임을 멈추고 되돌리거나 다시 실행함
 *   2. 메뉴: Edit 메뉴에 Undo, Redo가 운영체제의 메뉴 단축키(Ctrl 또는 Cmd)+Z, +Shift+Z로 등록되고, 누르면 동작함
 *
 * [커버리지]
 *   - 2는 MenuSite가 필요해 JFrame을 만들므로(화면에 띄우지는 않음) 화면이 없는 환경에서는 건너뜀
 *   - 실제 키 입력이 메뉴로 전달되는 것은 Swing이 맡으므로 단축키가 붙었는지까지만 확인함
 */
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
	@DisplayName("UndoCommand, RedoCommand: 실행 중이면 멈추고 마지막 변경을 되돌리거나 다시 실행한다")
	void undoAndRedoCommands() {
		// Given
		game.toggle(5, 5);
		game.start(30);

		// When, Then: Undo
		new UndoCommand(game).execute();
		assertFalse(ticker.running);
		assertEquals(Set.of(), board.liveCells());

		// When, Then: Redo
		game.start(30);
		new RedoCommand(game).execute();
		assertFalse(ticker.running);
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	//  2. 메뉴

	@Test
	@Tag("gui")
	@DisplayName("register(): Edit 메뉴에 Undo(메뉴 단축키+Z), Redo(메뉴 단축키+Shift+Z)를 등록하고, 누르면 되돌리고 다시 실행한다")
	void registersUndoAndRedoWithShortcuts() {
		assumeFalse(MenuSiteSetup.isHeadless());

		// Given
		MenuSiteSetup.establishOnce();
		menu = new EditMenu(game);
		menu.register();
		int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		JMenuItem undo = MenuSite.getMyMenuItem(menu, "Edit", "Undo");
		JMenuItem redo = MenuSite.getMyMenuItem(menu, "Edit", "Redo");

		// Then: 단축키
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut), undo.getAccelerator());
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK), redo.getAccelerator());

		// When, Then: 메뉴 항목을 누름
		game.toggle(5, 5);
		undo.doClick();
		assertEquals(Set.of(), board.liveCells());
		redo.doClick();
		assertEquals(Cells.fromRowColumn("5,5"), board.liveCells());
	}

	// 실행 중인지만 기억하는 가짜 시계
	private static class FakeTicker implements TickSource {
		boolean running;

		public void startTicking(int millisecondsBetweenTicks) {
			running = millisecondsBetweenTicks > 0;
		}

		public void stop() {
			running = false;
		}

		public void tick() {
		}

		public void addTickListener(Listener listener) {
		}
	}
}

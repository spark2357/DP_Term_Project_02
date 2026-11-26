package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.ui.MenuSite;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * Edit 메뉴: Undo, Redo를 Command로 등록하고 단축키를 붙임 (GoMenu, GridMenu와 같은 방식)
 * 단축키는 운영체제의 메뉴 단축키를 따름. Windows는 Ctrl+Z / Ctrl+Shift+Z, macOS는 Cmd+Z / Cmd+Shift+Z
 */
public class EditMenu {
    private final LifeGame game;

    public EditMenu(LifeGame game) {
        this.game = game;
    }

    public void register() {
        int shortcutKeyOS = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();     // Windows: Ctrl, macOS: Cmd
        add("Undo", new UndoCommand(game), KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutKeyOS));
        add("Redo", new RedoCommand(game), KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutKeyOS | InputEvent.SHIFT_DOWN_MASK));
    }

    private void add(String name, Command command, KeyStroke key) {
        MenuSite.addLine(this, "Edit", name, event -> command.execute());
        MenuSite.getMyMenuItem(this, "Edit", name).setAccelerator(key);
    }
}

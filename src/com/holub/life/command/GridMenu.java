package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.life.PatternFormats;
import com.holub.life.RleFormat;
import com.holub.life.mouse.MouseController;
import com.holub.life.mouse.Pen;
import com.holub.ui.MenuSite;

/**
 * Grid 메뉴: 항목마다 Command를 하나씩 만들어 MenuSite에 등록함 (GoMenu와 같은 방식)
 * 원본은 Universe 생성자 안에서 익명 리스너로 등록했음
 */
public class GridMenu {
    private final LifeGame game;
    private final PatternFormats formats;
    private final RleFormat rle;
    private final MouseController mouse;
    private final Pen pen;
    private final Dialogs dialogs;

    public GridMenu(LifeGame game, PatternFormats formats, RleFormat rle, MouseController mouse, Pen pen, Dialogs dialogs) {
        this.game = game;
        this.formats = formats;
        this.rle = rle;
        this.mouse = mouse;
        this.pen = pen;
        this.dialogs = dialogs;
    }

    public void register() {
        add("Clear", new ClearCommand(game));
        add("Load", new LoadCommand(game, formats, dialogs));
        add("Store", new StoreCommand(game, formats, dialogs));
        add("Stamp", new StampCommand(game, rle, mouse, pen, dialogs));
        add("Exit", new ExitCommand());
    }

    private void add(String name, Command command) {
        MenuSite.addLine(this, "Grid", name, event -> command.execute());
    }
}

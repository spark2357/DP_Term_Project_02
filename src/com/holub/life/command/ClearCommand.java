package com.holub.life.command;

import com.holub.life.LifeGame;

/**
 * Grid → Clear: 모든 셀을 죽임
 */
public class ClearCommand implements Command {
    private final LifeGame game;

    public ClearCommand(LifeGame game) {
        this.game = game;
    }

    @Override
    public void execute() {
        game.clear();
    }
}

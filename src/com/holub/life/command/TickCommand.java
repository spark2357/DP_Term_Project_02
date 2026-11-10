package com.holub.life.command;

import com.holub.life.LifeGame;

/**
 * Go → Tick (Single Step): 게임을 시작하지 않고 한 세대만 진행
 */
public class TickCommand implements Command {
    private final LifeGame game;

    public TickCommand(LifeGame game) {
        this.game = game;
    }

    @Override
    public void execute() {
        game.tick();
    }
}

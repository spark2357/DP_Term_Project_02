package com.holub.life.command;

import com.holub.life.LifeGame;

/**
 * Go → Halt: 게임을 멈춤
 */
public class HaltCommand implements Command {
    private final LifeGame game;

    public HaltCommand(LifeGame game) {
        this.game = game;
    }

    @Override
    public void execute() {
        game.stop();
    }
}

package com.holub.life.command;

import com.holub.life.LifeGame;

/**
 * Edit → Redo: 실행 중이면 멈추고, Undo로 되돌린 변경을 다시 실행함
 */
public class RedoCommand implements Command {
    private final LifeGame game;

    public RedoCommand(LifeGame game) {
        this.game = game;
    }

    @Override
    public void execute() {
        game.redo();
    }
}

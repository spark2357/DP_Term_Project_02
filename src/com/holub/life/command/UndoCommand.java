package com.holub.life.command;

import com.holub.life.game.LifeGame;

/**
 * Edit → Undo: 실행 중이면 멈추고, 마지막 변경(마우스 편집, 세대 진행, Clear, Load) 전의 보드로 되돌림
 */
public class UndoCommand implements Command {
    private final LifeGame game;

    public UndoCommand(LifeGame game) {
        this.game = game;
    }

    @Override
    public void execute() {
        game.undo();
    }
}

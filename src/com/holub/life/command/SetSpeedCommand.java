package com.holub.life.command;

import com.holub.life.LifeGame;

/**
 * Go → Agonizing, Slow, Medium, Fast: 정해진 간격으로 게임을 시작함
 * 이미 실행 중이면 새 간격으로 바뀜
 */
public class SetSpeedCommand implements Command {
    private final LifeGame game;
    private final int millisecondsBetweenTicks;

    // 0이면 Halt
    public SetSpeedCommand(LifeGame game, int millisecondsBetweenTicks) {
        if (millisecondsBetweenTicks <= 0) {
            throw new IllegalArgumentException("millisecondsBetweenTicks must be positive: " + millisecondsBetweenTicks);
        }
        this.game = game;
        this.millisecondsBetweenTicks = millisecondsBetweenTicks;
    }

    @Override
    public void execute() {
        game.start(millisecondsBetweenTicks);
    }
}

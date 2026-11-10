package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.ui.MenuSite;
import java.util.List;

/**
 * Go 메뉴: 항목마다 Command를 하나씩 만들어 MenuSite에 등록함
 * 원본(Clock.createMenus)은 모든 항목이 리스너 하나를 함께 쓰고, 메뉴 이름의 첫 글자로 할 일을 골랐음.
 * 여기서는 등록할 때 항목과 Command를 연결하므로 이름을 바꾸거나 항목을 더해도 분기를 고칠 필요가 없음
 */
public class GoMenu {
    private record Speed(String name, int millisecondsBetweenTicks) {
    }

    private static final List<Speed> SPEEDS = List.of(
            new Speed("Agonizing", 500),
            new Speed("Slow", 150),
            new Speed("Medium", 70),
            new Speed("Fast", 30));

    private final LifeGame game;

    public GoMenu(LifeGame game) {
        this.game = game;
    }

    public void register() {
        add("Halt", new HaltCommand(game));
        add("Tick (Single Step)", new TickCommand(game));
        for (Speed speed : SPEEDS) {
            add(speed.name(), new SetSpeedCommand(game, speed.millisecondsBetweenTicks()));
        }
    }

    private void add(String name, Command command) {
        MenuSite.addLine(this, "Go", name, event -> command.execute());
    }
}

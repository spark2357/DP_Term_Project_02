package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.ui.MenuSite;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.KeyStroke;

/**
 * Go 메뉴: 항목마다 Command를 하나씩 만들어 MenuSite에 등록함
 * 원본(Clock.createMenus)은 모든 항목이 리스너 하나를 함께 쓰고, 메뉴 이름의 첫 글자로 할 일을 골랐음.
 * 여기서는 등록할 때 항목과 Command를 연결하므로 이름을 바꾸거나 항목을 더해도 분기를 고칠 필요가 없음
 * <p>
 * 단축키도 등록할 때 항목마다 적어 둠 (Ctrl 같은 수식키 없이 키 하나). 이름의 첫 글자에서 뽑지 않으므로
 * Medium(D)처럼 이름과 다른 키를 쓸 수 있고, 이름을 바꿔도 키는 그대로임
 */
public class GoMenu {
    private record Speed(String name, int millisecondsBetweenTicks, int key) {
    }

    // 느린 것부터 빠른 것 순서로 왼손 홈 자리(A S D F)에 둠
    private static final List<Speed> SPEEDS = List.of(
            new Speed("Agonizing", 500, KeyEvent.VK_A),
            new Speed("Slow", 150, KeyEvent.VK_S),
            new Speed("Medium", 70, KeyEvent.VK_D),
            new Speed("Fast", 30, KeyEvent.VK_F));

    private final LifeGame game;
    private final Command halt;

    public GoMenu(LifeGame game) {
        this(game, new HaltCommand(game));
    }

    // [수정] 2026-11-16: Halt 메뉴가 할 일을 밖에서 받을 수 있게 함.
    // Life.main에서 "멈추기 + 실행 리포트"(MacroCommand)를 넘김. GoMenu는 그게 묶음인지 모름
    public GoMenu(LifeGame game, Command halt) {
        this.game = game;
        this.halt = halt;
    }

    public void register() {
        add("Halt", new HaltCommand(game), KeyEvent.VK_SPACE);
        add("Tick (Single Step)", new TickCommand(game), KeyEvent.VK_T);
        for (Speed speed : SPEEDS) {
            add(speed.name(), new SetSpeedCommand(game, speed.millisecondsBetweenTicks()), speed.key());
        }
    }

    private void add(String name, Command command, int key) {
        MenuSite.addLine(this, "Go", name, event -> command.execute());
        MenuSite.getMyMenuItem(this, "Go", name).setAccelerator(KeyStroke.getKeyStroke(key, 0));
    }
}

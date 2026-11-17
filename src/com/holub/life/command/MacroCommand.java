package com.holub.life.command;

import java.util.List;

/**
 * 여러 Command를 묶어 순서대로 실행하는 Command (Command에 Composite을 적용)
 * <p>
 * Neighborhood가 Cell을 담은 Cell인 것처럼, MacroCommand는 Command를 담은 Command임.
 * 메뉴는 이것이 묶음인지 모르고 다른 Command와 똑같이 execute()만 부름.
 * 묶이는 Command들은 서로를 모르므로 기존 Command를 고치지 않고 동작을 덧붙일 수 있음.
 * 예: Halt = 게임 멈추기(HaltCommand) + 실행 리포트 출력(ShowRunReportCommand)
 */
public class MacroCommand implements Command {
    private final List<Command> commands;

    // 실행 순서대로 넘김
    public MacroCommand(Command... commands) {
        this.commands = List.of(commands);
    }

    @Override
    public void execute() {
        for (Command command : commands)
            command.execute();
    }
}

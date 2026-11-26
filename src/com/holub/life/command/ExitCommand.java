package com.holub.life.command;

/**
 * Grid → Exit: 프로그램을 끝냄
 */
public class ExitCommand implements Command {
    @Override
    public void execute() {
        System.exit(0);
    }
}

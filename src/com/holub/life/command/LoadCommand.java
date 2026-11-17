package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.life.PatternFormat;
import com.holub.life.PatternFormats;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Grid → Load: 파일을 골라 확장자에 맞는 형식으로 읽음. 읽기에 실패하면 보드는 그대로 남음
 * 파일 창을 열기 전에 게임을 멈춤 (창이 떠 있는 동안에도 tick이 오기 때문)
 */
public class LoadCommand implements Command {
    private final LifeGame game;
    private final PatternFormats formats;
    private final Dialogs dialogs;

    public LoadCommand(LifeGame game, PatternFormats formats, Dialogs dialogs) {
        this.game = game;
        this.formats = formats;
        this.dialogs = dialogs;
    }

    @Override
    public void execute() {
        game.stop();

        Optional<File> chosen = dialogs.chooseFile("Load", formats.loadFilters());
        if (chosen.isEmpty()) {
            return;
        }

        File file = chosen.get();
        Optional<PatternFormat> format = formats.formatFor(file);
        if (format.isEmpty()) {
            dialogs.showError("Can't load \"" + file.getName() + "\".\n"
                    + "Supported files: " + formats.extensionList() + ".");
            return;
        }

        try (InputStream in = new FileInputStream(file)) {
            game.load(in, format.get());
        } catch (IOException e) {
            dialogs.showError("Can't load \"" + file.getName() + "\".\n"
                    + "The file may be missing or damaged, may not be a Life pattern,\n"
                    + "or may not fit on the board.");
        }
    }
}

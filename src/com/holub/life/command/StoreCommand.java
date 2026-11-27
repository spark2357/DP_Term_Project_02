package com.holub.life.command;

import com.holub.life.game.LifeGame;
import com.holub.life.model.PatternFormat;
import com.holub.life.format.PatternFormats;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Grid → Store: 파일을 골라 확장자에 맞는 형식으로 지금 보드를 씀
 * 파일 창을 열기 전에 게임을 멈춤 (OK를 누른 순간의 보드가 아니라 멈춘 보드를 저장)
 */
public class StoreCommand implements Command {
    private final LifeGame game;
    private final PatternFormats formats;
    private final Dialogs dialogs;

    public StoreCommand(LifeGame game, PatternFormats formats, Dialogs dialogs) {
        this.game = game;
        this.formats = formats;
        this.dialogs = dialogs;
    }

    @Override
    public void execute() {
        game.stop();

        Optional<File> chosen = dialogs.chooseFile("Store", formats.storeFilters());
        if (chosen.isEmpty()) {
            return;
        }

        File file = chosen.get();
        Optional<PatternFormat> format = formats.formatFor(file);
        if (format.isEmpty()) {
            dialogs.showError("Can't save \"" + file.getName() + "\".\n"
                    + "Use a file name that ends in " + formats.extensionList() + ".");
            return;
        }

        try (OutputStream out = new FileOutputStream(file)) {
            game.store(out, format.get());
        } catch (IOException e) {
            dialogs.showError("Can't save \"" + file.getName() + "\".\n"
                    + "Check that the folder exists and that you can write to it.");
        }
    }
}

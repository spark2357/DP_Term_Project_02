package com.holub.life.command;

import com.holub.life.LifeGame;
import com.holub.life.PatternFormats;
import com.holub.life.RleFormat;
import com.holub.life.mouse.MouseController;
import com.holub.life.mouse.Pen;
import com.holub.life.mouse.StampMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Grid → Stamp: 스탬프 모드를 켜고 끔
 * 켤 때는 .rle 파일을 골라 패턴 모양만 읽고(#R 위치는 무시), 마우스를 스탬프 모드로 바꿈. 다시 누르면 기본 모드로 돌아감
 */
public class StampCommand implements Command {
    private final LifeGame game;
    private final RleFormat rle;
    private final MouseController mouse;
    private final Pen pen;
    private final Dialogs dialogs;

    public StampCommand(LifeGame game, RleFormat rle, MouseController mouse, Pen pen, Dialogs dialogs) {
        this.game = game;
        this.rle = rle;
        this.mouse = mouse;
        this.pen = pen;
        this.dialogs = dialogs;
    }

    @Override
    public void execute() {
        if (mouse.mode() instanceof StampMode) {
            mouse.resetMode();
            return;
        }

        Optional<File> chosen = dialogs.chooseFile("Stamp", List.of(PatternFormats.filterFor(rle)));
        if (chosen.isEmpty()) {
            return;
        }

        File file = chosen.get();
        try (InputStream in = new FileInputStream(file)) {
            mouse.setMode(new StampMode(game, rle.readPattern(in), pen));
        } catch (IOException e) {
            dialogs.showError("Can't stamp \"" + file.getName() + "\".\n"
                    + "Choose an RLE pattern (." + rle.extension() + ") that fits on the board.");
        }
    }
}

package com.holub.life.command;

import java.io.File;
import java.util.Optional;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Command가 사용자에게 보여주는 창.
 */
public interface Dialogs {
    // 취소하면 빈 Optional
    Optional<File> chooseFile(String selectButtonText, FileNameExtensionFilter[] filters);

    void showError(String message);
}

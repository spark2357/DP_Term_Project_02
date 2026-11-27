package com.holub.life.command;

import com.holub.life.ExtensionFilter;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Command가 사용자에게 보여주는 창.
 */
public interface Dialogs {
    // 취소하면 빈 Optional
    Optional<File> chooseFile(String selectButtonText, List<ExtensionFilter> filters);

    void showError(String message);
}

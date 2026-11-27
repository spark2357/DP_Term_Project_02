package com.holub.life.view;

import com.holub.io.Files;
import com.holub.life.format.ExtensionFilter;
import java.io.File;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.holub.life.command.Dialogs;

/**
 * 실제 Swing 창으로 묻고 알리는 Dialogs
 */
public class SwingDialogs implements Dialogs {
    @Override
    public Optional<File> chooseFile(String selectButtonText, List<ExtensionFilter> filters) {
        return Files.userSelected(".", selectButtonText, toSwing(filters));
    }

    // 모델의 ExtensionFilter를 JFileChooser가 쓰는 Swing 필터로 바꿈
    private static FileNameExtensionFilter[] toSwing(List<ExtensionFilter> filters) {
        return filters.stream()
                .map(f -> new FileNameExtensionFilter(f.description(), f.extensions().toArray(new String[0])))
                .toArray(FileNameExtensionFilter[]::new);
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "The Game of Life", JOptionPane.ERROR_MESSAGE);
    }
}

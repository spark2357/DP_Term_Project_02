package com.holub.life.command;

import com.holub.io.Files;
import java.io.File;
import java.util.Optional;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 실제 Swing 창으로 묻고 알리는 Dialogs
 */
public class SwingDialogs implements Dialogs {
    @Override
    public Optional<File> chooseFile(String selectButtonText, FileNameExtensionFilter[] filters) {
        return Files.userSelected(".", selectButtonText, filters);
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "The Game of Life", JOptionPane.ERROR_MESSAGE);
    }
}

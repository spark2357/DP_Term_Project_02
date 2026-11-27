package com.holub.life.support;

import com.holub.life.ExtensionFilter;
import com.holub.life.command.Dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 파일 창과 오류 창을 대신하는 가짜 Dialogs (테스트 더블)
 * 화면 없이 Load, Store, Stamp Command를 테스트할 때 씀
 *
 *   - nextFile : 파일 창에서 고를 파일. null이면 취소한 것으로 봄
 *   - errors   : showError()로 받은 오류 메시지를 순서대로 모아 둠
 */
public class FakeDialogs implements Dialogs {
	public File nextFile;
	public final List<String> errors = new ArrayList<>();
	public Runnable onChooseFile = () -> {
	};

	@Override
	public Optional<File> chooseFile(String selectButtonText, List<ExtensionFilter> filters) {
		onChooseFile.run();
		return Optional.ofNullable(nextFile);
	}

	@Override
	public void showError(String message) {
		errors.add(message);
	}
}

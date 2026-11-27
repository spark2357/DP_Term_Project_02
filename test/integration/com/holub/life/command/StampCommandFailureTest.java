package com.holub.life.command;

import com.holub.life.mouse.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.holub.life.format.RleFormat;
import com.holub.life.game.LifeGame;
import com.holub.life.model.Board;
import com.holub.life.model.CellColor;

/**
 * StampCommand 통합 테스트: E. 스탬프 모드 켜기에서 고른 파일을 패턴으로 읽지 못하는 경로
 * 진짜 LifeGame·Board와 MouseController에 가짜 파일 창(FakeDialogs)을 넣어 화면 없이 실행함. 파일은 임시 폴더(@TempDir)에 씀
 *
 * StampCommandTest와 따로 둔 이유
 *   - StampCommandTest는 파일을 읽기 전에 끝나는 경로(취소, 끄기)만 다룸. 이 클래스는 실제 파일을 읽다가 실패하는 경로
 *
 * [테스트 시나리오]
 *   1. 읽을 수 없는 패턴 파일 (깨진 RLE, 보드보다 큰 패턴, .life 파일): 오류 창 하나, 마우스는 기본 모드(BrushMode) 그대로, 보드도 그대로
 *
 * [커버리지]
 *   - StampCommandFailureTest.java 테스트 대상: StampCommand.execute (패턴 읽기 실패)
 *   - 테스트 제외 대상
 *     - 켜기 성공, 끄기: StampCommandTest, GridCommandTest에서 확인
 *     - 찍기, 회전: StampModeTest, GridCommandTest에서 확인
 *     - 파일 창에서 취소: StampCommandTest에서 확인
 *     - 스탬프 모드를 켜도 게임은 멈추지 않음(의도한 동작), 따로 테스트하지 않음
 */
@Tag("extension")
class StampCommandFailureTest{
	@TempDir
	Path folder;

	private final Board board = new Board();
	private final LifeGame game = new LifeGame(board, new FakeTicker());
	private final FakeDialogs dialogs = new FakeDialogs();
	private final RleFormat rle = new RleFormat(LifeTestDriver.SIZE);
	private final Pen pen = new Pen(CellColor.DEFAULT);
	private final MouseController mouse = new MouseController(new BrushMode(game, pen));

	static Stream<Arguments> unreadableFiles() throws IOException{
		return Stream.of(
				Arguments.of("깨진 RLE", "broken.rle", "x = 3, y = 3\nbo$2bo$3x!".getBytes(StandardCharsets.UTF_8)),
				Arguments.of("보드보다 큰 패턴", "big.rle", "x = 65, y = 1\n65o!".getBytes(StandardCharsets.UTF_8)),
				Arguments.of(".life 파일", "pattern.life", lifeFile()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreadableFiles")
	@DisplayName("execute(): 패턴으로 읽을 수 없는 파일은 오류 창을 하나 띄우고, 마우스는 기본 모드 그대로이고, 보드도 그대로다")
	void unreadableFileShowsErrorAndKeepsBrushMode(String label, String name, byte[] content) throws IOException{
		// Given
		board.toggle(10, 10);
		Path file = folder.resolve(name);
		Files.write(file, content);
		dialogs.nextFile = file.toFile();

		// When
		new StampCommand(game, rle, mouse, pen, dialogs).execute();

		// Then
		assertEquals(1, dialogs.errors.size());
		assertInstanceOf(BrushMode.class, mouse.mode());
		assertEquals(Cells.fromRowColumn("10,10"), board.liveCells());
	}

	// 예전 형식 .life 파일 (Point 목록을 Java 직렬화)
	private static byte[] lifeFile() throws IOException{
		LinkedList<Point> cells = new LinkedList<>(Set.of(new Point(1, 1), new Point(2, 1)));
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(file)){
			out.writeObject(cells);
		}
		return file.toByteArray();
	}
}

package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 모델 아키텍처 테스트: 컴파일된 클래스를 읽어 모델과 화면(Swing/AWT)이 분리되어 있는지, 그리기 잠금이 없어졌는지 확인 (설계 개선)
 *
 *   확인 방법
 *   - 규칙 1: JDK의 jdeps 도구(ToolProvider)로 모델 클래스 파일의 의존 클래스를 뽑아 javax.swing, java.awt를 찾음. 새 라이브러리 없음
 *     클래스 폴더는 실행할 때 Board.class가 들어 있는 위치(CodeSource)에서 찾음
 *   - 규칙 2: jdeps로는 "잠금으로 쓰는지"를 알 수 없으므로 Neighborhood의 필드와 메서드를 리플렉션으로 확인
 *
 *   모델 클래스 (com.holub.life 바로 아래, 안쪽 클래스 포함)
 *   - 보드와 셀: Board, BoardView, Cell, Neighborhood, Resident, Direction, Storable, LiveCells, CellPattern, CellPainter
 *   - 셀 상태와 색 규칙: CellState, Alive, Dead, CellColor, BirthColorRule, ClashRule, BlendRule
 *   - 기록과 통계: BoardHistory, BoardSnapshot, CellVisitor, CellStatistics, StatisticsVisitor, RunRecorder, RunReport
 *   - 파일 형식: PatternFormat, PatternFormats, LifeFormatAdapter, RleFormat
 *   - 게임 Facade와 시계: LifeGame, Clock, TickSource, TickTimer
 *   - 제외(화면 쪽): Life, Universe, SwingTickTimer(Swing Adapter), brush, command(메뉴), mouse, painter 패키지
 *
 *   허용하는 java.awt 클래스
 *   - java.awt.Point: 셀 좌표 값 객체로 씀 (Cell.Memento, LiveCells, 파일 형식, LifeGame)
 *   - java.awt.Rectangle: 그릴 영역 값 객체로 씀 (Cell.redraw(), CellPainter, BoardView). 실제 그리기는 painter 패키지가 함
 *
 * [테스트 시나리오]
 *   1. 모델 클래스는 javax.swing, java.awt에 의존하지 않음 (Point, Rectangle 제외)
 *   2. Neighborhood에는 잠금으로 쓰는 static 필드, static 공유 상태, static synchronized 메서드가 없음 (마지막 그리기 요청 소실)
 *
 * [커버리지]
 *   - ModelArchitectureTest.java 테스트 대상: 모델 클래스의 직접 의존, Neighborhood의 static 멤버
 *   - 테스트 제외 대상
 *     - 다른 클래스를 거친 간접 의존 (예: 모델 → com.holub.ui → java.awt): 직접 의존만 확인
 *     - tick이 EDT에서 실행되는지: SwingTickTimerTest에서 확인
 *     - 화면 쪽이라 모델 목록에서 뺀 클래스 (단위 테스트도 없음)
 *       - Life: 메인 창(JFrame)과 조립 코드(main). 객체를 만들어 연결만 함
 *       - Universe: 보드를 그리고 마우스 입력을 셀 좌표로 바꾸는 JPanel
 *       - SwingTickTimer: javax.swing.Timer Adapter. tick이 EDT에서 오는지는 SwingTickTimerTest에서 확인
 *       - brush(BrushToolBar), painter(SwingCellPainter): Swing 도구 막대와 Graphics 그리기
 *       - command, mouse: 메뉴와 마우스 모드. 통합 테스트(test/integration의 command, mouse)에서 확인
 *     - com.holub.life 밖의 도구 클래스 (모델이 아니라 검사 대상 아님. Publisher 말고는 단위 테스트도 없음)
 *       - com.holub.ui.MenuSite, com.holub.ui.Colors: Swing 메뉴 바와 색 상수
 *       - com.holub.io.Files: 파일 창(JFileChooser). SwingDialogs만 씀
 *       - com.holub.tools.Log: 디버깅용 로그 설정. MenuSite 안의 수동 확인용 코드만 씀
 *       - com.holub.asynch.ConditionVariable: 원본의 그리기 잠금. 지금 src에서는 쓰는 곳이 없음 (규칙 2가 다시 쓰이지 않는지 확인)
 *       - com.holub.tools.Publisher: Swing과 상관없는 Observer 도구. PublisherTest(단위)에서 확인
 *   - 규칙 1은 지금 PatternFormats가 javax.swing.filechooser.FileNameExtensionFilter를 써서 실패함 (예외로 두지 않기로 정함)
 */
@Tag("improvement")
class ModelArchitectureTest{
	private static final List<String> MODEL_CLASSES = List.of(
			"Board", "BoardView", "Cell", "Neighborhood", "Resident", "Direction", "Storable", "LiveCells", "CellPattern", "CellPainter",
			"CellState", "Alive", "Dead", "CellColor", "BirthColorRule", "ClashRule", "BlendRule",
			"BoardHistory", "BoardSnapshot", "CellVisitor", "CellStatistics", "StatisticsVisitor", "RunRecorder", "RunReport",
			"PatternFormat", "PatternFormats", "LifeFormatAdapter", "RleFormat",
			"LifeGame", "Clock", "TickSource", "TickTimer");

	private static final Set<String> ALLOWED_AWT = Set.of("java.awt.Point", "java.awt.Rectangle");

	// jdeps -verbose:class 출력 한 줄: "   com.holub.life.Board   -> java.awt.Point   java.desktop"
	private static final Pattern DEPENDENCY = Pattern.compile("^\\s+(\\S+)\\s+->\\s+(\\S+)");

	@Test
	@DisplayName("모델 클래스의 모든 메서드(): javax.swing, java.awt 클래스를 쓰지 않는다 (값 객체 Point, Rectangle만 허용)")
	void modelClassesDoNotDependOnSwingOrAwt() throws Exception{
		// Given: 모델 클래스 파일
		List<String> classFiles = modelClassFiles();

		// When
		List<String> dependencies = jdepsDependencies(classFiles);

		// Then: "클래스 -> UI 의존" 목록이 비어 있어야 함
		Set<String> violations = new TreeSet<>();
		for (String line : dependencies) {
			Matcher m = DEPENDENCY.matcher(line);
			if (m.find() && isUi(m.group(2))) {
				violations.add(m.group(1) + " -> " + m.group(2));
			}
		}
		assertTrue(violations.isEmpty(), () -> "모델 클래스가 화면(Swing/AWT)에 의존함: " + violations);
	}

	@Test
	@DisplayName("Neighborhood.redraw(), transition(): 보드끼리 함께 쓰는 static 잠금(static 필드, static synchronized 메서드)을 쓰지 않는다 (결함)")
	void neighborhoodHasNoStaticLock(){
		// Given: Neighborhood와 안쪽 클래스. 원본은 static readingPermitted(ConditionVariable)를 잠금으로 써서 마지막 그리기 요청 소실이 생겼음
		List<Class<?>> classes = new ArrayList<>();
		classes.add(Neighborhood.class);
		classes.addAll(List.of(Neighborhood.class.getDeclaredClasses()));

		// When: static 필드 중 잠금 타입이거나 바뀌는 값(final 아님)인 것, static synchronized 메서드를 모음
		Set<String> found = new TreeSet<>();
		for (Class<?> type : classes) {
			for (Field field : type.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (!field.isSynthetic() && Modifier.isStatic(modifiers)
						&& (isLockType(field.getType()) || !Modifier.isFinal(modifiers))) {
					found.add("static 필드 " + type.getSimpleName() + "." + field.getName() + " (" + field.getType().getName() + ")");
				}
			}
			for (Method method : type.getDeclaredMethods()) {
				int modifiers = method.getModifiers();
				if (Modifier.isStatic(modifiers) && Modifier.isSynchronized(modifiers)) {
					found.add("static synchronized 메서드 " + type.getSimpleName() + "." + method.getName() + "()");
				}
			}
		}

		// Then
		assertTrue(found.isEmpty(), () -> "Neighborhood에 보드끼리 함께 쓰는 잠금이 남아 있음: " + found);
	}

	// ---- 헬퍼 메서드

	private static boolean isUi(String className){
		if (className.startsWith("javax.swing.")) {
			return true;
		}
		return className.startsWith("java.awt.") && !ALLOWED_AWT.contains(className);
	}

	// 원본의 readingPermitted(ConditionVariable) 같은 잠금 타입. Object 필드도 synchronized의 잠금으로 쓰일 수 있어 포함
	private static boolean isLockType(Class<?> type){
		return type == Object.class
				|| type.getName().equals("com.holub.asynch.ConditionVariable")
				|| Lock.class.isAssignableFrom(type)
				|| ReadWriteLock.class.isAssignableFrom(type)
				|| Condition.class.isAssignableFrom(type)
				|| type.getName().startsWith("java.util.concurrent.");
	}

	// 컴파일된 클래스 폴더에서 모델 클래스와 그 안쪽 클래스(Board$CellSetMemento 등)의 .class 파일을 찾음
	private static List<String> modelClassFiles() throws IOException, URISyntaxException{
		Path classesRoot = Path.of(Board.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path packageFolder = classesRoot.resolve("com/holub/life");
		assertTrue(Files.isDirectory(packageFolder), () -> "클래스 폴더를 찾지 못함: " + packageFolder);

		List<String> files = new ArrayList<>();
		for (String name : MODEL_CLASSES) {
			Path outer = packageFolder.resolve(name + ".class");
			assertTrue(Files.isRegularFile(outer), () -> "모델 클래스 파일이 없음: " + outer);
			files.add(outer.toString());
			try (Stream<Path> inner = Files.list(packageFolder)) {
				inner.filter(p -> p.getFileName().toString().startsWith(name + "$"))
						.forEach(p -> files.add(p.toString()));
			}
		}
		return files;
	}

	// jdeps -verbose:class로 클래스 단위 의존을 뽑음
	private static List<String> jdepsDependencies(List<String> classFiles){
		ToolProvider jdeps = ToolProvider.findFirst("jdeps")
				.orElseThrow(() -> new AssertionError("JDK의 jdeps 도구를 찾지 못함 (JRE가 아닌 JDK로 실행해야 함)"));
		List<String> args = new ArrayList<>(List.of("-verbose:class", "-filter:none"));
		args.addAll(classFiles);

		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = jdeps.run(new PrintWriter(out), new PrintWriter(err), args.toArray(new String[0]));
		assertEquals(0, exitCode, () -> "jdeps 실행 실패: " + err);

		List<String> lines = out.toString().lines().toList();
		assertTrue(lines.stream().anyMatch(line -> line.contains("com.holub.life.Board ")),
				() -> "jdeps 출력에 모델 클래스가 없음: " + out);
		return lines;
	}
}

package com.holub.life.mouse;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * MouseController 단위 테스트: 지금 마우스 모드를 들고 있다가 마우스 입력을 넘기는 곳 (State 패턴의 Context)
 * 진짜 모드(ToggleMode, StampMode) 대신 받은 입력을 기록하는 가짜 모드(FakeMode)를 넣어 확인함
 *
 *   메서드
 *   - MouseController(기본 모드) : 처음 모드는 기본 모드
 *   - setMode(모드)              : 모드를 바꾸고 모드 리스너를 부름
 *   - resetMode()                : 기본 모드로 돌아감
 *   - setModeListener(listener)  : 모드가 바뀔 때 부를 리스너 (화면이 이전 모드의 미리보기를 지우도록)
 *   - clicked, rightClicked, moved, exited, drawPreview : 지금 모드에 그대로 넘김
 *
 * [테스트 시나리오]
 *   1. 처음 모드: 만들면 기본 모드
 *   2. 모드 바꾸기: setMode()하면 모드가 바뀌고 리스너가 한 번 불림. 리스너를 정하지 않았어도 예외 없음
 *   3. 기본 모드로: resetMode()하면 기본 모드로 돌아가고 리스너가 불림
 *   4. 입력 전달: 클릭, 오른쪽 클릭, 미리보기는 지금 모드로 감. moved(), exited()는 지금 모드의 답을 그대로 돌려줌.
 *      모드를 바꾸면 그 뒤 입력은 새 모드로만 감
 *
 * [커버리지]
 *   - MouseControllerTest.java 테스트 대상: 생성자, mode, setMode, resetMode, setModeListener, clicked, rightClicked, moved, exited, drawPreview
 *   - 테스트 제외 대상
 *     - 진짜 모드(칠하기, Stamp)와 함께 쓰는 것: 통합 테스트 ToggleModeTest에서 확인
 *     - 픽셀을 셀 좌표로 바꿔 넘기고 리스너로 다시 그리는 Universe의 마우스 처리 (GUI)
 *     - MouseMode의 default 메서드: 가짜 모드가 모두 직접 구현해서 이 테스트에서는 보지 않음
 */
@Tag("extension")
class MouseControllerTest{
	private FakeMode defaultMode;
	private FakeMode otherMode;
	private MouseController mouse;

	@BeforeEach
	void setUp(){
		defaultMode = new FakeMode();
		otherMode = new FakeMode();
		mouse = new MouseController(defaultMode);
	}

	//  1. 처음 모드

	@Test
	@DisplayName("MouseController(): 처음 모드는 만들 때 넘긴 기본 모드다")
	void startsWithDefaultMode(){
		assertSame(defaultMode, mouse.mode());
	}

	//  2. 모드 바꾸기

	@Test
	@DisplayName("setMode(): 지금 모드를 넘긴 모드로 바꾼다")
	void setModeSwitchesMode(){
		// When
		mouse.setMode(otherMode);

		// Then
		assertSame(otherMode, mouse.mode());
	}

	@Test
	@DisplayName("setMode(): 모드를 바꾸면 모드 리스너를 한 번 부른다")
	void setModeTellsListener(){
		// Given
		List<String> calls = new ArrayList<>();
		mouse.setModeListener(() -> calls.add("mode changed"));

		// When
		mouse.setMode(otherMode);

		// Then
		assertEquals(List.of("mode changed"), calls);
	}

	@Test
	@DisplayName("setMode(): 모드 리스너를 정하지 않았어도 예외 없이 모드를 바꾼다")
	void setModeWorksWithoutListener(){
		assertDoesNotThrow(() -> mouse.setMode(otherMode));
		assertSame(otherMode, mouse.mode());
	}

	//  3. 기본 모드로

	@Test
	@DisplayName("resetMode(): 다른 모드에서 기본 모드로 돌아가고 모드 리스너를 부른다")
	void resetModeReturnsToDefaultAndTellsListener(){
		// Given
		mouse.setMode(otherMode);
		List<String> calls = new ArrayList<>();
		mouse.setModeListener(() -> calls.add("mode changed"));

		// When
		mouse.resetMode();

		// Then
		assertSame(defaultMode, mouse.mode());
		assertEquals(List.of("mode changed"), calls);
	}

	//  4. 입력 전달

	@Test
	@DisplayName("clicked(), rightClicked(): 왼쪽, 오른쪽 클릭을 그 셀 좌표 그대로 지금 모드에 넘긴다")
	void clicksGoToCurrentMode(){
		// When
		mouse.clicked(1, 2);
		mouse.rightClicked(3, 4);

		// Then
		assertEquals(List.of("clicked 1,2", "rightClicked 3,4"), defaultMode.calls);
	}

	@Test
	@DisplayName("moved(), exited(): 지금 모드에 넘기고, 다시 그려야 하는지는 모드의 답을 그대로 돌려준다")
	void movedAndExitedReturnModeAnswer(){
		// Given: 이동에는 true, 나감에는 false라고 답하는 모드
		defaultMode.movedAnswer = true;
		defaultMode.exitedAnswer = false;

		// When
		boolean movedRepaint = mouse.moved(5, 6);
		boolean exitedRepaint = mouse.exited();

		// Then
		assertTrue(movedRepaint);
		assertFalse(exitedRepaint);
		assertEquals(List.of("moved 5,6", "exited"), defaultMode.calls);
	}

	@Test
	@DisplayName("drawPreview(): 셀 크기(픽셀)를 그대로 지금 모드에 넘긴다")
	void drawPreviewGoesToCurrentMode(){
		// When: 가짜 모드는 Graphics를 쓰지 않으므로 null을 넘김
		mouse.drawPreview(null, 8);

		// Then
		assertEquals(List.of("drawPreview 8"), defaultMode.calls);
	}

	@Test
	@DisplayName("clicked(): 모드를 바꾼 뒤의 입력은 새 모드로만 간다")
	void inputGoesToNewModeAfterSwitch(){
		// Given
		mouse.setMode(otherMode);

		// When
		mouse.clicked(7, 8);

		// Then
		assertEquals(List.of("clicked 7,8"), otherMode.calls);
		assertEquals(List.of(), defaultMode.calls);
	}

	// ---- 헬퍼

	private static class FakeMode implements MouseMode{
		final List<String> calls = new ArrayList<>();
		boolean movedAnswer;
		boolean exitedAnswer;

		@Override
		public void clicked(int row, int column){
			calls.add("clicked " + row + "," + column);
		}

		@Override
		public void rightClicked(int row, int column){
			calls.add("rightClicked " + row + "," + column);
		}

		@Override
		public boolean moved(int row, int column){
			calls.add("moved " + row + "," + column);
			return movedAnswer;
		}

		@Override
		public boolean exited(){
			calls.add("exited");
			return exitedAnswer;
		}

		@Override
		public void drawPreview(Graphics g, int pixelsPerCell){
			calls.add("drawPreview " + pixelsPerCell);
		}
	}
}

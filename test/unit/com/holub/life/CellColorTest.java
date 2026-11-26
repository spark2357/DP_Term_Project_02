package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * CellColor 단위 테스트: 색 값
 *
 *   - 생성자 : 0~255 밖의 값은 거부
 *   - equals : 값이 같으면 같은 색 (record)
 *
 * [테스트 시나리오]
 *   1. 범위: 경계값 0과 255는 받아들이고 값을 그대로 가짐. 빨강, 초록, 파랑 중 하나라도 0~255 밖이면 IllegalArgumentException
 *   2. 같음: 빨강, 초록, 파랑 값이 같으면 같은 색
 *
 * [커버리지]
 *   - CellColorTest.java 테스트 대상: CellColor 생성자, equals
 *   - 테스트 제외 대상
 *     - 태어날 셀의 색을 정하는 규칙: ClashRuleTest, BlendRuleTest에서 확인
 *     - com.holub.ui.Colors: Swing(java.awt.Color) 색 상수만 모은 Holub 원본 인터페이스라 제외.
 *       CellColor를 화면 색(java.awt.Color)으로 바꾸는 것은 SwingCellPainter(GUI)의 일이라 제외
 */
@Tag("extension")
class CellColorTest {

	@Test
	@DisplayName("equals(): 빨강, 초록, 파랑 값이 같으면 같은 색이다")
	void equalByValue() {
		assertEquals(CellColor.DEFAULT, new CellColor(255, 0, 0));
		assertNotEquals(CellColor.DEFAULT, new CellColor(0, 0, 255));
	}

	@ParameterizedTest(name = "({0}, {1}, {2})")
	@CsvSource({
		"0, 0, 0",
		"255, 255, 255",
		"0, 255, 0",
		"255, 0, 255"
	})
	@DisplayName("CellColor(): 경계값 0과 255는 받아들이고 값을 그대로 가진다")
	void acceptsBoundaryComponents(int red, int green, int blue){
		// When
		CellColor color = new CellColor(red, green, blue);

		// Then
		assertEquals(red, color.red());
		assertEquals(green, color.green());
		assertEquals(blue, color.blue());
	}

	@ParameterizedTest(name = "({0}, {1}, {2})")
	@CsvSource({
		"-1, 0, 0",
		"0, -1, 0",
		"0, 0, -1",
		"256, 0, 0",
		"0, 256, 0",
		"0, 0, 256"
	})
	@DisplayName("CellColor(): 빨강, 초록, 파랑 중 하나라도 0~255 밖이면 IllegalArgumentException")
	void rejectsAnyOutOfRangeComponent(int red, int green, int blue){
		assertThrows(IllegalArgumentException.class, () -> new CellColor(red, green, blue));
	}
}

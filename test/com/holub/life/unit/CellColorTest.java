package com.holub.life.unit;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CellColor 단위 테스트: 색 값
 *
 *   - 생성자 : 0~255 밖의 값은 거부
 *   - equals : 값이 같으면 같은 색 (record)
 *
 * [커버리지]
 *   - CellColorTest.java 테스트 대상: CellColor 생성자, equals
 *   - 태어날 셀의 색을 정하는 규칙은 BirthColorRuleTest에서 확인
 */
class CellColorTest {

	@Test
	@DisplayName("CellColor(): 0~255 밖의 값은 IllegalArgumentException")
	void rejectsOutOfRange() {
		assertThrows(IllegalArgumentException.class, () -> new CellColor(256, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new CellColor(0, -1, 0));
	}

	@Test
	@DisplayName("CellColor: 빨강, 초록, 파랑 값이 같으면 같은 색이다")
	void equalByValue() {
		assertEquals(CellColor.DEFAULT, new CellColor(255, 0, 0));
		assertNotEquals(CellColor.DEFAULT, new CellColor(0, 0, 255));
	}
}

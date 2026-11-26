package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * BlendRule 단위 테스트: 태어날 셀의 색을 부모 색의 평균으로 정하는 출생 색 규칙 (BirthColorRule Strategy의 ConcreteStrategy)
 *
 *   메서드
 *   - newborn(부모 색들) : 항상 부모 색의 평균으로 삶 (같은 색끼리면 그 색). 빨강, 초록, 파랑을 각각 평균 내고 반올림
 *
 * [테스트 시나리오]
 *   1. Blend: 같은 색끼리면 그 색, 2:1이든 1:1:1이든 세 색의 평균. 부모가 3명이 아니어도 모든 부모 색의 평균
 *   2. 반올림과 범위: .5는 올림하고, 0이나 255 근처 색끼리 섞어도 0~255 안
 *
 * [커버리지]
 *   - BlendRuleTest.java 테스트 대상: BlendRule.newborn
 *   - 테스트 제외 대상
 *     - 다수결 규칙: ClashRuleTest에서 확인
 *     - 보드 위에서 규칙을 바꿔 끼우는 동작: CellColorBoardTest에서 확인
 */
@Tag("extension")
class BlendRuleTest{
	private static final CellColor RED = CellColor.DEFAULT;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private static final BirthColorRule BLEND = new BlendRule();

	//  1. Blend

	@Test
	@DisplayName("BlendRule.newborn(): 부모가 모두 같은 색이면 그 색이다")
	void blendKeepsASingleColor() {
		assertEquals(Optional.of(BLUE), BLEND.newborn(List.of(BLUE, BLUE, BLUE)).getColor());
	}

	@Test
	@DisplayName("BlendRule.newborn(): 2:1이어도 다수 색이 아니라 세 색의 평균이다 (파랑 2 + 빨강 1 → 푸른 보라)")
	void blendAveragesEvenWithAMajority() {
		// (0+255+0)/3 = 85, 0, (255+0+255)/3 = 170
		assertEquals(Optional.of(new CellColor(85, 0, 170)), BLEND.newborn(List.of(BLUE, RED, BLUE)).getColor());
	}

	@Test
	@DisplayName("BlendRule.newborn(): 셋 다 다르면 빨강, 초록, 파랑을 각각 평균 낸 색으로 태어난다 (반올림)")
	void blendAveragesThreeColors() {
		// (0+0+255)/3 = 85, (0+128+255)/3 = 127.67 → 128, (255+0+0)/3 = 85
		assertEquals(Optional.of(new CellColor(85, 128, 85)), BLEND.newborn(List.of(BLUE, GREEN, YELLOW)).getColor());
	}

	@Test
	@DisplayName("BlendRule.newborn(): 부모가 3명이 아니어도 모든 부모 색의 평균이다")
	void blendWorksForAnyNumberOfParents() {
		assertEquals(Optional.of(new CellColor(0, 0, 128)), BLEND.newborn(List.of(BLUE, new CellColor(0, 0, 0))).getColor());
	}

	//  2. 반올림과 범위

	@Test
	@DisplayName("BlendRule.newborn(): 평균이 .5로 끝나면 올림한다 (빨강 + 검정 → (127.5, 0, 0) → (128, 0, 0))")
	void blendRoundsHalfUp(){
		// Given
		CellColor black = new CellColor(0, 0, 0);

		// When
		CellState newborn = BLEND.newborn(List.of(RED, black));

		// Then
		assertEquals(Optional.of(new CellColor(128, 0, 0)), newborn.getColor());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("blendExtremes")
	@DisplayName("BlendRule.newborn(): 0이나 255 근처 색끼리 섞어도 결과는 0~255 안에 있다")
	void blendStaysInRange(String name, List<CellColor> parents, CellColor expected){
		// When
		CellState newborn = BLEND.newborn(parents);

		// Then
		assertEquals(Optional.of(expected), newborn.getColor());
	}

	private static Stream<Arguments> blendExtremes(){
		CellColor white = new CellColor(255, 255, 255);
		CellColor almostWhite = new CellColor(254, 254, 254);
		CellColor black = new CellColor(0, 0, 0);
		CellColor almostBlack = new CellColor(1, 1, 1);
		return Stream.of(
				Arguments.of("흰색 8명", List.of(white, white, white, white, white, white, white, white), white),
				Arguments.of("흰색 + 거의 흰색 (254.5 → 255)", List.of(white, almostWhite), white),
				Arguments.of("검정 8명", List.of(black, black, black, black, black, black, black, black), black),
				Arguments.of("검정 2명 + 거의 검정 (0.33 → 0)", List.of(black, black, almostBlack), black));
	}
}

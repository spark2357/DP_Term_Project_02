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
 * ClashRule 단위 테스트: 태어날 셀의 상태를 부모의 다수 색으로 정하는 출생 색 규칙 (BirthColorRule Strategy의 ConcreteStrategy)
 *
 *   메서드
 *   - newborn(부모 색들) : 다수 색이 있으면 그 색으로 삶, 셋 다 다르면 태어나지 않음(DEAD)
 *
 * [테스트 시나리오]
 *   1. Clash: 3:0, 2:1(다수 색이 어느 자리에 있든)이면 다수 색, 1:1:1이면 DEAD. 부모가 3명이 아니어도 가장 많은 색이 하나면 그 색, 동점이면 DEAD
 *
 * [커버리지]
 *   - ClashRuleTest.java 테스트 대상: ClashRule.newborn
 *   - 테스트 제외 대상
 *     - 평균 규칙: BlendRuleTest에서 확인
 *     - 보드 위에서 규칙을 바꿔 끼우는 동작: CellColorBoardTest에서 확인
 *     - 부모가 없는 경우: 게임에서 생기지 않음(셀은 산 이웃 3개일 때만 태어남)
 */
@Tag("extension")
class ClashRuleTest{
	private static final CellColor RED = CellColor.DEFAULT;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private static final BirthColorRule CLASH = new ClashRule();

	//  1. 다수결

	@Test
	@DisplayName("ClashRule.newborn(): 3:0, 2:1이면 다수 색으로 태어난다. 다수 색이 어느 자리에 있든 같다")
	void clashUsesTheMajorityAnywhere() {
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, BLUE)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, RED)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, RED, BLUE)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(RED, BLUE, BLUE)).getColor());
	}

	@Test
	@DisplayName("ClashRule.newborn(): 셋 다 다르면 태어나지 않는다 (DEAD)")
	void clashCancelsThreeColors() {
		assertSame(CellState.DEAD, CLASH.newborn(List.of(BLUE, GREEN, YELLOW)));
	}

	@Test
	@DisplayName("ClashRule.newborn(): 부모가 3명이 아니어도 가장 많은 색이 하나면 그 색, 동점이면 태어나지 않는다")
	void clashWorksForAnyNumberOfParents() {
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, BLUE, BLUE, RED, GREEN)).getColor());
		assertSame(CellState.DEAD, CLASH.newborn(List.of(BLUE, BLUE, BLUE, RED, RED, RED)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("clashEdgeCases")
	@DisplayName("ClashRule.newborn(): 부모가 1~2명이어도 가장 많은 색이 하나면 그 색, 동점이면 태어나지 않는다")
	void clashEdgeParentCounts(String name, List<CellColor> parents, Optional<CellColor> expected){
		// When
		CellState newborn = CLASH.newborn(parents);

		// Then
		assertEquals(expected.isPresent(), newborn.isAlive());
		assertEquals(expected, newborn.getColor());
	}

	private static Stream<Arguments> clashEdgeCases(){
		return Stream.of(
				Arguments.of("부모 1명", List.of(BLUE), Optional.of(BLUE)),
				Arguments.of("같은 색 2명", List.of(BLUE, BLUE), Optional.of(BLUE)),
				Arguments.of("다른 색 2명(동점)", List.of(BLUE, RED), Optional.empty()));
	}
}

package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * BirthColorRule 단위 테스트: 태어날 셀의 상태를 부모 셋의 색으로 정하는 규칙 (Strategy)
 *
 *   규칙
 *   - ClashRule : 다수 색이 있으면 그 색, 셋 다 다르면 태어나지 않음(DEAD)
 *   - BlendRule : 항상 부모 셋 색의 평균 (같은 색끼리면 그 색)
 *
 * [테스트 시나리오]
 *   1. Clash: 3:0, 2:1(다수 색이 어느 자리에 있든)이면 다수 색, 1:1:1이면 DEAD. 부모가 3명이 아니어도 가장 많은 색이 하나면 그 색, 동점이면 DEAD
 *   2. Blend: 같은 색끼리면 그 색, 2:1이든 1:1:1이든 세 색의 평균. 부모가 3명이 아니어도 모든 부모 색의 평균
 *
 * [커버리지]
 *   - BirthColorRuleTest.java 테스트 대상: ClashRule, BlendRule
 *   - 보드 위에서 규칙을 바꿔 끼우는 동작은 CellColorBoardTest에서 확인
 */
@Tag("unclassified")
class BirthColorRuleTest {
	private static final CellColor RED = CellColor.DEFAULT;
	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final CellColor GREEN = new CellColor(0, 128, 0);
	private static final CellColor YELLOW = new CellColor(255, 255, 0);

	private static final BirthColorRule CLASH = new ClashRule();
	private static final BirthColorRule BLEND = new BlendRule();

	//  1. 다수결

	@Test
	@DisplayName("ClashRule: 3:0, 2:1이면 다수 색으로 태어난다. 다수 색이 어느 자리에 있든 같다")
	void clashUsesTheMajorityAnywhere() {
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, BLUE)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, RED)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, RED, BLUE)).getColor());
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(RED, BLUE, BLUE)).getColor());
	}

	@Test
	@DisplayName("ClashRule: 셋 다 다르면 태어나지 않는다 (DEAD)")
	void clashCancelsThreeColors() {
		assertSame(CellState.DEAD, CLASH.newborn(List.of(BLUE, GREEN, YELLOW)));
	}

	@Test
	@DisplayName("ClashRule: 부모가 3명이 아니어도 가장 많은 색이 하나면 그 색, 동점이면 태어나지 않는다")
	void clashWorksForAnyNumberOfParents() {
		assertEquals(Optional.of(BLUE), CLASH.newborn(List.of(BLUE, BLUE, BLUE, BLUE, RED, GREEN)).getColor());
		assertSame(CellState.DEAD, CLASH.newborn(List.of(BLUE, BLUE, BLUE, RED, RED, RED)));
	}

	//  2. Blend

	@Test
	@DisplayName("BlendRule: 부모가 모두 같은 색이면 그 색이다")
	void blendKeepsASingleColor() {
		assertEquals(Optional.of(BLUE), BLEND.newborn(List.of(BLUE, BLUE, BLUE)).getColor());
	}

	@Test
	@DisplayName("BlendRule: 2:1이어도 다수 색이 아니라 세 색의 평균이다 (파랑 2 + 빨강 1 → 푸른 보라)")
	void blendAveragesEvenWithAMajority() {
		// (0+255+0)/3 = 85, 0, (255+0+255)/3 = 170
		assertEquals(Optional.of(new CellColor(85, 0, 170)), BLEND.newborn(List.of(BLUE, RED, BLUE)).getColor());
	}

	@Test
	@DisplayName("BlendRule: 셋 다 다르면 빨강, 초록, 파랑을 각각 평균 낸 색으로 태어난다 (반올림)")
	void blendAveragesThreeColors() {
		// (0+0+255)/3 = 85, (0+128+255)/3 = 127.67 → 128, (255+0+0)/3 = 85
		assertEquals(Optional.of(new CellColor(85, 128, 85)), BLEND.newborn(List.of(BLUE, GREEN, YELLOW)).getColor());
	}

	@Test
	@DisplayName("BlendRule: 부모가 3명이 아니어도 모든 부모 색의 평균이다")
	void blendWorksForAnyNumberOfParents() {
		assertEquals(Optional.of(new CellColor(0, 0, 128)), BLEND.newborn(List.of(BLUE, new CellColor(0, 0, 0))).getColor());
	}
}

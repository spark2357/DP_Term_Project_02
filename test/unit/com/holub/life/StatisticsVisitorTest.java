package com.holub.life;

import static com.holub.life.support.VisitorTree.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * StatisticsVisitor 단위 테스트: Cell 트리를 돌며 산 셀 수, 활성 블록 수, 전체 블록 수를 세는 방문자 (Visitor 패턴, E. 통계 리포트 표시)
 * 트리를 도는 일은 가짜 트리(VisitorTree)가 맡음. Neighborhood.accept()처럼 "블록에 들어감 → 자식 → 블록에서 나옴" 순서로 방문자를 부름
 * Neighborhood와 Resident는 final 클래스이고 방문자 메서드의 인자 타입이라 가짜로 바꿀 수 없어서,
 * 활성 여부와 생사만 정해 둔 진짜 객체를 넘김 (방문자는 isActive(), isAlive()만 물음)
 * 여기에는 그 답(isActive(), isAlive())에 결과가 달리지 않는 테스트만 둠
 *
 *   필드
 *   - sawResident                         : 지금 블록 안에서 셀을 만났는지 (셀을 직접 담은 가장 안쪽 블록인지)
 *   - liveCells, activeBlocks, totalBlocks : 센 값
 *   메서드
 *   - enterNeighborhood(블록) : sawResident를 지움
 *   - visitResident(셀)       : sawResident를 켜고, 산 셀이면 셈
 *   - leaveNeighborhood(블록) : 셀을 직접 담은 블록이면 전체 블록 수에, 그 블록이 활성이면 활성 블록 수에도 셈
 *   - result()                : 센 값을 CellStatistics로 돌려줌
 *
 * [테스트 시나리오]
 *   1. 빈 방문: 아무것도 방문하지 않으면 모두 0
 *   2. 전체 블록 수: 셀을 직접 담은 블록은 활성이 아니어도 셈
 *
 * [커버리지]
 *   - StatisticsVisitorTest.java 테스트 대상: enterNeighborhood, visitResident, leaveNeighborhood(전체 블록 수), result
 *   - 테스트 제외 대상
 *     - 산 셀 수, 활성 블록 수, 가장 바깥 블록, 여러 단계로 겹친 블록: 결과가 진짜 Resident.isAlive(), Neighborhood.isActive()의 답에 달려 있어서
 *       통합 테스트 StatisticsVisitorCellTest에서 확인
 *     - 진짜 Cell 트리의 방문 순서: Neighborhood.accept()는 NeighborhoodTest, Resident.accept()는 ResidentTest에서 확인
 *     - 실제 보드에서 센 값: 통합 테스트 RunRecorderTest, RunReportUseCaseTest에서 확인
 */
@Tag("extension")
class StatisticsVisitorTest{
	private final StatisticsVisitor visitor = new StatisticsVisitor();

	//  1. 빈 방문

	@Test
	@DisplayName("result(): 아무것도 방문하지 않으면 산 셀, 활성 블록, 전체 블록이 모두 0이다")
	void nothingVisitedCountsZero(){
		assertEquals(new CellStatistics(0, 0, 0), visitor.result());
	}

	//  2. 전체 블록 수

	@Test
	@DisplayName("leaveNeighborhood(): 셀을 직접 담은 블록은 활성이 아니어도 전체 블록 수에 센다")
	void countsEveryInnerBlockInTotal(){
		// Given: 바깥 블록 안에 활성 블록 하나와 활성이 아닌 블록 하나
		Node tree = block(inactiveBlock(),
				block(activeBlock(), cell(deadCell())),
				block(inactiveBlock(), cell(deadCell())));

		// When
		tree.accept(visitor);

		// Then
		assertEquals(2, visitor.result().totalBlocks());
	}
}

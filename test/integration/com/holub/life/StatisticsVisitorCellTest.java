package com.holub.life;

import static com.holub.life.support.VisitorTree.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * StatisticsVisitor 통합 테스트: 진짜 Neighborhood, Resident의 상태를 보고 산 셀 수와 활성 블록 수를 세는지 (Visitor 패턴, E. 통계 리포트 표시)
 * 트리를 도는 일은 가짜 트리(VisitorTree)가 맡고, 방문자에게는 클릭으로 활성 여부와 생사를 정해 둔 진짜 블록과 셀을 넘김
 * 통합 테스트에 둔 이유: 센 값이 진짜 Resident.isAlive()와 Neighborhood.isActive()의 답(클릭한 셀은 살고, 클릭한 블록은 활성)에 달려 있음.
 * 두 클래스는 final이고 방문자 메서드의 인자 타입이라 가짜로 바꿀 수 없음
 *
 * [테스트 시나리오]
 *   1. 산 셀 수: 산 셀만 셈
 *   2. 활성 블록 수: 셀을 직접 담은 블록 중 활성인 블록만 셈
 *   3. 가장 바깥 블록: 블록을 담은 블록은 활성이어도 세지 않음
 *   4. 여러 단계로 겹친 블록: 3단계로 겹쳐도 셀을 직접 담은 가장 안쪽 블록만 셈
 *
 * [커버리지]
 *   - StatisticsVisitorCellTest.java 테스트 대상: StatisticsVisitor.visitResident(산 셀 수), leaveNeighborhood(활성 블록 수, 가장 안쪽 블록만 세기), result
 *   - 테스트 제외 대상
 *     - 빈 방문, 활성 여부와 상관없는 전체 블록 수: 단위 테스트 StatisticsVisitorTest에서 확인
 *     - 진짜 Cell 트리의 방문 순서: Neighborhood.accept()는 NeighborhoodTest, Resident.accept()는 ResidentTest에서 확인
 *     - 실제 보드에서 센 값: RunRecorderTest, RunReportUseCaseTest에서 확인
 */
@Tag("extension")
class StatisticsVisitorCellTest{
	private final StatisticsVisitor visitor = new StatisticsVisitor();

	//  1. 산 셀 수

	@Test
	@DisplayName("visitResident(): 산 셀만 센다")
	void countsOnlyLiveCells(){
		// Given: 산 셀 2개, 죽은 셀 2개를 담은 블록
		Node tree = block(inactiveBlock(), cell(liveCell()), cell(deadCell()), cell(liveCell()), cell(deadCell()));

		// When
		tree.accept(visitor);

		// Then
		assertEquals(2, visitor.result().liveCells());
	}

	//  2. 활성 블록 수

	@Test
	@DisplayName("leaveNeighborhood(): 셀을 직접 담은 블록 중 활성인 블록만 활성 블록 수에 센다")
	void countsOnlyActiveInnerBlocksAsActive(){
		// Given: 바깥 블록 안에 활성 블록 하나와 활성이 아닌 블록 하나
		Node tree = block(inactiveBlock(),
				block(activeBlock(), cell(deadCell())),
				block(inactiveBlock(), cell(deadCell())));

		// When
		tree.accept(visitor);

		// Then
		assertEquals(1, visitor.result().activeBlocks());
	}

	//  3. 가장 바깥 블록

	@Test
	@DisplayName("leaveNeighborhood(): 블록을 담은 가장 바깥 블록은 활성이어도 활성 블록 수와 전체 블록 수에 세지 않는다")
	void outerBlockIsNotCounted(){
		// Given: 활성인 바깥 블록 안에 활성이 아닌 블록 2개
		Node tree = block(activeBlock(),
				block(inactiveBlock(), cell(deadCell())),
				block(inactiveBlock(), cell(deadCell())));

		// When
		tree.accept(visitor);

		// Then
		assertEquals(new CellStatistics(0, 0, 2), visitor.result());
	}

	//  4. 여러 단계로 겹친 블록

	@Test
	@DisplayName("leaveNeighborhood(): 블록이 3단계로 겹쳐도 셀을 직접 담은 가장 안쪽 블록만 센다")
	void countsOnlyInnermostBlocksInThreeLevels(){
		// Given: 바깥 블록 → 중간 블록 2개 → 안쪽 블록 2개씩. 안쪽 블록마다 산 셀 하나, 안쪽 블록 중 2개만 활성
		Node tree = block(activeBlock(),
				block(activeBlock(),
						block(activeBlock(), cell(liveCell()), cell(deadCell())),
						block(inactiveBlock(), cell(liveCell()), cell(deadCell()))),
				block(inactiveBlock(),
						block(inactiveBlock(), cell(liveCell()), cell(deadCell())),
						block(activeBlock(), cell(liveCell()), cell(deadCell()))));

		// When
		tree.accept(visitor);

		// Then
		assertEquals(new CellStatistics(4, 2, 4), visitor.result());
	}
}

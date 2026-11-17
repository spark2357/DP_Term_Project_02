package com.holub.life;

/**
 * Cell 트리(Composite)를 돌면서 할 일을 담는 방문자 (Visitor 패턴)
 * <p>
 * 트리를 도는 일은 Cell이 맡음: Neighborhood.accept()가 "블록에 들어감 → 자식 Cell들 → 블록에서 나옴"
 * 순서로 방문자를 넘김. 방문자는 각 지점에서 할 일만 정함.
 * 트리 위에서 할 일(통계 등)을 추가할 때 Cell 클래스들은 고치지 않고 방문자 클래스만 새로 만들면 됨
 * <p>
 * 들어갈 때와 나올 때를 따로 알려 주므로 블록 단위로 값을 모을 수 있음
 * (블록에 들어갈 때 0으로 시작 → 셀을 셈 → 나올 때 그 블록의 결과를 정리)
 *
 * @see StatisticsVisitor
 */
public interface CellVisitor {
    // 블록(Neighborhood)에 들어갈 때. 이어서 블록 안의 Cell들이 방문됨
    void enterNeighborhood(Neighborhood block);

    // 셀(Resident) 하나를 만났을 때
    void visitResident(Resident cell);

    // 블록 안의 Cell을 모두 방문하고 나올 때
    void leaveNeighborhood(Neighborhood block);
}

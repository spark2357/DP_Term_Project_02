package com.holub.life;

/**
 * Cell 트리를 돌며 살아 있는 셀 수와 활성 블록 수를 세는 방문자 (Visitor 패턴)
 * <p>
 * 블록 수는 셀을 직접 담은 가장 안쪽 블록만 셈. 바깥 블록(블록들을 담은 블록)은 세지 않음.
 * 어떤 블록이 가장 안쪽인지는 그 블록 안에서 셀(Resident)을 만났는지로 판단함.
 * <p>
 * 전제: 한 블록은 셀만 담거나 블록만 담음 (Neighborhood는 원형 하나로 자식을 모두 만들므로 섞이지 않음).
 * 그래서 "지금 블록에서 셀을 만났는지"를 boolean 하나로 기억하면 됨. 블록이 몇 단계로 겹쳐 있어도 동작함
 */
public class StatisticsVisitor implements CellVisitor {
    // [수정] 2026-11-17: 스택(Deque) 대신 boolean 하나로 기억함. 블록 안에 셀과 블록이 섞이지 않으므로 스택이 필요 없음
    private boolean sawResident = false;    // 지금 들어와 있는 블록 안에서 셀을 만났는지

    private int liveCells = 0;
    private int activeBlocks = 0;
    private int totalBlocks = 0;

    @Override
    public void enterNeighborhood(Neighborhood block) {
        sawResident = false;
    }

    @Override
    public void visitResident(Resident cell) {
        sawResident = true;                 // 지금 블록은 셀을 직접 담은 블록
        if (cell.isAlive())
            liveCells++;
    }

    @Override
    public void leaveNeighborhood(Neighborhood block) {
        if (sawResident) {                  // 셀을 직접 담은 안쪽 블록만 셈
            totalBlocks++;
            if (block.isActive())
                activeBlocks++;
        }
        sawResident = false;                // 바깥 블록이 안쪽 블록의 값을 이어받지 않게 지움
    }

    // 방문이 끝난 뒤 결과를 꺼냄
    public CellStatistics result() {
        return new CellStatistics(liveCells, activeBlocks, totalBlocks);
    }
}

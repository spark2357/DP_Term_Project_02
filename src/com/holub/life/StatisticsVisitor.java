package com.holub.life;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cell 트리를 돌며 살아 있는 셀 수와 활성 블록 수를 세는 방문자 (Visitor 패턴)
 * <p>
 * 블록 수는 셀을 직접 담은 가장 안쪽 블록만 셈. 바깥 블록(블록들을 담은 블록)은 세지 않음.
 * 어떤 블록이 가장 안쪽인지는 그 블록 안에서 셀(Resident)을 만났는지로 판단함.
 * 블록에 들어갈 때 기록을 하나 쌓고, 나올 때 꺼내서 정리하므로 블록이 몇 단계로 겹쳐 있어도 동작함
 */
public class StatisticsVisitor implements CellVisitor {
    // 지금 들어가 있는 블록들. 맨 위가 가장 안쪽 블록
    private final Deque<Boolean> containsResidents = new ArrayDeque<>();

    private int liveCells = 0;
    private int activeBlocks = 0;
    private int totalBlocks = 0;

    @Override
    public void enterNeighborhood(Neighborhood block) {
        containsResidents.push(false);
    }

    @Override
    public void visitResident(Resident cell) {
        if (!containsResidents.isEmpty() && !containsResidents.peek()) {
            containsResidents.pop();          // 지금 블록은 셀을 직접 담은 블록으로 표시
            containsResidents.push(true);
        }
        if (cell.isAlive())
            liveCells++;
    }

    @Override
    public void leaveNeighborhood(Neighborhood block) {
        boolean innermost = containsResidents.pop();
        if (!innermost)
            return;
        totalBlocks++;
        if (block.isActive())
            activeBlocks++;
    }

    // 방문이 끝난 뒤 결과를 꺼냄
    public CellStatistics result() {
        return new CellStatistics(liveCells, activeBlocks, totalBlocks);
    }
}

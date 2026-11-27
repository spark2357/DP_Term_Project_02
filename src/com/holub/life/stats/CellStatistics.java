package com.holub.life.stats;

/**
 * 보드 한 순간의 통계 (StatisticsVisitor가 Cell 트리를 한 바퀴 돌아 만든 스냅샷)
 *
 * @param liveCells    살아 있는 셀 수
 * @param activeBlocks 마지막 세대 진행에서 셀이 바뀐 블록 수 (블록 건너뛰기에서 계산된 블록)
 * @param totalBlocks  셀을 직접 담은 블록(가장 안쪽 Neighborhood) 수
 */
public record CellStatistics(int liveCells, int activeBlocks, int totalBlocks) {
}

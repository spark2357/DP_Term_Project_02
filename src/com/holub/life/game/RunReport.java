package com.holub.life.game;

/**
 * 한 번의 실행을 요약한 결과 (RunRecorder.finishRun()이 만듦)
 *
 * @param generations         이번 실행에서 진행한 세대 수 (보드가 안 바뀐 세대 포함)
 * @param finalCells          실행이 끝났을 때 살아 있는 셀 수
 * @param averageLiveCells    세대마다 살아 있던 셀 수의 평균
 * @param averageActiveBlocks 세대마다 셀이 바뀐 블록 수의 평균 (블록 건너뛰기로 나머지 블록은 대부분 계산을 건너뜀)
 * @param totalBlocks         전체 블록 수
 */
public record RunReport(int generations, int finalCells, double averageLiveCells,
                        double averageActiveBlocks, int totalBlocks) {
}

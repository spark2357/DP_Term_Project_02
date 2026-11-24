package com.holub.life.command;

import com.holub.life.RunRecorder;
import com.holub.life.RunReport;
import java.util.function.Consumer;

/**
 * 이번 실행의 통계 요약을 출력하고, 다음 실행을 위해 기록을 비움
 * <p>
 * 어디에 보여 줄지는 모름: 출력 함수(output)에 글자만 넘김. 화면(Swing)은 Life.main에서 연결함
 * (지금은 창 아래 상태 표시줄. System.out::println을 넘기면 콘솔 출력이 됨)
 * 진행한 세대가 없으면 아무것도 출력하지 않음 (이전 리포트가 그대로 남음)
 */
public class ShowRunReportCommand implements Command {
    private final RunRecorder recorder;
    private final String reason;                // 멈춘 이유. 예: "사용자 정지", "멸종으로 정지"
    private final Consumer<String> output;

    public ShowRunReportCommand(RunRecorder recorder, String reason, Consumer<String> output) {
        this.recorder = recorder;
        this.reason = reason;
        this.output = output;
    }

    @Override
    public void execute() {
        recorder.finishRun().ifPresent(report -> output.accept(format(report)));
    }

    private String format(RunReport report) {
        return String.format("%s · %d세대 · 현재 %d셀 · 평균 %.1f셀 · 활성 블록 평균 %.1f/%d",
                reason,
                report.generations(),
                report.finalCells(),
                report.averageLiveCells(),
                report.averageActiveBlocks(),
                report.totalBlocks());
    }
}

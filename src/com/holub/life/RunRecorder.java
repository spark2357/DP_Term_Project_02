package com.holub.life;

import java.util.Optional;

/**
 * 한 번의 실행 동안 세대마다 통계 스냅샷을 모아 요약함 (LifeGame을 구독하는 Observer)
 * <p>
 * 실행 = 마지막 요약(finishRun) 이후 진행한 세대들. Go 메뉴의 속도 선택과 Tick(한 단계 진행)을 모두 포함함.
 * LifeGame은 보드가 바뀐 세대에만 알리므로, 세대 수는 LifeGame.generation()의 차이로 셈.
 * 알림 없이 지나간 세대(보드가 안 바뀐 세대)는 셀 수가 직전 세대와 같으므로 그 값으로 채우고,
 * 활성 블록은 0으로 평균에 반영됨
 * <p>
 * 모든 셀이 죽으면(멸종) whenExtinct로 등록한 동작을 실행함. 무엇을 할지(멈추고 리포트 출력)는 조립하는 쪽이 정함.
 * 통계 값은 화면에 보여 줄 숫자일 뿐 아니라, 멈출지 말지를 판단하는 근거로도 쓰임
 */
public final class RunRecorder implements LifeGame.Listener {
    private final LifeGame game;
    private Runnable extinctionAction = () -> { };

    private int startGeneration;        // 이번 실행이 시작된 세대 (LifeGame 기준)
    private int lastGeneration;         // 셀 수를 마지막으로 더한 세대 (이번 실행 기준)
    private int lastLiveCells;          // 마지막으로 센 셀 수
    private boolean counted;            // 이번 실행에서 셀 수를 한 번이라도 셌는지
    private long liveCellSum;
    private long activeBlockSum;

    public RunRecorder(LifeGame game) {
        this.game = game;
        reset();
        game.addGameListener(this);
    }

    // 모든 셀이 죽었을 때 할 일을 정함
    public void whenExtinct(Runnable action) {
        this.extinctionAction = action;
    }

    // 보드가 바뀐 세대마다 불림: 지금 보드의 통계를 찍어(Visitor) 누적함
    @Override
    public void boardChanged() {
        CellStatistics now = game.statistics();
        int generation = game.generation() - startGeneration;

        addUnchangedGenerations(generation - 1, counted ? lastLiveCells : now.liveCells());
        liveCellSum += now.liveCells();
        activeBlockSum += now.activeBlocks();
        lastGeneration = generation;
        lastLiveCells = now.liveCells();
        counted = true;

        if (now.liveCells() == 0)
            extinctionAction.run();
    }

    // 이번 실행을 끝내고 요약을 돌려줌. 진행한 세대가 없으면 빈 값. 다음 실행을 위해 기록을 비움
    public Optional<RunReport> finishRun() {
        int generations = game.generation() - startGeneration;
        Optional<RunReport> report = Optional.empty();
        if (generations > 0) {
            CellStatistics end = game.statistics();
            // 마지막 변화 이후 세대들. 한 번도 안 바뀌었으면(고정 패턴) 지금 셀 수가 내내 같았던 것
            addUnchangedGenerations(generations, counted ? lastLiveCells : end.liveCells());
            report = Optional.of(new RunReport(
                    generations,
                    end.liveCells(),
                    (double) liveCellSum / generations,
                    (double) activeBlockSum / generations,
                    end.totalBlocks()));
        }
        reset();
        return report;
    }

    // lastGeneration 다음 세대부터 upToGeneration까지, 보드가 안 바뀌어 알림이 없던 세대들의 셀 수를 더함
    private void addUnchangedGenerations(int upToGeneration, int liveCells) {
        if (upToGeneration > lastGeneration) {
            liveCellSum += (long) liveCells * (upToGeneration - lastGeneration);
            lastGeneration = upToGeneration;
        }
    }

    private void reset() {
        startGeneration = game.generation();
        lastGeneration = 0;
        lastLiveCells = 0;
        counted = false;
        liveCellSum = 0;
        activeBlockSum = 0;
    }
}

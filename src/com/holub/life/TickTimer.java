package com.holub.life;

/**
 * 일정 간격으로 작업을 반복 실행하는 타이머 (Adapter 패턴의 Target)
 * Clock은 이 인터페이스만 알고, 실제 타이머 라이브러리는 구현 클래스(SwingTickTimer)가 감쌈
 * <p>
 * 구현은 action을 UI 이벤트 스레드(EDT)에서 한 번에 하나씩 실행해야 함. Board는 스레드에 안전하지 않아서,
 * 다른 스레드에서 실행하면 그리기, 마우스 클릭과 같은 셀을 동시에 건드림 (TickSource 참고)
 *
 * @include /etc/license.txt
 */
public interface TickTimer {
    // millisecondsBetweenTicks 간격으로 action을 반복 실행. 첫 실행은 바로 함.
    // 이미 돌고 있으면 이전 실행을 멈추고 새 간격과 action으로 다시 시작함
    void start(int millisecondsBetweenTicks, Runnable action);

    // 반복 실행을 멈춤. 멈춘 상태에서 불러도 아무 일도 일어나지 않음
    void stop();
}

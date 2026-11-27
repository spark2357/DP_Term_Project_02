package com.holub.life.clock;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;		// not java.util.Timer: ticks must run on the Swing event thread

/**
 * TickTimer를 javax.swing.Timer로 구현한 Adapter (Adaptee: javax.swing.Timer, Client: Clock)
 * Swing을 아는 타이머 코드는 이 클래스에만 둠
 * <p>
 * javax.swing.Timer는 정해진 간격마다 할 일을 Swing의 이벤트 처리 줄(EDT)에 넣어 줌.
 * 그래서 action은 그리기, 마우스 클릭, 메뉴 동작과 같은 스레드에서 하나씩 실행됨.
 * EDT가 바쁘면 밀린 실행은 하나로 합쳐짐
 *
 * @include /etc/license.txt
 */
public class SwingTickTimer implements TickTimer {
    // [수정] 2026-11-07: java.util.Timer는 별도 스레드에서 tick을 실행해 그리기, 마우스 클릭(EDT)과 같은 셀을 동시에 건드렸음.
    // javax.swing.Timer로 바꿔 tick도 EDT의 이벤트 큐에서 하나씩 실행되게 함 (그리기, 클릭, 메뉴와 한 줄로 처리)
    // [수정] 2026-11-16: 이 Timer를 Clock에서 이 클래스로 옮김. Clock은 TickTimer 인터페이스만 앎
    private final Timer timer = new Timer(0, event -> fire());

    private Runnable action = () -> { };

    @Override
    public void start(int millisecondsBetweenTicks, Runnable action) {
        timer.stop();
        this.action = action;
        timer.setDelay(millisecondsBetweenTicks);
        timer.setInitialDelay(0);		// first tick right away, as before
        timer.start();
    }

    @Override
    public void stop() {
        timer.stop();
    }

    // 타이머가 울릴 때마다 EDT에서 불림. 메뉴가 열려 있으면 이번 실행은 미루지 않고 버림 (원본 동작과 같음)
    // [수정] 2026-11-16: 열린 메뉴 검사를 Clock.tick()에서 이곳으로 옮김. 메뉴 검사도 Swing 화면 사정이므로 Adapter가 맡음.
    // Go → Tick(한 단계 진행)은 메뉴를 클릭한 뒤 메뉴가 닫힌 상태에서 실행되므로 검사가 필요 없음
    private void fire() {
        if (!menuIsActive())
            action.run();
    }

    /** Check if any item on the menu bar has been selected.
     *  This is an incredible kluge. The problem is that Swing draws the
     *  menus on the same "canvas" as the main frame. As a consequence,
     *  displayed menus are overwritten by a running game. Moreover, Swing
     *  provides no notification on the order of "some menu item has been
     *  selected," so the only way to detect menu-bar activity is to poll
     *  it. This method does just that, and returns true if some menu
     *  is being displayed. The {@link #fire} method effectively suspends
     *  clock ticks as long as a menu is displayed, (which slows down
     *  the clock, unfortunately).
     *  <p>
     *  From a design-patterns perspective, this method demonstrates that
     *  a facade (the MenuSite) does not provide strict isolation from
     *  the underlying subsystem. I can't immagine another application
     *  that would need to know whether or not the menu bar is active,
     *  so adding this menu to the MenuSite would be "noise." Since
     *  the method does something that's of no relevance to other
     *  MenuSite users, it makes an "end run" around the facade.
     */
    private boolean menuIsActive() {
        MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
        return (path != null && path.length > 0);
    }
}

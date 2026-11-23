package com.holub.life.support;

import com.holub.life.TickTimer;

/**
 * 실제 시간을 기다리지 않는 가짜 TickTimer (테스트 더블)
 * Clock에 진짜 Swing Timer 대신 넣어서, tick을 테스트가 원할 때 직접 일으킴
 *
 *   - running                  : start() 뒤 true, stop() 뒤 false
 *   - millisecondsBetweenTicks : 마지막으로 start()에 받은 간격
 *   - fire()                   : 돌고 있으면 등록된 action을 한 번 실행 (타이머가 한 번 울린 것과 같음)
 */
public class FakeTickTimer implements TickTimer {
	public boolean running;
	public int millisecondsBetweenTicks;
	private Runnable action;

	@Override
	public void start(int millisecondsBetweenTicks, Runnable action) {
		this.millisecondsBetweenTicks = millisecondsBetweenTicks;
		this.action = action;
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	public void fire() {
		if (running) {
			action.run();
		}
	}
}

package com.holub.life.support;

import com.holub.life.TickSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 실제 시간 없이 동작하는 가짜 TickSource (테스트 더블)
 * LifeGame과 Go 메뉴 Command를 시계 없이 테스트할 때 씀
 *
 *   - running            : startTicking(ms > 0)이면 true, stop() 또는 startTicking(0)이면 false
 *   - millisecondsBetweenTicks : 마지막으로 받은 속도
 *   - tick()             : 등록된 리스너에게 tick을 한 번 바로 보냄 (진짜 Clock.tick()과 같음)
 */
public class FakeTicker implements TickSource {
	public boolean running;
	public int millisecondsBetweenTicks;
	private final List<Listener> listeners = new ArrayList<>();

	@Override
	public void startTicking(int millisecondsBetweenTicks) {
		this.millisecondsBetweenTicks = millisecondsBetweenTicks;
		running = millisecondsBetweenTicks > 0;
	}

	@Override
	public void stop() {
		startTicking(0);
	}

	@Override
	public void tick() {
		for (Listener listener : listeners) {
			listener.tick();
		}
	}

	@Override
	public void addTickListener(Listener listener) {
		listeners.add(listener);
	}
}

package com.holub.life.support;

import com.holub.life.game.LifeGame;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면 대신 LifeGame의 알림을 기록하는 가짜 리스너 (테스트 더블)
 * Command 통합 테스트에서 화면 없이 "다시 그리라는 알림이 갔는지"를 확인할 때 씀
 *
 *   - events : 받은 알림을 순서대로 모아 둠. boardChanged()는 "changed", boardEdited()는 "edited"
 */
public class RecordingGameListener implements LifeGame.Listener {
	public final List<String> events = new ArrayList<>();

	@Override
	public void boardChanged() {
		events.add("changed");
	}

	@Override
	public void boardEdited() {
		events.add("edited");
	}
}

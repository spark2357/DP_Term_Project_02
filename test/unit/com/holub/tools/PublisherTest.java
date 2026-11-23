package com.holub.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Publisher 단위 테스트: 구독자 목록을 관리하고 알림을 배달하는 Observer 도우미 (Clock, LifeGame이 씀)
 * 원본의 main() 수동 테스트(Publisher.Test)를 JUnit으로 옮긴 것
 *
 *   메서드
 *   - subscribe(subscriber)          : 구독자 추가
 *   - cancelSubscription(subscriber) : 구독자 빼기
 *   - publish(distributor)           : 구독자마다 distributor.deliverTo(구독자)를 부름
 *
 * [테스트 시나리오]
 *   1. 배달: 모든 구독자에게 한 번씩 배달됨. 나중에 구독한 쪽부터 받음
 *   2. 구독 취소: 취소한 구독자는 더 이상 받지 않음. 다른 구독자가 남아 있을 때 이미 취소한(구독하지 않은) 대상을 취소하면 NoSuchElementException
 *   3. 구독자 없음: publish()해도 아무 일 없음
 *
 * [커버리지]
 *   - PublisherTest.java 테스트 대상: subscribe, cancelSubscription, publish
 *   - 테스트 제외 대상
 *     - 여러 스레드에서 동시에 구독·배달: 게임은 모두 Swing 이벤트 스레드에서 돌아서 제외
 */
@Tag("original")
class PublisherTest {
	private final Publisher publisher = new Publisher();
	private final List<String> received = new ArrayList<>();

	@Test
	@DisplayName("publish(): 모든 구독자에게 한 번씩 배달하고, 나중에 구독한 쪽부터 받는다")
	void deliversToEverySubscriberLatestFirst() {
		// Given
		publisher.subscribe("first");
		publisher.subscribe("second");

		// When
		publisher.publish(subscriber -> received.add((String) subscriber));

		// Then
		assertEquals(List.of("second", "first"), received);
	}

	@Test
	@DisplayName("cancelSubscription(): 구독을 취소하면 더 이상 받지 않는다")
	void cancelledSubscriberReceivesNothing() {
		// Given
		publisher.subscribe("first");
		publisher.subscribe("second");

		// When
		publisher.cancelSubscription("first");
		publisher.publish(subscriber -> received.add((String) subscriber));

		// Then
		assertEquals(List.of("second"), received);
	}

	@Test
	@DisplayName("cancelSubscription(): 이미 취소한 구독자를 다시 취소하면 NoSuchElementException이 난다")
	void cancellingAgainThrows() {
		// Given: 다른 구독자가 남아 있고, first는 이미 취소함
		publisher.subscribe("first");
		publisher.subscribe("second");
		publisher.cancelSubscription("first");

		// When, Then
		assertThrows(NoSuchElementException.class, () -> publisher.cancelSubscription("first"));
	}

	@Test
	@DisplayName("publish(): 구독자가 없으면 아무 일도 일어나지 않는다")
	void publishWithoutSubscribersDoesNothing() {
		assertDoesNotThrow(() -> publisher.publish(subscriber -> received.add("unexpected")));
		assertEquals(List.of(), received);
	}
}

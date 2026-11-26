package com.holub.life.support;

import com.holub.ui.MenuSite;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;

/**
 * 화면이 필요한 테스트(gui 태그)용 헬퍼: MenuSite를 JVM에서 한 번만 준비함
 * Clock과 Universe는 생성자에서 메뉴를 만들기 때문에, 만들기 전에 MenuSite.establish(JFrame)가 필요함
 * JFrame은 메뉴바를 붙이는 데만 쓰고 화면에 띄우지 않음 (setVisible을 부르지 않음)
 * MenuSite.establish()는 JVM에서 한 번만 부를 수 있으므로 여러 테스트 클래스가 이 헬퍼를 함께 씀
 */
public final class MenuSiteSetup {
	private static boolean established = false;

	private MenuSiteSetup() {
	}

	// 화면이 없는 환경(headless)이면 JFrame을 만들 수 없으므로 true. 테스트는 이때 건너뜀
	public static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless();
	}

	public static synchronized void establishOnce() {
		if (!established) {
			MenuSite.establish(new JFrame());
			established = true;
		}
	}
}

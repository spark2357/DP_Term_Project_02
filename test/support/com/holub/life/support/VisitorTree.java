package com.holub.life.support;

import com.holub.life.model.BirthColorRule;
import com.holub.life.model.CellVisitor;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Supplier;

/**
 * StatisticsVisitor 테스트용 가짜 Cell 트리 (단위 StatisticsVisitorTest와 통합 StatisticsVisitorCellTest가 함께 씀)
 * 트리를 도는 일은 가짜 노드(Node)가 맡음. Neighborhood.accept()처럼 "블록에 들어감 → 자식 → 블록에서 나옴" 순서로 방문자를 부름
 * 방문자에게 넘기는 블록과 셀은 진짜 Neighborhood, Resident (둘 다 final 클래스이고 방문자 메서드의 인자 타입이라 가짜로 바꿀 수 없음).
 * 활성 여부와 생사만 정해 둠
 */
public final class VisitorTree{
	// 가짜 출생 색 규칙: 이 테스트들에서는 아무도 태어나지 않으므로 쓰이지 않음
	private static final Supplier<BirthColorRule> RULE = () -> parents -> { throw new AssertionError("아무도 태어나지 않아야 함"); };

	private VisitorTree(){
	}

	// Cell 트리 대신 방문자를 부르기만 하는 가짜 노드
	public interface Node{
		void accept(CellVisitor visitor);
	}

	// Neighborhood.accept()와 같은 순서: 블록에 들어감 → 자식 → 블록에서 나옴
	public static Node block(Neighborhood block, Node... children){
		return visitor -> {
			visitor.enterNeighborhood(block);
			for (Node child : children) {
				child.accept(visitor);
			}
			visitor.leaveNeighborhood(block);
		};
	}

	// Resident.accept()와 같음: 셀 하나를 알림
	public static Node cell(Resident cell){
		return visitor -> visitor.visitResident(cell);
	}

	// ---- 방문자에게 넘길 객체 (상태만 정해 둠)

	// 클릭한 블록은 활성 (Neighborhood.isActive()가 true)
	public static Neighborhood activeBlock(){
		Neighborhood block = new Neighborhood(1, new Resident(RULE));
		block.userClicked(new Point(0, 0), new Rectangle(0, 0, 1, 1));
		return block;
	}

	public static Neighborhood inactiveBlock(){
		return new Neighborhood(1, new Resident(RULE));
	}

	public static Resident liveCell(){
		Resident cell = new Resident(RULE);
		cell.userClicked(new Point(0, 0), new Rectangle(0, 0, 1, 1));
		return cell;
	}

	public static Resident deadCell(){
		return new Resident(RULE);
	}
}

package com.holub.life.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Undo/Redo 기록. Memento 패턴의 Caretaker 역할을 LifeGame과 나눠 맡음. 이 클래스는 스냅샷을 쌓아 두고
 * Undo/Redo 때 돌아갈 스냅샷을 골라 줌. 보드를 실제로 되돌리는 것은 LifeGame(board.restore())
 * undo에는 이전 스냅샷들, redo에는 Undo로 되돌린 스냅샷들을 push.
 * 새 기록이 생기면 redo는 초기화. Undo한 뒤 다른 편집을 했으면 redo 불가
 */
public class BoardHistory {
    private final int limit;
    private final Deque<BoardSnapshot> undo = new ArrayDeque<>();     // 맨 앞이 가장 최근
    private final Deque<BoardSnapshot> redo = new ArrayDeque<>();

    // limit: 스택 하나에 쌓아 둘 최대 개수. 넘치면 가장 오래된 것부터 버림
    public BoardHistory(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        this.limit = limit;
    }

    // 보드가 바뀌었을 때 호출. before는 바뀌기 전의 스냅샷
    public void record(BoardSnapshot before) {
        push(undo, before);
        redo.clear();
    }

    // 되돌릴 스냅샷. current(지금 보드)는 redo로 이동. 기록이 없으면 빈 Optional이고 아무것도 바뀌지 않음
    public Optional<BoardSnapshot> undo(BoardSnapshot current) {
        return move(undo, redo, current);
    }

    // 다시 실행할 스냅샷. current(지금 보드)는 undo로 이동. 기록이 없으면 빈 Optional이고 아무것도 바뀌지 않음
    public Optional<BoardSnapshot> redo(BoardSnapshot current) {
        return move(redo, undo, current);
    }

    private Optional<BoardSnapshot> move(Deque<BoardSnapshot> from, Deque<BoardSnapshot> to, BoardSnapshot current) {
        if (from.isEmpty()) {
            return Optional.empty();
        }
        push(to, current);
        return Optional.of(from.pop());
    }

    private void push(Deque<BoardSnapshot> stack, BoardSnapshot snapshot) {
        stack.push(snapshot);
        if (stack.size() > limit) {
            stack.removeLast();
        }
    }
}

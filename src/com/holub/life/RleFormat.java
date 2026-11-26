package com.holub.life;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RLE(Run Length Encoded) 패턴 형식. LifeWiki, Golly 등에서 쓰는 텍스트 형식
 *
 *   #N Glider          ← # 으로 시작하는 줄은 주석 (무시)
 *   #R 30 30           ← 패턴 왼쪽 위의 보드 좌표 (열, 행). 이 앱이 Store할 때 씀
 *   x = 3, y = 3       ← 패턴의 너비, 높이 (rule은 생략 가능, 생략하면 B3/S23)
 *   bo$2bo$3o!         ← b: 죽은 칸, o: 산 칸, $: 줄 끝, !: 패턴 끝, 앞의 숫자: 반복 횟수
 *
 * Load할 때 #R이 있으면 그 위치에, 없으면 보드 가운데에 놓음 (남는 한 칸은 오른쪽, 아래쪽 여백으로).
 * 보드보다 큰 패턴, #R 위치에 놓으면 보드를 벗어나는 패턴, B3/S23이 아닌 규칙, 형식 오류는 IOException
 */
public class RleFormat implements PatternFormat {
    private static final Pattern HEADER = Pattern.compile(
            "x\\s*=\\s*(\\d{1,9})\\s*,\\s*y\\s*=\\s*(\\d{1,9})(?:\\s*,\\s*rule\\s*=\\s*(\\S+))?\\s*");

    private static final Pattern POSITION = Pattern.compile("#R\\s+(-?\\d{1,9})\\s+(-?\\d{1,9})\\s*");

    // 이 앱은 B3/S23 규칙만 지원함. 같은 규칙의 다른 표기도 허용
    private static final Set<String> CONWAY_RULES = Set.of("B3/S23", "S23/B3", "23/3");

    private static final int MAX_LINE_LENGTH = 70;

    private final int widthInCells;

    public RleFormat(int widthInCells) {
        this.widthInCells = widthInCells;
    }

    @Override
    public String extension() {
        return "rle";
    }

    @Override
    public String description() {
        return "RLE Pattern Files";
    }

    // RLE는 색을 저장하지 않으므로 읽은 셀은 모두 기본색
    @Override
    public LiveCells read(InputStream in) throws IOException {
        Parsed parsed = parse(in);
        return LiveCells.of(place(parsed.pattern(), parsed.position()));
    }

    // 위치 정보(#R)는 버리고 패턴 모양만 읽음 (Stamp에서 씀)
    public CellPattern readPattern(InputStream in) throws IOException {
        return parse(in).pattern();
    }

    // 패턴을 보드에 놓은 좌표. position이 없을 때만 가운데에 놓음
    private Set<Point> place(CellPattern pattern, Point position) throws IOException {
        int left;
        int top;
        if (position == null) {
            left = (widthInCells - pattern.width()) / 2;
            top = (widthInCells - pattern.height()) / 2;
        } else {
            left = position.x;
            top = position.y;
            if (left < 0 || top < 0
                    || left + pattern.width() > widthInCells || top + pattern.height() > widthInCells) {
                throw new IOException("RLE: pattern at #R " + left + " " + top + " does not fit on the board");
            }
        }
        Set<Point> cells = new HashSet<>();
        for (Point p : pattern.cells()) {
            cells.add(new Point(p.x + left, p.y + top));
        }
        return cells;
    }

    private record Parsed(CellPattern pattern, Point position) {
    }

    private Parsed parse(InputStream in) throws IOException {
        // 스트림은 닫지 않음 (연 쪽이 닫음)
        // UTF-8은 ASCII를 포함하므로 RLE를 그대로 읽고, 주석의 비ASCII 문자도 깨지지 않음
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        Point position = null;
        String line;
        while (true) {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("RLE: header line is missing");
            }
            line = line.trim();
            if (line.startsWith("#R")) {
                Matcher matcher = POSITION.matcher(line);
                if (!matcher.matches()) {
                    throw new IOException("RLE: invalid #R line");
                }
                position = new Point(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                break;
            }
        }

        Matcher header = HEADER.matcher(line);
        if (!header.matches()) {
            throw new IOException("RLE: invalid header line");
        }
        int width = Integer.parseInt(header.group(1));
        int height = Integer.parseInt(header.group(2));
        String rule = header.group(3);

        if (width > widthInCells || height > widthInCells) {
            throw new IOException("RLE: pattern (" + width + "x" + height + ") is larger than the board");
        }
        if (rule != null && !CONWAY_RULES.contains(rule.toUpperCase())) {
            throw new IOException("RLE: unsupported rule " + rule);
        }

        return new Parsed(new CellPattern(width, height, readBody(reader, width, height)), position);
    }

    // 본문을 읽어 패턴 안의 좌표(왼쪽 위가 0,0)를 돌려줌
    private static Set<Point> readBody(BufferedReader reader, int width, int height) throws IOException {
        Set<Point> cells = new HashSet<>();
        int row = 0;
        int column = 0;
        int count = 0;
        boolean hasCount = false;

        int c;
        while ((c = reader.read()) != -1) {
            char ch = (char) c;

            if (ch >= '0' && ch <= '9') {
                count = count * 10 + (ch - '0');
                hasCount = true;
                if (count > 1_000_000) {
                    throw new IOException("RLE: run count is too large");
                }
                continue;
            }
            if (Character.isWhitespace(ch)) {
                if (hasCount) {
                    throw new IOException("RLE: whitespace between a run count and its tag");
                }
                continue;
            }

            int run = hasCount ? count : 1;
            if (hasCount && count == 0) {
                throw new IOException("RLE: run count must be positive");
            }
            count = 0;
            hasCount = false;

            switch (ch) {
                case 'b':
                    column += run;
                    break;
                case 'o':
                    if (row >= height) {
                        throw new IOException("RLE: pattern has more rows than y = " + height);
                    }
                    for (int i = 0; i < run; i++) {
                        cells.add(new Point(column + i, row));
                    }
                    column += run;
                    break;
                case '$':
                    row += run;
                    column = 0;
                    break;
                case '!':
                    if (run != 1) {
                        throw new IOException("RLE: run count before '!'");
                    }
                    return cells;
                default:
                    throw new IOException("RLE: unexpected character '" + ch + "'");
            }
            if (column > width) {
                throw new IOException("RLE: row is longer than x = " + width);
            }
        }

        // '!' 없이 파일이 끝나도 허용 (손으로 만든 파일에서 흔함)
        if (hasCount) {
            throw new IOException("RLE: run count at the end of the file");
        }
        return cells;
    }

    // RLE는 색을 저장하지 않으므로 위치만 씀
    @Override
    public void write(LiveCells liveCells, OutputStream out) throws IOException {
        Set<Point> cells = liveCells.positions();
        // 스트림은 닫지 않음 (연 쪽이 닫음). flush만 함
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        if (cells.isEmpty()) {
            writer.write("x = 0, y = 0, rule = B3/S23\n!\n");
            writer.flush();
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : cells) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        writer.write("#R " + minX + " " + minY + "\n");
        writer.write("x = " + (maxX - minX + 1) + ", y = " + (maxY - minY + 1) + ", rule = B3/S23\n");

        LineWrapper body = new LineWrapper(writer);
        int lastRow = minY;
        for (int y = minY; y <= maxY; y++) {
            boolean rowHasCells = false;
            int x = minX;
            while (x <= maxX) {
                boolean alive = cells.contains(new Point(x, y));
                int run = 0;
                while (x <= maxX && cells.contains(new Point(x, y)) == alive) {
                    run++;
                    x++;
                }
                if (!alive && x > maxX) {
                    break;                      // 줄 끝의 죽은 칸은 생략
                }
                if (!rowHasCells && y > lastRow) {
                    body.add(token(y - lastRow, '$'));  // 빈 줄은 n$ 하나로 합침
                    lastRow = y;
                }
                rowHasCells = true;
                body.add(token(run, alive ? 'o' : 'b'));
            }
        }
        body.add("!");
        body.finish();
        writer.flush();
    }

    private static String token(int run, char tag) {
        return run == 1 ? String.valueOf(tag) : run + String.valueOf(tag);
    }

    // 본문을 한 줄 70자 이하로 나눠 씀. 토큰(예: 12o)은 중간에서 자르지 않음
    private static class LineWrapper {
        private final Writer writer;
        private int lineLength = 0;

        LineWrapper(Writer writer) {
            this.writer = writer;
        }

        void add(String token) throws IOException {
            if (lineLength + token.length() > MAX_LINE_LENGTH) {
                writer.write('\n');
                lineLength = 0;
            }
            writer.write(token);
            lineLength += token.length();
        }

        void finish() throws IOException {
            writer.write('\n');
        }
    }
}

package com.holub.life.brush;

import com.holub.life.BirthColorRule;
import com.holub.life.BlendRule;
import com.holub.life.CellColor;
import com.holub.life.ClashRule;
import com.holub.life.LifeGame;
import com.holub.life.mouse.Pen;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;

// [수정] 2026-11-22: 셀 색 기능. 펜 버튼(아래 막대가 지금 색)을 누르면 팔레트가 열림.
// 팔레트 위 Clash / Blend로 출생 색 규칙(BirthColorRule)을 바꿈
public class BrushToolBar extends JToolBar{
    public static final List<CellColor> PALETTE = List.of(
            new CellColor(255, 255, 0), new CellColor(0, 255, 0), new CellColor(0, 255, 255),
            new CellColor(255, 0, 255), new CellColor(0, 0, 255),
            CellColor.DEFAULT, new CellColor(0, 0, 128), new CellColor(0, 128, 128),
            new CellColor(0, 128, 0), new CellColor(128, 0, 128),
            new CellColor(128, 0, 0), new CellColor(128, 128, 0), new CellColor(128, 128, 128),
            new CellColor(192, 192, 192), new CellColor(0, 0, 0));

    private static final int COLUMNS = 5;
    private static final Border SELECTED = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
    private static final Border UNSELECTED = BorderFactory.createEmptyBorder(2, 2, 2, 2);

    private final LifeGame game;
    private final Pen pen;
    private final JButton penButton = new JButton(new PenIcon());
    private final JPopupMenu palette = new JPopupMenu();
    private final List<JButton> swatches = new ArrayList<>();

    public BrushToolBar(LifeGame game, Pen pen){
        this.game = game;
        this.pen = pen;
        setFloatable(false);

        penButton.setFocusable(false);
        penButton.addActionListener(event -> {
            showSelection();
            palette.show(penButton, 0, penButton.getHeight());
        });

        palette.add(createPalettePanel());
        add(penButton);
    }

    private JPanel createPalettePanel(){
        JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
        for (CellColor color : PALETTE){
            JButton swatch = new JButton(new SwatchIcon(color));
            swatch.setToolTipText("RGB " + color.red() + ", " + color.green() + ", " + color.blue());
            swatch.setFocusable(false);
            swatch.setContentAreaFilled(false);
            swatch.addActionListener(event -> choose(() -> pen.setColor(color)));
            swatches.add(swatch);
            grid.add(swatch);
        }

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(createRulePanel(), BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRulePanel(){
        JPanel rules = new JPanel(new GridLayout(1, 2, 4, 0));
        ButtonGroup group = new ButtonGroup();
        addRule(rules, group, "Clash", new ClashRule());
        addRule(rules, group, "Blend", new BlendRule());
        return rules;
    }

    private void addRule(JPanel rules, ButtonGroup group, String name, BirthColorRule rule){
        // [수정] 2026-11-24: 시작할 때는 Board의 지금 규칙을 읽어 버튼만 표시하고(Board → 툴바), 누를 때만 Board로 보냄(툴바 → Board)
        boolean selected = rule.getClass() == game.birthColorRule().getClass();
        JToggleButton button = new JToggleButton(name, selected);
        button.setFocusable(false);
        button.addActionListener(event -> game.setBirthColorRule(rule));
        group.add(button);
        rules.add(button);
    }

    private void choose(Runnable selection){
        selection.run();
        palette.setVisible(false);
        penButton.repaint();
    }

    private void showSelection(){
        for (int i = 0; i < swatches.size(); ++i){
            boolean selected = PALETTE.get(i).equals(pen.getColor());
            swatches.get(i).setBorder(selected ? SELECTED : UNSELECTED);
        }
    }

    private static Color toAwt(CellColor color){
        return new Color(color.red(), color.green(), color.blue());
    }

    private class PenIcon implements Icon{
        private static final int SIZE = 24;
        private static final String PEN = "\u270E";
        private static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 19);

        public int getIconWidth(){
            return SIZE;
        }

        public int getIconHeight(){
            return SIZE;
        }

        public void paintIcon(Component c, Graphics graphics, int x, int y){
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(x, y);
            g.setColor(c.isEnabled() ? new Color(70, 70, 70) : Color.LIGHT_GRAY);

            g.setFont(FONT);
            int width = g.getFontMetrics().stringWidth(PEN);
            g.drawString(PEN, (SIZE - width) / 2, 17);

            g.setColor(toAwt(pen.getColor()));
            g.fillRect(2, 20, SIZE - 4, 4);
            g.dispose();
        }
    }

    private static class SwatchIcon implements Icon{
        private static final int SIZE = 22;
        private final Color color;

        SwatchIcon(CellColor color){
            this.color = toAwt(color);
        }

        public int getIconWidth(){
            return SIZE;
        }

        public int getIconHeight(){
            return SIZE;
        }

        public void paintIcon(Component c, Graphics g, int x, int y){
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.GRAY);
            g.drawRect(x, y, SIZE - 1, SIZE - 1);
        }
    }
}

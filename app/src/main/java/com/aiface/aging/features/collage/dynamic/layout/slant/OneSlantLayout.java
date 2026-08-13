package com.aiface.aging.features.collage.dynamic.layout.slant;


import com.aiface.aging.features.collage.dynamic.puzzle.Line;

/**
 * @author wupanjie
 */

public class OneSlantLayout extends NumberSlantLayout {
    public OneSlantLayout(int theme) {
        super(theme);
    }

    @Override
    public int getThemeCount() {
        return 4;
    }

    @Override
    public void layout() {
        switch (theme) {
            case 0:
                addLine(0, Line.Direction.HORIZONTAL, 1.56f, 2.44f);
                break;
            case 1:
                addLine(0, Line.Direction.VERTICAL, 0.56f, 0.44f);
                break;
            case 2:
                addCross(0, 0.56f, 0.44f, 0.56f, 0.44f);
                break;
            case 3:
                cutArea(0, 1, 2);
                break;
        }
    }
}

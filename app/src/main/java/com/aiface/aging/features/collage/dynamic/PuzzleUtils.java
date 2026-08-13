package com.aiface.aging.features.collage.dynamic;



import com.aiface.aging.features.collage.dynamic.layout.slant.OneSlantLayout;
import com.aiface.aging.features.collage.dynamic.layout.slant.SlantLayoutHelper;
import com.aiface.aging.features.collage.dynamic.layout.slant.ThreeSlantLayout;
import com.aiface.aging.features.collage.dynamic.layout.slant.TwoSlantLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.EightStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.FiveStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.FourStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.NineStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.OneStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.SevenStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.SixStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.StraightLayoutHelper;
import com.aiface.aging.features.collage.dynamic.layout.straight.ThreeStraightLayout;
import com.aiface.aging.features.collage.dynamic.layout.straight.TwoStraightLayout;
import com.aiface.aging.features.collage.dynamic.puzzle.PuzzleLayout;

import java.util.ArrayList;
import java.util.List;



/**
 * @author wupanjie
 */
public class PuzzleUtils {
    private static final String TAG = "PuzzleUtils";

    private PuzzleUtils() {
        //no instance
    }

    public static PuzzleLayout getPuzzleLayout(int type, int borderSize, int themeId) {
        ///////////SlantLine Type
        if (type == 0) {
            switch (borderSize) {
                case 2:
                    return new TwoSlantLayout(themeId);
                case 3:
                    return new ThreeSlantLayout(themeId);
                case 1:
                default:
                    return new OneSlantLayout(themeId);
            }
            ///////////StraightLine Type
        } else {
            switch (borderSize) {
                case 2:
                    return new TwoStraightLayout(themeId);
                case 3:
                    return new ThreeStraightLayout(themeId);
                case 4:
                    return new FourStraightLayout(themeId);
                case 5:
                    return new FiveStraightLayout(themeId);
                case 6:
                    return new SixStraightLayout(themeId);
                case 7:
                    return new SevenStraightLayout(themeId);
                case 8:
                    return new EightStraightLayout(themeId);
                case 9:
                    return new NineStraightLayout(themeId);
                case 1:
                default:
                    return new OneStraightLayout(themeId);
            }
        }
    }

    public static List<PuzzleLayout> getAllPuzzleLayouts() {
        List<PuzzleLayout> puzzleLayouts = new ArrayList<>();
        //slant layout
        puzzleLayouts.addAll(SlantLayoutHelper.getAllThemeLayout(2));
        puzzleLayouts.addAll(SlantLayoutHelper.getAllThemeLayout(3));

        // straight layout
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(2));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(3));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(4));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(5));
        //puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(6));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(7));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(8));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(9));
        return puzzleLayouts;
    }

    public static List<PuzzleLayout> getPuzzleLayouts(int pieceCount) {
        List<PuzzleLayout> puzzleLayouts = new ArrayList<>();
        puzzleLayouts.addAll(SlantLayoutHelper.getAllThemeLayout(pieceCount));
        puzzleLayouts.addAll(StraightLayoutHelper.getAllThemeLayout(pieceCount));
        return puzzleLayouts;
    }
}

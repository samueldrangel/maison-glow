package com.maisonglow.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.util.Objects;

/**
 * The logo of the sidebar while the bar opens or closes: the isotipo fades out
 * while the full logo grows from the isotipo's size, and the space they take
 * follows the width of the bar, so nothing below the logo jumps.
 *
 * <p>It sets plain node properties (opacity, scale, height) on every frame, which is
 * cheap because it does not touch the CSS. For that, the rules that hide one of the
 * two logos in styles.css only apply while the bar has the class {@code css-motion}
 * (what Scene Builder shows); the controller removes it when the application starts.
 */
public final class SidebarLogo {

    /** The isotipo is gone once the bar is this far (0..1) from collapsed to expanded. */
    private static final double MARK_GONE_AT = 0.5;
    /** The full logo starts to appear once the bar is this far along. */
    private static final double FULL_VISIBLE_FROM = 0.25;

    private final Region slot;
    private final Node full;
    private final Node mark;
    private final ImageView fullImage;
    private final ImageView markImage;

    /**
     * Wraps the {@code logo-slot} of the FXML.
     *
     * @param slot the container that holds the {@code logo-full} and {@code logo-mark} wrappers
     */
    public SidebarLogo(Region slot) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.full = Objects.requireNonNull(slot.lookup(".logo-full"), "logo-full");
        this.mark = Objects.requireNonNull(slot.lookup(".logo-mark"), "logo-mark");
        this.fullImage = (ImageView) full.lookup(".image-view");
        this.markImage = (ImageView) mark.lookup(".image-view");
    }

    /**
     * Draws the logo for a moment of the motion.
     *
     * @param progress 0 with the bar collapsed, 1 with the bar expanded
     */
    public void update(double progress) {
        double p = Math.max(0, Math.min(1, progress));
        double markHeight = markImage.getLayoutBounds().getHeight();
        double fullHeight = fullImage.getLayoutBounds().getHeight();
        double startScale = markImage.getFitWidth() / fullImage.getFitWidth();

        double fullScale = startScale + (1 - startScale) * smooth(p);
        double markScale = 1 + 0.25 * smooth(p / MARK_GONE_AT);

        Insets padding = slot.getPadding();
        double height = markHeight + (fullHeight - markHeight) * smooth(p) + padding.getTop() + padding.getBottom();
        slot.setMinHeight(height);
        slot.setPrefHeight(height);
        slot.setMaxHeight(height);

        full.setVisible(true);
        full.setOpacity(smooth((p - FULL_VISIBLE_FROM) / (1 - FULL_VISIBLE_FROM)));
        full.setScaleX(fullScale);
        full.setScaleY(fullScale);
        mark.setVisible(true);
        mark.setOpacity(1 - smooth(p / MARK_GONE_AT));
        mark.setScaleX(markScale);
        mark.setScaleY(markScale);
    }

    private static double smooth(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }
}

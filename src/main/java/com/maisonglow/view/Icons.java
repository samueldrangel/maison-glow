package com.maisonglow.view;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.util.Objects;

/**
 * The line icons of the interface, drawn as vector paths so they take the colour
 * of the CSS ({@code .line-icon} in styles.css) instead of being bitmaps. They
 * share the stroke of the menu icons, so the whole application looks drawn by
 * the same hand.
 */
public final class Icons {

    /** The icons that the dialogs, notices and fields use. */
    public enum Kind {
        WARNING("M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01"),
        ERROR("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M15 9l-6 6 M9 9l6 6"),
        INFO("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 16v-4 M12 8h.01"),
        QUESTION("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3 M12 17h.01"),
        SUCCESS("M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3"),
        ALERT_CIRCLE("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 8v4 M12 16h.01"),
        SPARKLE("M12 2l2.4 7.6L22 12l-7.6 2.4L12 22l-2.4-7.6L2 12l7.6-2.4z");

        private final String path;

        Kind(String path) {
            this.path = path;
        }
    }

    private static final double MEDALLION_ICON_SCALE = 1.1;
    private static final double ICON_BOX = 24;

    private Icons() {
    }

    /**
     * @param kind which icon
     * @return the icon as a 24 x 24 vector, coloured by {@code .line-icon}
     */
    public static SVGPath line(Kind kind) {
        SVGPath icon = new SVGPath();
        icon.setContent(Objects.requireNonNull(kind, "kind").path);
        icon.getStyleClass().add("line-icon");
        return icon;
    }

    /**
     * A small icon for next to a line of text, such as the error under a field.
     *
     * @param kind which icon
     * @param size width and height it takes, in pixels
     * @return the icon in a box of that size
     */
    public static StackPane small(Kind kind, double size) {
        SVGPath icon = line(kind);
        icon.setScaleX(size / ICON_BOX);
        icon.setScaleY(size / ICON_BOX);
        StackPane box = new StackPane(icon);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    /**
     * The four-point star of the brand, for the main action of a screen. A button that has it as its
     * graphic makes it twinkle while the cursor is over the button ({@link Animations#lift}).
     *
     * @return the star in a small box, with the style class {@code sparkle}
     */
    public static StackPane sparkle() {
        StackPane star = small(Kind.SPARKLE, 15);
        star.getStyleClass().add("sparkle");
        return star;
    }

    /**
     * The round badge of a dialog: a ring and the icon of its kind, tinted by the tone of the dialog.
     *
     * @param type the kind of dialog
     * @return the badge
     */
    public static StackPane medallion(AlertType type) {
        Objects.requireNonNull(type, "type");
        SVGPath icon = line(kindOf(type));
        icon.setScaleX(MEDALLION_ICON_SCALE);
        icon.setScaleY(MEDALLION_ICON_SCALE);
        StackPane badge = new StackPane(icon);
        badge.getStyleClass().addAll("dialog-medallion", toneOf(type));
        return badge;
    }

    /** @return the icon that goes with a kind of dialog */
    static Kind kindOf(AlertType type) {
        return switch (type) {
            case ERROR -> Kind.ERROR;
            case WARNING -> Kind.WARNING;
            case CONFIRMATION -> Kind.QUESTION;
            case INFORMATION, NONE -> Kind.INFO;
        };
    }

    /** @return the style class that tints a dialog of this kind */
    static String toneOf(AlertType type) {
        return switch (type) {
            case ERROR -> "tone-error";
            case WARNING -> "tone-warning";
            case CONFIRMATION -> "tone-question";
            case INFORMATION, NONE -> "tone-info";
        };
    }
}

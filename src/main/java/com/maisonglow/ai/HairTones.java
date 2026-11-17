package com.maisonglow.ai;

import java.util.List;
import java.util.Locale;

/** One palette shared by the UI, validation and both image providers. */
public final class HairTones {
    public record Tone(String name, String hex, String swatch) { }
    private HairTones() { }
    public static final List<Tone> ALL = List.of(
        new Tone("castaño oscuro", "#30201b", "01"), new Tone("castaño chocolate", "#533526", "02"),
        new Tone("castaño claro", "#83624a", "03"), new Tone("rubio dorado", "#c4a078", "04"),
        new Tone("rubio platino", "#ded0b8", "05"), new Tone("negro natural", "#171515", "06"),
        new Tone("castaño ceniza", "#493b35", "07"), new Tone("rubio oscuro", "#8b705c", "08"),
        new Tone("cobrizo", "#995332", "09"), new Tone("rojo cobrizo", "#b5523b", "10"),
        new Tone("caoba", "#783622", "11"), new Tone("borgoña", "#651c29", "12"),
        new Tone("ciruela", "#58283e", "13"), new Tone("rosa intenso", "#ab4a75", "14"),
        new Tone("rosa pastel", "#c58c8c", "15"));
    public static List<String> names() { return ALL.stream().map(Tone::name).toList(); }
    public static Tone find(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(tone -> tone.name().equals(normalized)).findFirst().orElseThrow();
    }
}

package com.portwallet.shared.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Languages the product speaks (see docs/features/nfr-i18n). {@link #PT_BR} is the default and the fallback. */
public enum Language {

    PT_BR(Locale.of("pt", "BR")),
    EN(Locale.ENGLISH),
    ES(Locale.of("es")),
    FR(Locale.FRENCH);

    public static final Language DEFAULT = PT_BR;

    private final Locale locale;

    Language(Locale locale) {
        this.locale = locale;
    }

    public Locale locale() {
        return locale;
    }

    /** BCP 47 tag, as used by the frontend and in {@code Accept-Language} ({@code pt-BR}, {@code en}...). */
    public String tag() {
        return locale.toLanguageTag();
    }

    /** The language for a locale: exact match first, then the same language in any region, then the default. */
    public static Language of(Locale locale) {
        if (locale == null) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(language -> language.locale.equals(locale))
                .findFirst()
                .or(() -> Arrays.stream(values())
                        .filter(language -> language.locale.getLanguage().equals(locale.getLanguage()))
                        .findFirst())
                .orElse(DEFAULT);
    }

    public static List<Locale> supportedLocales() {
        return Arrays.stream(values()).map(Language::locale).toList();
    }
}

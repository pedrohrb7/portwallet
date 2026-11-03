package com.portwallet.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.portwallet.shared.error.Problems;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The message bundles are complete and safe for MessageFormat in every language. */
class MessageBundlesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    @ParameterizedTest
    @ValueSource(strings = {"en", "es", "fr"})
    void hasTheSameKeysAsTheBaseBundle(String language) throws IOException {
        assertThat(bundle("messages_" + language).stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(bundle("messages").stringPropertyNames());
    }

    @ParameterizedTest
    @ValueSource(strings = {"messages", "messages_en", "messages_es", "messages_fr"})
    void usesTheSamePlaceholdersAsTheBaseBundleAndNoAsciiApostrophe(String name) throws IOException {
        Properties base = bundle("messages");
        Properties translated = bundle(name);
        for (String key : base.stringPropertyNames()) {
            String text = translated.getProperty(key);
            assertThat(text).as(key).isNotBlank().doesNotContain("'");
            assertThat(placeholders(text)).as(key).isEqualTo(placeholders(base.getProperty(key)));
        }
    }

    @Test
    void frenchPunctuationUsesANoBreakSpace() throws IOException {
        bundle("messages_fr").forEach((key, text) ->
                assertThat((String) text).as((String) key).doesNotContainPattern(" [:!?;]"));
    }

    @Test
    void everyGenericProblemTypeHasATitle() throws IOException {
        Properties base = bundle("messages");
        Arrays.stream(Problems.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .map(MessageBundlesTest::value)
                .forEach(type -> assertThat(base).containsKey("problem." + type + ".title"));
    }

    private static Properties bundle(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MessageBundlesTest.class.getResourceAsStream("/" + name + ".properties")) {
            assertThat(in).as(name).isNotNull();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static List<String> placeholders(String text) {
        return PLACEHOLDER.matcher(text).results().map(MatchResult::group).sorted().toList();
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.portwallet.shared.error;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Title of each problem {@code type}, from the {@code problem.<type>.title} messages. */
@Component
public class ProblemTitles {

    private final MessageSource messages;

    ProblemTitles(MessageSource messages) {
        this.messages = messages;
    }

    /** The title of a type in a language; falls back to the base bundle (pt-BR). Null for a type not in it. */
    public String of(String type, Locale locale) {
        return messages.getMessage("problem." + type + ".title", null, null, locale);
    }

    /** Sets the title of a problem from its type. Leaves it untouched for a type without a title. */
    public void apply(ProblemDetail problem, Locale locale) {
        if (problem.getType() == null) {
            return;
        }
        String title = of(problem.getType().toString(), locale);
        if (title != null) {
            problem.setTitle(title);
        }
    }
}

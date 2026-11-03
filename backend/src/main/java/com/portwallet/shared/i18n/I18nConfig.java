package com.portwallet.shared.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Language of each request, from {@code Accept-Language}: an exact supported locale first, then a supported language
 * without region ({@code en-US} answers in {@code en}), then {@link Language#DEFAULT}, also used without the header.
 */
@Configuration(proxyBeanMethods = false)
public class I18nConfig {

    /** Named {@code localeResolver} so it replaces the one Spring MVC would create. */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(Language.supportedLocales());
        resolver.setDefaultLocale(Language.DEFAULT.locale());
        return resolver;
    }
}

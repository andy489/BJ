package com.casino.blackjack.model.enumerated;


import lombok.Getter;

@Getter
public enum LocaleEnum {
    EN("en_US", "EN"),
    BG("bg_BG", "BG"),
    DE("de_DE", "DE"),
    IT("it_IT", "IT"),
    ES("es_ES", "ES"),
    RU("ru_RU", "RU"),
    ZH("zh_CN", "ZH");

    private final String baseLocaleId;
    private final String displayName;

    LocaleEnum(String baseLocaleId, String displayName) {
        this.baseLocaleId = baseLocaleId;
        this.displayName = displayName;
    }
}

package com.seaurl.platform.space.service;

public interface TranslatorService {

    String startTranslation(String sourceLanguage, String targetLanguage, String sourceTxt);

    String detectLanguage(String sourceTxt);
}

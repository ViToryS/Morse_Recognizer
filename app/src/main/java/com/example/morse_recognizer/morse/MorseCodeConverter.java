package com.example.morse_recognizer.morse;

import android.util.Log;

import java.util.Map;
import java.util.Objects;

public class MorseCodeConverter {
    private static Language currentLanguage = Language.RUSSIAN;

    public enum Language {
        RUSSIAN("ru-RU", "Рус", MorseCodeMaps.RUSSIAN_TO_MORSE, MorseCodeMaps.MORSE_TO_RUSSIAN),
        ENGLISH("en-US", "Анг", MorseCodeMaps.ENGLISH_TO_MORSE, MorseCodeMaps.MORSE_TO_ENGLISH);

        private final String ttsCode;
        private final String buttonText;
        final Map<Character, String> toMorseMap;
        final Map<String, Character> fromMorseMap;

        Language(String ttsCode, String buttonText, Map<Character, String> toMorse, Map<String, Character> fromMorse) {
            this.ttsCode = ttsCode;
            this.toMorseMap = toMorse;
            this.fromMorseMap = fromMorse;
            this.buttonText = buttonText;

        }
        public String getTtsCode() {
            return ttsCode;
        }
        public String getButtonText() {
            return buttonText;
        }
        public static Language getNext(Language current) {
            Language[] languages = values();
            int nextIndex = (current.ordinal() + 1) % languages.length;
            return languages[nextIndex];
        }
    }


    public static void setLanguage(Language language) {
        currentLanguage = language;
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }


    public static String convertToMorse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder morseCode = new StringBuilder();
        String upperText = text.toUpperCase();
        for (char c : upperText.toCharArray())  {
            String code = currentLanguage.toMorseMap.get(c);
            morseCode.append(Objects.requireNonNullElse(code, "*")).append(" ");
        Log.d("MORSE", "Message: " + morseCode);
        }
        return morseCode.toString().trim();
    }

    public static String convertFromMorse(String morseText) {
        if (morseText == null || morseText.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String[] morseCodes = morseText.split(" {2}");

        for (String code : morseCodes) {
            if (code.isEmpty()) {
                continue;
            }
            Character character = currentLanguage.fromMorseMap.get(code);
            if (character != null) {
                result.append(character);
            } else {
                result.append("*");
            }
        }
        return result.toString().toLowerCase();
    }
}

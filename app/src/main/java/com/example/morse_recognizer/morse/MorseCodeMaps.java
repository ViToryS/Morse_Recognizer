package com.example.morse_recognizer.morse;

import java.util.HashMap;
import java.util.Map;

public class MorseCodeMaps {
    public static final Map<Character, String> RUSSIAN_TO_MORSE = new HashMap<>();
    public static final Map<String, Character> MORSE_TO_RUSSIAN = new HashMap<>();

    public static final Map<Character, String> ENGLISH_TO_MORSE = new HashMap<>();
    public static final Map<String, Character> MORSE_TO_ENGLISH = new HashMap<>();

    static {
        addRussianPair('А', ".-");
        addRussianPair('Б', "-...");
        addRussianPair('В', ".--");
        addRussianPair('Г', "--.");
        addRussianPair('Д', "-..");
        addRussianPair('Е', ".");
        addRussianPair('Ж', "...-");
        addRussianPair('З', "--..");
        addRussianPair('И', "..");
        addRussianPair('Й', ".---");
        addRussianPair('К', "-.-");
        addRussianPair('Л', ".-..");
        addRussianPair('М', "--");
        addRussianPair('Н', "-.");
        addRussianPair('О', "---");
        addRussianPair('П', ".--.");
        addRussianPair('Р', ".-.");
        addRussianPair('С', "...");
        addRussianPair('Т', "-");
        addRussianPair('У', "..-");
        addRussianPair('Ф', "..-.");
        addRussianPair('Х', "....");
        addRussianPair('Ц', "-.-.");
        addRussianPair('Ч', "---.");
        addRussianPair('Ш', "----");
        addRussianPair('Щ', "--.-");
        addRussianPair('Ъ', "--.--");
        addRussianPair('Ы', "-.--");
        addRussianPair('Ь', "-..-");
        addRussianPair('Э', "..-..");
        addRussianPair('Ю', "..--");
        addRussianPair('Я', ".-.-");

        addEnglishPair('A', ".-");
        addEnglishPair('B', "-...");
        addEnglishPair('C', "-.-.");
        addEnglishPair('D', "-..");
        addEnglishPair('E', ".");
        addEnglishPair('F', "..-.");
        addEnglishPair('G', "--.");
        addEnglishPair('H', "....");
        addEnglishPair('I', "..");
        addEnglishPair('J', ".---");
        addEnglishPair('K', "-.-");
        addEnglishPair('L', ".-..");
        addEnglishPair('M', "--");
        addEnglishPair('N', "-.");
        addEnglishPair('O', "---");
        addEnglishPair('P', ".--.");
        addEnglishPair('Q', "--.-");
        addEnglishPair('R', ".-.");
        addEnglishPair('S', "...");
        addEnglishPair('T', "-");
        addEnglishPair('U', "..-");
        addEnglishPair('V', "...-");
        addEnglishPair('W', ".--");
        addEnglishPair('X', "-..-");
        addEnglishPair('Y', "-.--");
        addEnglishPair('Z', "--..");

        addGeneralPair('0', "-----");
        addGeneralPair('1', ".----");
        addGeneralPair('2', "..---");
        addGeneralPair('3', "...--");
        addGeneralPair('4', "....-");
        addGeneralPair('5', ".....");
        addGeneralPair('6', "-....");
        addGeneralPair('7', "--...");
        addGeneralPair('8', "---..");
        addGeneralPair('9', "----.");
        addGeneralPair('.', "......");
        addGeneralPair(',', ".-.-.-");
        addGeneralPair('?', "..--..");
        addGeneralPair('!', "--..--");
        addGeneralPair('-', "-....-");
        addGeneralPair(' ', "+");
    }

    private static void addRussianPair(Character ch, String morse) {
        RUSSIAN_TO_MORSE.put(ch, morse);
        MORSE_TO_RUSSIAN.put(morse, ch);
    }

    private static void addEnglishPair(Character ch, String morse) {
        ENGLISH_TO_MORSE.put(ch, morse);
        MORSE_TO_ENGLISH.put(morse, ch);
    }
    private static void addGeneralPair(Character ch, String morse) {
        ENGLISH_TO_MORSE.put(ch, morse);
        MORSE_TO_ENGLISH.put(morse, ch);
        RUSSIAN_TO_MORSE.put(ch, morse);
        MORSE_TO_RUSSIAN.put(morse, ch);
    }
}
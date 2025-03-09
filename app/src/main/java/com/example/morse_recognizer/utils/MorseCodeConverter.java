package com.example.morse_recognizer.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class MorseCodeConverter {

    private static final Map<Character, String> morseCodeMap = new HashMap<>();

    static {
        morseCodeMap.put('А', ".-");
        morseCodeMap.put('Б', "-...");
        morseCodeMap.put('В', ".--");
        morseCodeMap.put('Г', "--.");
        morseCodeMap.put('Д', "-..");
        morseCodeMap.put('Е', ".");
        morseCodeMap.put('Ж', "...-");
        morseCodeMap.put('З', "--..");
        morseCodeMap.put('И', "..");
        morseCodeMap.put('Й', ".---");
        morseCodeMap.put('К', "-.-");
        morseCodeMap.put('Л', ".-..");
        morseCodeMap.put('М', "--");
        morseCodeMap.put('Н', "-.");
        morseCodeMap.put('О', "---");
        morseCodeMap.put('П', ".--.");
        morseCodeMap.put('Р', ".-.");
        morseCodeMap.put('С', "...");
        morseCodeMap.put('Т', "-");
        morseCodeMap.put('У', "..-");
        morseCodeMap.put('Ф', "..-.");
        morseCodeMap.put('Х', "....");
        morseCodeMap.put('Ц', "-.-.");
        morseCodeMap.put('Ч', "---.");
        morseCodeMap.put('Ш', "----");
        morseCodeMap.put('Щ', "--.-");
        morseCodeMap.put('Ъ', "--.--");
        morseCodeMap.put('Ы', "-.--");
        morseCodeMap.put('Ь', "-..-");
        morseCodeMap.put('Э', "..-..");
        morseCodeMap.put('Ю', "..--");
        morseCodeMap.put('Я', ".-.-");


        morseCodeMap.put('0', "-----");
        morseCodeMap.put('1', ".----");
        morseCodeMap.put('2', "..---");
        morseCodeMap.put('3', "...--");
        morseCodeMap.put('4', "....-");
        morseCodeMap.put('5', ".....");
        morseCodeMap.put('6', "-....");
        morseCodeMap.put('7', "--...");
        morseCodeMap.put('8', "---..");
        morseCodeMap.put('9', "----.");

        morseCodeMap.put('.', "......");
        morseCodeMap.put(',', ".-.-.-");
        morseCodeMap.put('?', "..--..");
        morseCodeMap.put('!', "--..--");
        morseCodeMap.put('-', "-....-");

        morseCodeMap.put(' ', "+");
}


    public static String convertToMorse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder morseCode = new StringBuilder();
        String upperText = text.toUpperCase();
        for (char c : upperText.toCharArray())  {

            if (morseCodeMap.containsKey(c)) {
                morseCode.append(morseCodeMap.get(c)).append(" ");

            }
        Log.d("MORSE", "Message: " + morseCode);
        }
        return morseCode.toString().trim();
    }
}

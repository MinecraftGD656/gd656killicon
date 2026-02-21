package org.mods.gd656killicon.client.render.effect;

import java.util.Random;

/**
 * TextScrambleEffect provides a "scrambling" or "scrolling" animation for text.
 * It replaces characters with random ones of similar type before revealing the target character.
 */
public class TextScrambleEffect {
    private static final String SCRAMBLE_LATIN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SCRAMBLE_NUMERIC = "0123456789";
    private static final String SCRAMBLE_CJK = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年样能下部动方出进面家种已心";
    private static final Random RANDOM = new Random();

    private final String target;
    private final boolean[] shouldScramble;
    private final long startTime;
    private final long duration;
    private final long refreshRate; // New: frequency of character changes
    private final boolean active;
    
    // Cache for currently displayed random characters
    private final char[] cachedChars;
    private long lastRefreshTime = 0;

    /**
     * Creates a new scramble effect.
     *
     * @param text        The final text to display.
     * @param duration    The animation duration in milliseconds.
     * @param refreshRate The frequency of character changes in milliseconds.
     * @param active      Whether the effect is enabled.
     */
    public TextScrambleEffect(String text, long duration, long refreshRate, boolean active) {
        this.target = text;
        this.duration = duration;
        this.refreshRate = refreshRate;
        this.active = active;
        this.startTime = System.currentTimeMillis();
        this.shouldScramble = new boolean[text.length()];
        this.cachedChars = new char[text.length()];

        if (active) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                shouldScramble[i] = isScrambleable(c);
                if (shouldScramble[i]) {
                    cachedChars[i] = getRandomCharLike(c);
                }
            }
        }
    }

    private boolean isScrambleable(char c) {
        if (Character.isWhitespace(c)) return false;
        if (Character.isDigit(c)) return false;
        // Basic symbols that shouldn't scramble
        if (c == '+' || c == '-' || c == '.' || c == ',' || c == '(' || c == ')' || c == ':' || c == '|') return false;
        return true;
    }

    /**
     * Gets the current state of the scrambled text.
     *
     * @return The current string to display.
     */
    public String getCurrentText() {
        if (!active) return target;

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        float progress = Math.min(1.0f, (float) elapsed / duration);

        if (progress >= 1.0f) return target;

        // Check if we need to refresh the random characters based on refreshRate
        boolean shouldUpdateCache = (now - lastRefreshTime) >= refreshRate;
        if (shouldUpdateCache) {
            lastRefreshTime = now;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < target.length(); i++) {
            if (!shouldScramble[i]) {
                sb.append(target.charAt(i));
                continue;
            }

            // Staggered reveal based on character position
            float charRevealStart = (float) i / target.length() * 0.4f; 
            float charRevealDuration = 0.6f; 
            float charProgress = Math.max(0, Math.min(1.0f, (progress - charRevealStart) / charRevealDuration));

            if (charProgress >= 1.0f) {
                sb.append(target.charAt(i));
            } else {
                if (shouldUpdateCache) {
                    cachedChars[i] = getRandomCharLike(target.charAt(i));
                }
                sb.append(cachedChars[i]);
            }
        }
        return sb.toString();
    }

    private char getRandomCharLike(char original) {
        // CJK Unified Ideographs
        if (original >= 0x4E00 && original <= 0x9FFF) {
            return SCRAMBLE_CJK.charAt(RANDOM.nextInt(SCRAMBLE_CJK.length()));
        }
        // Latin letters
        if ((original >= 'a' && original <= 'z') || (original >= 'A' && original <= 'Z')) {
            return SCRAMBLE_LATIN.charAt(RANDOM.nextInt(SCRAMBLE_LATIN.length()));
        }
        // Digits (though we usually don't scramble them, handle just in case)
        if (original >= '0' && original <= '9') {
            return SCRAMBLE_NUMERIC.charAt(RANDOM.nextInt(SCRAMBLE_NUMERIC.length()));
        }
        // Fallback to random Latin if unknown
        return SCRAMBLE_LATIN.charAt(RANDOM.nextInt(SCRAMBLE_LATIN.length()));
    }

    public boolean isFinished() {
        return !active || (System.currentTimeMillis() - startTime) >= duration;
    }
}

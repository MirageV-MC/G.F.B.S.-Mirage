package org.mirage.gfbs.Event.ccio.dmr;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class DmrShutdownCodeManager {
    private DmrShutdownCodeManager() {}

    private static final AtomicReference<String> currentCode = new AtomicReference<>(null);
    private static final AtomicReference<int[]> crackedDigits = new AtomicReference<>(new int[]{-1, -1, -1, -1, -1, -1});
    private static final Random random = new Random();

    public static String generateCode() {
        int code = random.nextInt(1000000);
        String codeStr = String.format("%06d", code);
        currentCode.set(codeStr);
        crackedDigits.set(new int[]{-1, -1, -1, -1, -1, -1});
        return codeStr;
    }

    public static String getCurrentCode() {
        return currentCode.get();
    }

    public static boolean hasActiveCode() {
        return currentCode.get() != null;
    }

    public static boolean verifyCode(String inputCode) {
        String code = currentCode.get();
        if (code == null || inputCode == null) return false;
        return code.equals(inputCode);
    }

    public static void clearCode() {
        currentCode.set(null);
        crackedDigits.set(new int[]{-1, -1, -1, -1, -1, -1});
    }

    public static int[] getCrackedDigits() {
        return crackedDigits.get().clone();
    }

    public static int getCrackedDigitCount() {
        int[] digits = crackedDigits.get();
        int count = 0;
        for (int d : digits) {
            if (d >= 0) count++;
        }
        return count;
    }

    public static int tryCrackDigit(int position, int digit) {
        String code = currentCode.get();
        if (code == null) return -1;
        if (position < 0 || position > 5) return -1;
        if (digit < 0 || digit > 9) return -1;

        int correctDigit = code.charAt(position) - '0';
        if (digit == correctDigit) {
            int[] digits = crackedDigits.get();
            if (digits[position] < 0) {
                digits[position] = digit;
            }
            return 1;
        }
        return 0;
    }

    public static boolean isFullyCracked() {
        int[] digits = crackedDigits.get();
        for (int d : digits) {
            if (d < 0) return false;
        }
        return true;
    }

    public static String getCrackedDisplay() {
        int[] digits = crackedDigits.get();
        StringBuilder sb = new StringBuilder();
        for (int d : digits) {
            if (d >= 0) {
                sb.append(d);
            } else {
                sb.append('-');
            }
        }
        return sb.toString();
    }
}

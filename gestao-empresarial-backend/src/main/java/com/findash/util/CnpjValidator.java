package com.findash.util;

public final class CnpjValidator {

    private CnpjValidator() {}

    public static String normalize(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("[^0-9]", "");
    }

    public static String format(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;
        String digits = normalize(cnpj);
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int remainder = sum % 11;
        int check1 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(12)) != check1) return false;

        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        remainder = sum % 11;
        int check2 = remainder < 2 ? 0 : 11 - remainder;
        return Character.getNumericValue(digits.charAt(13)) == check2;
    }
}

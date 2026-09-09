package io.github.mmilk23.screenduo.display;

final class PixelFont {

    private PixelFont() {
    }

    static int[] glyph(char character) {
        return switch (Character.toUpperCase(character)) {
            case 'A' -> rows(14, 17, 17, 31, 17, 17, 17);
            case 'B' -> rows(30, 17, 17, 30, 17, 17, 30);
            case 'C' -> rows(14, 17, 16, 16, 16, 17, 14);
            case 'D' -> rows(30, 17, 17, 17, 17, 17, 30);
            case 'E' -> rows(31, 16, 16, 30, 16, 16, 31);
            case 'F' -> rows(31, 16, 16, 30, 16, 16, 16);
            case 'G' -> rows(14, 17, 16, 23, 17, 17, 15);
            case 'H' -> rows(17, 17, 17, 31, 17, 17, 17);
            case 'I' -> rows(14, 4, 4, 4, 4, 4, 14);
            case 'J' -> rows(7, 2, 2, 2, 18, 18, 12);
            case 'K' -> rows(17, 18, 20, 24, 20, 18, 17);
            case 'L' -> rows(16, 16, 16, 16, 16, 16, 31);
            case 'M' -> rows(17, 27, 21, 21, 17, 17, 17);
            case 'N' -> rows(17, 25, 21, 19, 17, 17, 17);
            case 'O' -> rows(14, 17, 17, 17, 17, 17, 14);
            case 'P' -> rows(30, 17, 17, 30, 16, 16, 16);
            case 'Q' -> rows(14, 17, 17, 17, 21, 18, 13);
            case 'R' -> rows(30, 17, 17, 30, 20, 18, 17);
            case 'S' -> rows(15, 16, 16, 14, 1, 1, 30);
            case 'T' -> rows(31, 4, 4, 4, 4, 4, 4);
            case 'U' -> rows(17, 17, 17, 17, 17, 17, 14);
            case 'V' -> rows(17, 17, 17, 17, 17, 10, 4);
            case 'W' -> rows(17, 17, 17, 21, 21, 21, 10);
            case 'X' -> rows(17, 17, 10, 4, 10, 17, 17);
            case 'Y' -> rows(17, 17, 10, 4, 4, 4, 4);
            case 'Z' -> rows(31, 1, 2, 4, 8, 16, 31);
            case '0' -> rows(14, 17, 19, 21, 25, 17, 14);
            case '1' -> rows(4, 12, 4, 4, 4, 4, 14);
            case '2' -> rows(14, 17, 1, 2, 4, 8, 31);
            case '3' -> rows(30, 1, 1, 14, 1, 1, 30);
            case '4' -> rows(2, 6, 10, 18, 31, 2, 2);
            case '5' -> rows(31, 16, 16, 30, 1, 1, 30);
            case '6' -> rows(14, 16, 16, 30, 17, 17, 14);
            case '7' -> rows(31, 1, 2, 4, 8, 8, 8);
            case '8' -> rows(14, 17, 17, 14, 17, 17, 14);
            case '9' -> rows(14, 17, 17, 15, 1, 1, 14);
            case '.' -> rows(0, 0, 0, 0, 0, 12, 12);
            case ',' -> rows(0, 0, 0, 0, 0, 12, 8);
            case '-' -> rows(0, 0, 0, 31, 0, 0, 0);
            case '>' -> rows(16, 8, 4, 2, 4, 8, 16);
            case '(' -> rows(2, 4, 8, 8, 8, 4, 2);
            case ')' -> rows(8, 4, 2, 2, 2, 4, 8);
            case ':' -> rows(0, 12, 12, 0, 12, 12, 0);
            case '%' -> rows(17, 2, 4, 4, 8, 16, 17);
            case '/' -> rows(1, 2, 2, 4, 8, 8, 16);
            case ' ' -> rows(0, 0, 0, 0, 0, 0, 0);
            default -> rows(14, 17, 1, 2, 4, 0, 4);
        };
    }

    private static int[] rows(int... rows) {
        return rows;
    }
}

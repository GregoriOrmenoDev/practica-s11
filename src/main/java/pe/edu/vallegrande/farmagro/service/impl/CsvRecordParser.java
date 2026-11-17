package pe.edu.vallegrande.farmagro.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CsvRecordParser {

    private static final char QUOTE = '"';
    private static final char COMMA = ',';
    private static final char SEMICOLON = ';';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final int NO_FIELDS = 0;

    private CsvRecordParser() {
    }

    static List<String[]> readAll(InputStream input) throws IOException {
        return parseRecords(readContent(input));
    }

    private static String readContent(InputStream input) throws IOException {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Uploads declare no delimiter; the one giving more header columns wins.
    private static List<String[]> parseRecords(String content) throws IOException {
        List<String[]> commaRecords = parseRecords(content, COMMA);
        List<String[]> semicolonRecords = parseRecords(content, SEMICOLON);
        return headerWidth(semicolonRecords) > headerWidth(commaRecords) ? semicolonRecords : commaRecords;
    }

    private static int headerWidth(List<String[]> records) {
        return records.isEmpty() ? NO_FIELDS : records.getFirst().length;
    }

    private static List<String[]> parseRecords(String content, char delimiter) throws IOException {
        CsvParseState state = new CsvParseState();
        for (int index = 0; index < content.length(); index++) {
            index = parseCharacter(content, delimiter, index, state);
        }
        return finishRecords(state);
    }

    private static int parseCharacter(String content, char delimiter, int index, CsvParseState state) {
        char current = content.charAt(index);
        if (current == QUOTE) return parseQuote(content, index, state);
        if (!state.insideQuotes && current == delimiter) addField(state.fields, state.field);
        else if (!state.insideQuotes && isLineBreak(current)) return parseLineBreak(content, index, state);
        else state.field.append(current);
        return index;
    }

    private static int parseQuote(String content, int index, CsvParseState state) {
        QuoteScan scan = scanQuote(content, index, state.insideQuotes);
        if (scan.escaped()) {
            state.field.append(QUOTE);
            return scan.nextIndex();
        }
        if (state.insideQuotes) state.insideQuotes = false;
        else if (state.field.isEmpty()) state.insideQuotes = scan.insideQuotes();
        else state.field.append(QUOTE);
        return scan.nextIndex();
    }

    private static int parseLineBreak(String content, int index, CsvParseState state) {
        addRecord(state.records, state.fields, state.field);
        if (content.charAt(index) == CARRIAGE_RETURN && index + 1 < content.length()
                && content.charAt(index + 1) == LINE_FEED) return index + 1;
        return index;
    }

    private static boolean isLineBreak(char character) {
        return character == LINE_FEED || character == CARRIAGE_RETURN;
    }

    private static List<String[]> finishRecords(CsvParseState state) throws IOException {
        if (state.insideQuotes) {
            throw new IOException("CSV contains an unterminated quoted field");
        }
        if (!state.field.isEmpty() || !state.fields.isEmpty()) {
            addRecord(state.records, state.fields, state.field);
        }
        return state.records;
    }

    static boolean isBlank(String[] record) {
        for (String field : record) {
            if (field != null && !field.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static QuoteScan scanQuote(String content, int index, boolean insideQuotes) {
        if (insideQuotes && index + 1 < content.length() && content.charAt(index + 1) == QUOTE) {
            return new QuoteScan(true, index + 1, true);
        }
        return new QuoteScan(!insideQuotes, index, false);
    }

    private static void addField(List<String> fields, StringBuilder field) {
        fields.add(field.toString());
        field.setLength(0);
    }

    private static void addRecord(List<String[]> records, List<String> fields, StringBuilder field) {
        addField(fields, field);
        records.add(fields.toArray(String[]::new));
        fields.clear();
    }

    private static final class CsvParseState {
        private final List<String[]> records = new ArrayList<>();
        private final List<String> fields = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean insideQuotes;
    }

    private record QuoteScan(boolean insideQuotes, int nextIndex, boolean escaped) {
    }
}

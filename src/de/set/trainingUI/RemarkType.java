package de.set.trainingUI;

public enum RemarkType {
    WRONG_COMPARISON("Fehlerhafte(r) Vergleich/Bedingung"),
    WRONG_CALCULATION("Fehlerhafte Berechnung"),
    MISSING_CODE("Fehlender Code"),
    OTHER_ALGORITHMIC_PROBLEM("Anderes algorithmisches Problem"),
    DUPLICATE_CODE("Doppelter Code");

    private String text;

    private RemarkType(final String text) {
        this.text = text;
    }

    public String getValue() {
        return this.name();
    }

    public String getText() {
        return this.text;
    }
}
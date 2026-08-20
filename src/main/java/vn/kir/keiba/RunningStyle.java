package vn.kir.keiba;

enum RunningStyle {
    NIGE("Dẫn đầu"),
    SENKO("Bám đầu"),
    SASHI("Tăng tốc giữa"),
    OIKOMI("Nước rút cuối");

    final String label;
    RunningStyle(String label) { this.label = label; }
    String label() { return label; }
}

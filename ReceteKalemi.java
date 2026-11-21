package KafeMenu;

/** Ürün içeriğindeki tek bir satır: "Latte içinde 200 ml Süt" gibi (porsiyon başına). */
public record ReceteKalemi(int malzemeId, String malzemeAdi, String birim, double miktar,
		double malzemeStok, double malzemeMinStok, String alerjen) {
}

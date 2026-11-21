package KafeMenu;

/** Ürün hazırlanırken stokta yetmeyen bir malzeme. */
public record EksikMalzeme(int malzemeId, String ad, String birim, double gereken, double mevcut, double minStok) {

	public double eksik() {
		return gereken - mevcut;
	}
}

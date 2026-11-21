package KafeMenu;

import java.math.BigDecimal;

/**
 * Menüdeki bir ürün = kafe_menu_listesi tablosundaki bir satır.
 *
 * icerik, alerjenler, vegan ve vejetaryen bilgileri ürünün malzemelerinden otomatik hesaplanır.
 * hazirlanabilir: mevcut malzemelerle kaç porsiyon daha hazırlanabileceği (-1 = içerik tanımsız).
 */
public record Urun(int id, String ad, String kategori, BigDecimal fiyat, String icerik, String alerjenler,
		boolean vegan, boolean vejetaryen, int stok, int minStok, int hazirlanabilir) {

	public static final String[] KATEGORILER = { "Yemek", "İçecek", "Tatlı" };

	/** Ekranda gösterilen kısa kod, ör. U-007 */
	public String kod() {
		return String.format("U-%03d", id);
	}

	public StokDurumu durum() {
		return StokDurumu.hesapla(stok, minStok);
	}

	public boolean alerjenVar() {
		return alerjenler != null && !alerjenler.isBlank();
	}
}

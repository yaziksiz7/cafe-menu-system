package KafeMenu;

/**
 * Mutfakta kullanılan bir malzeme = kafe_malzemeler tablosundaki bir satır.
 * kullanimSayisi: bu malzemenin kaç üründe kullanıldığı (veritabanında hesaplanır).
 */
public record Malzeme(int id, String ad, String birim, double stok, double minStok, String alerjen,
		boolean vegan, boolean vejetaryen, int kullanimSayisi) {

	/** Avrupa Birliği'nin 14 temel alerjeni (Türkçe). */
	public static final String[] ALERJENLER = { "Gluten", "Süt", "Yumurta", "Kuruyemiş", "Yer fıstığı", "Soya",
			"Susam", "Balık", "Kabuklu deniz ürünleri", "Yumuşakçalar", "Kereviz", "Hardal", "Sülfit", "Acı bakla" };

	public static final String[] BIRIMLER = { "gr", "kg", "ml", "lt", "adet", "dilim", "paket" };

	public String kod() {
		return String.format("M-%03d", id);
	}

	public StokDurumu durum() {
		return StokDurumu.hesapla(stok, minStok);
	}

	public boolean alerjenVar() {
		return alerjen != null && !alerjen.isBlank();
	}

	/** Açılır listelerde malzemenin adı görünsün diye. */
	@Override
	public String toString() {
		return ad;
	}
}

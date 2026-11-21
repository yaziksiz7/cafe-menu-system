package KafeMenu;

import java.awt.Color;

/** Stok miktarının kritik seviyeye göre durumu. */
public enum StokDurumu {
	TUKENDI("Tükendi", Tema.KIRMIZI, Tema.KIRMIZI_ACIK),
	KRITIK("Kritik", Tema.KIRMIZI, Tema.KIRMIZI_ACIK),
	AZALIYOR("Azalıyor", Tema.TURUNCU, Tema.TURUNCU_ACIK),
	YETERLI("Yeterli", Tema.YESIL, Tema.YESIL_ACIK);

	public final String etiket;
	public final Color renk;
	public final Color acikRenk;

	StokDurumu(String etiket, Color renk, Color acikRenk) {
		this.etiket = etiket;
		this.renk = renk;
		this.acikRenk = acikRenk;
	}

	/** stok <= 0 tükendi, stok <= kritik seviye kritik, kritik seviyenin 2 katına kadar azalıyor. */
	public static StokDurumu hesapla(double stok, double minStok) {
		if (stok <= 0) return TUKENDI;
		if (stok <= minStok) return KRITIK;
		if (stok <= minStok * 2) return AZALIYOR;
		return YETERLI;
	}

	/** Sipariş verilmesi gereken durum mu? */
	public boolean dusukMu() {
		return this == TUKENDI || this == KRITIK;
	}
}

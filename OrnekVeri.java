package KafeMenu;

import java.util.List;

/**
 * Uygulama ilk kez açıldığında (malzeme tablosu boşsa) MySQL'e yüklenen örnek menü.
 * Bazı ürün ve malzemelerin stoğu, uyarıları görebilmeniz için bilerek düşük bırakıldı.
 */
final class OrnekVeri {

	private OrnekVeri() {
	}

	/** Malzeme: ad, birim, stok, kritik seviye, alerjen, vegan mı, vejetaryen mi */
	record M(String ad, String birim, double stok, double min, String alerjen, boolean vegan, boolean vejetaryen) {
	}

	/** Ürün: ad, kategori, fiyat, stok, kritik seviye, içerik (malzeme adı, miktar, malzeme adı, miktar, ...) */
	record U(String ad, String kategori, String fiyat, int stok, int min, Object[] recete) {
	}

	private static M m(String ad, String birim, double stok, double min, String alerjen, boolean vegan, boolean vej) {
		return new M(ad, birim, stok, min, alerjen, vegan, vej);
	}

	private static U u(String ad, String kategori, String fiyat, int stok, int min, Object... recete) {
		return new U(ad, kategori, fiyat, stok, min, recete);
	}

	static final List<M> MALZEMELER = List.of(
			m("Kahve çekirdeği", "gr", 2400, 600, null, true, true),
			m("Süt", "ml", 9000, 3000, "Süt", false, true),
			m("Yulaf sütü", "ml", 2600, 1500, "Gluten", true, true),
			m("Siyah çay", "gr", 900, 250, null, true, true),
			m("Şeker", "gr", 6000, 1500, null, true, true),
			m("Kakao", "gr", 1400, 400, null, true, true),
			m("Bitter çikolata", "gr", 2200, 600, "Süt", false, true),
			m("Vanilya özü", "ml", 250, 60, null, true, true),
			m("Limon", "adet", 45, 15, null, true, true),
			m("Taze nane", "gr", 260, 100, null, true, true),
			m("Portakal", "adet", 14, 20, null, true, true),
			m("Çilek", "gr", 1600, 500, null, true, true),
			m("Muz", "adet", 22, 10, null, true, true),
			m("Buz", "gr", 12000, 3000, null, true, true),
			m("Un", "gr", 12000, 3000, "Gluten", true, true),
			m("Yumurta", "adet", 90, 30, "Yumurta", false, true),
			m("Tereyağı", "gr", 2800, 800, "Süt", false, true),
			m("Krema", "ml", 2100, 800, "Süt", false, true),
			m("Krem peynir", "gr", 1300, 500, "Süt", false, true),
			m("Kedi dili bisküvi", "gr", 1200, 400, "Gluten, Yumurta", false, true),
			m("Fındık", "gr", 900, 300, "Kuruyemiş", true, true),
			m("Tam buğday ekmeği", "dilim", 60, 20, "Gluten", true, true),
			m("Tavuk göğsü", "gr", 5200, 1500, null, false, false),
			m("Kaşar peyniri", "gr", 2600, 800, "Süt", false, true),
			m("Parmesan", "gr", 700, 300, "Süt", false, true),
			m("Domates", "gr", 4200, 1000, null, true, true),
			m("Marul", "gr", 1900, 600, null, true, true),
			m("Avokado", "adet", 4, 8, null, true, true),
			m("Nohut", "gr", 3200, 800, null, true, true),
			m("Tahin", "gr", 950, 300, "Susam", true, true),
			m("Zeytinyağı", "ml", 5200, 1000, null, true, true),
			m("Penne makarna", "gr", 5400, 1500, "Gluten", true, true),
			m("Mantar", "gr", 1500, 500, null, true, true),
			m("Somon fileto", "gr", 700, 900, "Balık", false, false),
			m("Kinoa", "gr", 2100, 500, null, true, true),
			m("Fesleğen pesto", "gr", 750, 300, "Süt, Kuruyemiş", false, true),
			m("Kırmızı mercimek", "gr", 3400, 800, null, true, true),
			m("Soğan", "gr", 3100, 800, null, true, true),
			m("Hindistan cevizi sütü", "ml", 1800, 600, null, true, true),
			m("Hardal sos", "gr", 600, 200, "Hardal", true, true));

	static final List<U> URUNLER = List.of(
			// ---- İçecekler ----
			u("Espresso", "İçecek", "75", 40, 10, "Kahve çekirdeği", 18),
			u("Caffè Latte", "İçecek", "110", 30, 10, "Kahve çekirdeği", 18, "Süt", 200),
			u("Yulaf Sütlü Latte", "İçecek", "125", 18, 8, "Kahve çekirdeği", 18, "Yulaf sütü", 200),
			u("Sıcak Çikolata", "İçecek", "115", 6, 8, "Kakao", 20, "Bitter çikolata", 25, "Süt", 250, "Şeker", 10),
			u("Taze Portakal Suyu", "İçecek", "95", 4, 5, "Portakal", 3),
			u("Ev Yapımı Limonata", "İçecek", "85", 24, 8, "Limon", 2, "Şeker", 30, "Taze nane", 5, "Buz", 120),
			u("Çilekli Muz Smoothie", "İçecek", "120", 12, 6, "Çilek", 120, "Muz", 1, "Yulaf sütü", 150, "Buz", 80),
			u("Demleme Çay", "İçecek", "35", 80, 20, "Siyah çay", 5),
			// ---- Yemekler ----
			u("Tavuklu Sezar Salata", "Yemek", "245", 10, 5, "Tavuk göğsü", 150, "Marul", 120, "Parmesan", 25,
					"Tam buğday ekmeği", 1, "Hardal sos", 10, "Zeytinyağı", 15),
			u("Avokadolu Tost", "Yemek", "195", 3, 5, "Tam buğday ekmeği", 2, "Avokado", 1, "Domates", 50,
					"Zeytinyağı", 10),
			u("Humus Tabağı", "Yemek", "165", 14, 5, "Nohut", 150, "Tahin", 30, "Limon", 1, "Zeytinyağı", 20,
					"Tam buğday ekmeği", 1),
			u("Kremalı Mantarlı Penne", "Yemek", "225", 9, 5, "Penne makarna", 120, "Mantar", 100, "Krema", 80,
					"Kaşar peyniri", 30),
			u("Pesto Soslu Makarna", "Yemek", "215", 8, 5, "Penne makarna", 120, "Fesleğen pesto", 40, "Domates", 40),
			u("Fırında Somon & Kinoa", "Yemek", "345", 5, 4, "Somon fileto", 180, "Kinoa", 80, "Marul", 50, "Limon", 1,
					"Zeytinyağı", 10),
			u("Mercimek Çorbası", "Yemek", "110", 20, 8, "Kırmızı mercimek", 80, "Soğan", 30, "Zeytinyağı", 10,
					"Limon", 1),
			u("Kaşarlı Tost", "Yemek", "140", 16, 6, "Tam buğday ekmeği", 2, "Kaşar peyniri", 60, "Tereyağı", 10),
			// ---- Tatlılar ----
			u("San Sebastian Cheesecake", "Tatlı", "185", 8, 4, "Krem peynir", 150, "Yumurta", 2, "Krema", 50,
					"Şeker", 40, "Un", 5),
			u("Çikolatalı Sufle", "Tatlı", "170", 2, 4, "Bitter çikolata", 60, "Tereyağı", 30, "Yumurta", 2,
					"Şeker", 25, "Un", 15),
			u("Tiramisu", "Tatlı", "175", 7, 4, "Krem peynir", 100, "Kedi dili bisküvi", 60, "Kahve çekirdeği", 10,
					"Kakao", 5, "Yumurta", 1, "Şeker", 20),
			u("Vegan Çikolatalı Kek", "Tatlı", "150", 10, 4, "Un", 60, "Kakao", 20, "Şeker", 40,
					"Hindistan cevizi sütü", 60, "Zeytinyağı", 20, "Vanilya özü", 2),
			u("Taze Meyve Salatası", "Tatlı", "120", 9, 4, "Çilek", 80, "Muz", 1, "Portakal", 1, "Taze nane", 2),
			u("Fındıklı Brownie", "Tatlı", "150", 12, 5, "Bitter çikolata", 70, "Tereyağı", 40, "Yumurta", 1, "Un", 30,
					"Fındık", 25, "Şeker", 30));
}

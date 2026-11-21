package KafeMenu;

import javax.swing.SwingUtilities;

/**
 * KAFE MENÜ ATÖLYESİ - programın başlangıç noktası.
 *
 * Eclipse'te bu dosyaya sağ tıklayıp Run As > Java Application ile çalıştırın.
 *
 * Akış:
 *   1) MySQL giriş ekranı açılır, şifre sizden istenir (kodda yazmaz).
 *   2) Bağlantı kurulunca tablolar yoksa oluşturulur, ilk açılışta örnek menü yüklenir.
 *   3) Ana pencere açılır: Menü, Malzemeler & Stok, İşlem Geçmişi.
 */
public final class KafeMenuUygulamasi {

	private KafeMenuUygulamasi() {
	}

	public static void main(String[] args) {
		// Swing ekranları her zaman "Event Dispatch Thread" üzerinde oluşturulmalı.
		SwingUtilities.invokeLater(() -> {
			Tema.uygula();
			new GirisEkrani((db, ornekYuklendi) -> new AnaPencere(db, ornekYuklendi).setVisible(true))
					.setVisible(true);
		});
	}
}

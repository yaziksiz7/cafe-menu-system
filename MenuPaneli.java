package KafeMenu;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.Hap;
import KafeMenu.Bilesenler.Kart;
import KafeMenu.Bilesenler.Kutu;
import KafeMenu.Bilesenler.Rozet;
import KafeMenu.Bilesenler.RozetEtiketi;

/** "Kafe Menüsü" sekmesi: arama, filtreler, ürün tablosu ve seçili ürünün detayları. */
final class MenuPaneli extends JPanel {

	private final AnaPencere ana;
	private final UrunModeli model = new UrunModeli();
	private final Tablolar.BosDurumTablo tablo = new Tablolar.BosDurumTablo(model);
	private final TableRowSorter<UrunModeli> siralayici = new TableRowSorter<>(model);

	private final Alan arama = new Alan("Ürün, içerik veya alerjen ara…   (ör. latte, süt, vegan)", Simge.Tur.ARA);
	private final JLabel sonucEtiketi = Bilesenler.etiket(" ", Font.PLAIN, 12.5f, Tema.SOLUK);
	private final Map<String, Hap> kategoriHaplari = new LinkedHashMap<>();
	private final Hap veganHap = new Hap("Vegan", Tema.YESIL);
	private final Hap vejetaryenHap = new Hap("Vejetaryen", Tema.YAPRAK);
	private final Hap alerjensizHap = new Hap("Alerjensiz", Tema.MAVI);
	private final Hap dusukHap = new Hap("Kritik stok", Tema.TURUNCU);
	private String kategori = "Tümü";

	private final Buton duzenleButonu = new Buton("Düzenle / İçerik", Simge.Tur.KALEM, Tema.KAHVE, Buton.Stil.CERCEVE);
	private final Buton fiyatButonu = new Buton("Fiyat Güncelle", Simge.Tur.ETIKET, Tema.MOR, Buton.Stil.CERCEVE);
	private final Buton silButonu = new Buton("Sil", Simge.Tur.COP, Tema.KIRMIZI, Buton.Stil.CERCEVE);
	private final UrunDetayi detay = new UrunDetayi();

	MenuPaneli(AnaPencere ana) {
		super(new BorderLayout(0, 14));
		this.ana = ana;
		setOpaque(false);
		add(filtreKarti(), BorderLayout.NORTH);
		JPanel orta = new JPanel(new BorderLayout(14, 0));
		orta.setOpaque(false);
		orta.add(tabloKarti(), BorderLayout.CENTER);
		orta.add(detay, BorderLayout.EAST);
		add(orta, BorderLayout.CENTER);
		tabloyuAyarla();
		butonlariGuncelle();
	}

	// =====================================================================
	// Görünüm
	// =====================================================================

	private JComponent filtreKarti() {
		Kart k = new Kart(new BorderLayout(0, 12));
		k.setBorder(new EmptyBorder(14, 18, 16, 18));

		arama.setPreferredSize(new Dimension(470, 40));
		arama.setMaximumSize(new Dimension(470, 40));
		Buton ara = new Buton("Ara", Simge.Tur.ARA, Tema.KAHVE, Buton.Stil.DOLU);
		ara.addActionListener(e -> filtrele(true));
		arama.addActionListener(e -> filtrele(true));
		Buton temizle = new Buton("Temizle", Simge.Tur.CARPI, Tema.KAHVE, Buton.Stil.CERCEVE);
		temizle.addActionListener(e -> filtreleriTemizle());
		arama.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				filtrele(false);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				filtrele(false);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				filtrele(false);
			}
		});
		JPanel satir1 = new JPanel(new BorderLayout());
		satir1.setOpaque(false);
		satir1.add(Bilesenler.yatay(8, arama, ara, temizle), BorderLayout.WEST);
		satir1.add(sonucEtiketi, BorderLayout.EAST);

		ButtonGroup grup = new ButtonGroup();
		List<Component> parcalar = new ArrayList<>();
		parcalar.add(Bilesenler.etiket("Kategori", Font.BOLD, 12.5f, Tema.KAHVE));
		for (String ad : new String[] { "Tümü", "Yemek", "İçecek", "Tatlı" }) {
			Color renk = "Tümü".equals(ad) ? Tema.ESPRESSO : Tema.kategoriRenk(ad)[0];
			Hap h = new Hap(ad, renk);
			grup.add(h);
			h.addActionListener(e -> {
				kategori = ad;
				filtrele(false);
			});
			kategoriHaplari.put(ad, h);
			parcalar.add(h);
		}
		kategoriHaplari.get("Tümü").setSelected(true);
		parcalar.add(Box.createHorizontalStrut(10));
		parcalar.add(Bilesenler.dikeyAyirici());
		parcalar.add(Box.createHorizontalStrut(10));
		parcalar.add(Bilesenler.etiket("Filtre", Font.BOLD, 12.5f, Tema.KAHVE));
		for (Hap h : new Hap[] { veganHap, vejetaryenHap, alerjensizHap, dusukHap }) {
			h.addActionListener(e -> filtrele(false));
			parcalar.add(h);
		}
		JPanel satir2 = Bilesenler.yatay(8, parcalar.toArray(new Component[0]));
		JPanel satir2Kap = new JPanel(new BorderLayout());
		satir2Kap.setOpaque(false);
		satir2Kap.add(satir2, BorderLayout.WEST);

		k.add(satir1, BorderLayout.NORTH);
		k.add(satir2Kap, BorderLayout.CENTER);
		return k;
	}

	private JComponent tabloKarti() {
		Kart k = new Kart(new BorderLayout(0, 12));
		JLabel baslik = Bilesenler.etiket("Kafe Menü Listesi", Font.BOLD, 17f, Tema.METIN);
		JLabel alt = Bilesenler.etiket("MySQL tablosu: kafe_menu_listesi  ·  Sıralamak için sütun başlığına tıklayın",
				Font.PLAIN, 12f, Tema.SOLUK);
		Buton yeni = new Buton("Yeni Ürün", Simge.Tur.ARTI, Tema.KARAMEL, Buton.Stil.DOLU);
		yeni.addActionListener(e -> yeniUrun());
		duzenleButonu.addActionListener(e -> duzenle());
		fiyatButonu.addActionListener(e -> fiyatGuncelle());
		silButonu.addActionListener(e -> sil());

		JPanel ust = new JPanel(new BorderLayout(16, 0));
		ust.setOpaque(false);
		ust.add(Bilesenler.dikey(2, baslik, alt), BorderLayout.CENTER);
		ust.add(Bilesenler.yatay(8, yeni, duzenleButonu, fiyatButonu, silButonu), BorderLayout.EAST);
		k.add(ust, BorderLayout.NORTH);
		k.add(Tablolar.kaydirma(tablo), BorderLayout.CENTER);
		return k;
	}

	private void tabloyuAyarla() {
		Tablolar.bicimle(tablo, 42);
		tablo.setRowSorter(siralayici);
		tablo.bosMesaj("Aramanıza uyan ürün bulunamadı");
		Tablolar.sutun(tablo, 0, 70, 66, new Tablolar.MetinRenderer(Font.BOLD, Tema.SOLUK,
				v -> String.format("U-%03d", (Integer) v)));
		Tablolar.sutun(tablo, 1, 190, 150, new Tablolar.MetinRenderer(Font.BOLD, Tema.METIN, null));
		Tablolar.sutun(tablo, 2, 96, 92, new Tablolar.RozetRenderer(Tablolar::kategoriRozeti));
		Tablolar.sutun(tablo, 3, 92, 88, new Tablolar.MetinRenderer(Font.BOLD, Tema.KARAMEL_KOYU,
				v -> Tema.para((BigDecimal) v)));
		Tablolar.sutun(tablo, 4, 250, 100, new Tablolar.MetinRenderer(Font.PLAIN, new Color(0x5E4E44), null));
		Tablolar.sutun(tablo, 5, 160, 120, new Tablolar.RozetRenderer(Tablolar::alerjenRozetleri));
		Tablolar.sutun(tablo, 6, 150, 146, new Tablolar.RozetRenderer(Tablolar::beslenmeRozeti));
		Tablolar.sutun(tablo, 7, 150, 132, new Tablolar.StokRenderer(true));

		tablo.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				detay.goster(seciliUrun());
				butonlariGuncelle();
			}
		});
		tablo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && tablo.rowAtPoint(e.getPoint()) >= 0) duzenle();
			}
		});
		tablo.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "sil");
		tablo.getActionMap().put("sil", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sil();
			}
		});
		tablo.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "duzenle");
		tablo.getActionMap().put("duzenle", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				duzenle();
			}
		});
	}

	// =====================================================================
	// Veri ve filtre
	// =====================================================================

	void veriYukle(List<Urun> urunler) {
		Urun secili = seciliUrun();
		model.ayarla(urunler);
		filtrele(false);
		if (secili != null) sec(secili.id());
		else detay.goster(null);
	}

	void filtreleriTemizle() {
		arama.setText("");
		kategori = "Tümü";
		kategoriHaplari.get("Tümü").setSelected(true);
		for (Hap h : new Hap[] { veganHap, vejetaryenHap, alerjensizHap, dusukHap }) h.setSelected(false);
		filtrele(false);
	}

	void dusukStokGoster() {
		filtreleriTemizle();
		dusukHap.setSelected(true);
		filtrele(false);
	}

	private void filtrele(boolean araButonu) {
		String[] kelimeler = Tema.kucuk(arama.getText()).trim().split("\\s+");
		boolean vegan = veganHap.isSelected(), vej = vejetaryenHap.isSelected();
		boolean alerjensiz = alerjensizHap.isSelected(), dusuk = dusukHap.isSelected();
		siralayici.setRowFilter(new RowFilter<UrunModeli, Integer>() {
			@Override
			public boolean include(Entry<? extends UrunModeli, ? extends Integer> giris) {
				Urun u = model.urun(giris.getIdentifier());
				if (!"Tümü".equals(kategori) && !kategori.equals(u.kategori())) return false;
				if (vegan && !u.vegan()) return false;
				if (vej && !u.vejetaryen()) return false;
				if (alerjensiz && u.alerjenVar()) return false;
				if (dusuk && !u.durum().dusukMu()) return false;
				String metin = Tema.kucuk(u.kod() + " " + u.ad() + " " + u.kategori() + " " + u.icerik() + " "
						+ u.alerjenler() + (u.vegan() ? " vegan" : "") + (u.vejetaryen() ? " vejetaryen" : ""));
				for (String k : kelimeler) {
					if (!k.isEmpty() && !metin.contains(k)) return false;
				}
				return true;
			}
		});
		int gorunen = tablo.getRowCount();
		sonucEtiketi.setText(gorunen == model.getRowCount() ? model.getRowCount() + " ürün listeleniyor"
				: model.getRowCount() + " üründen " + gorunen + " tanesi gösteriliyor");
		if (araButonu) {
			if (gorunen > 0) {
				tablo.setRowSelectionInterval(0, 0);
				tablo.scrollRectToVisible(tablo.getCellRect(0, 0, true));
				tablo.requestFocusInWindow();
			} else {
				Bilesenler.bildirim(ana, "“" + arama.getText().trim() + "” için ürün bulunamadı.", false);
			}
		}
		if (seciliUrun() == null) detay.goster(null);
	}

	private Urun seciliUrun() {
		int satir = tablo.getSelectedRow();
		return satir < 0 ? null : model.urun(tablo.convertRowIndexToModel(satir));
	}

	void sec(int urunId) {
		int m = model.satirBul(urunId);
		if (m < 0) return;
		int v = tablo.convertRowIndexToView(m);
		if (v < 0) return;
		tablo.setRowSelectionInterval(v, v);
		Rectangle r = tablo.getCellRect(v, 0, true);
		tablo.scrollRectToVisible(r);
		detay.goster(model.urun(m));
	}

	private void butonlariGuncelle() {
		boolean var = seciliUrun() != null;
		duzenleButonu.setEnabled(var);
		fiyatButonu.setEnabled(var);
		silButonu.setEnabled(var);
	}

	// =====================================================================
	// İşlemler
	// =====================================================================

	private void yeniUrun() {
		if (ana.malzemeler().isEmpty()) {
			Diyaloglar.uyari(ana, "Önce malzeme ekleyin",
					"Ürün içeriği malzemelerden oluşur. Önce “Malzemeler & Stok” sekmesinden malzeme ekleyin.");
			return;
		}
		UrunDialog d = new UrunDialog(ana, null, List.of());
		if (d.ac()) {
			filtreleriTemizle();
			ana.yenile();
			sec(d.kaydedilenId());
			ana.basari("“" + d.kaydedilenAd() + "” menüye eklendi.");
		}
	}

	private void duzenle() {
		Urun u = seciliUrun();
		if (u == null) return;
		try {
			UrunDialog d = new UrunDialog(ana, u, ana.db.receteGetir(u.id()));
			if (d.ac()) {
				ana.yenile();
				sec(u.id());
				ana.basari("“" + d.kaydedilenAd() + "” güncellendi.");
			}
		} catch (SQLException e) {
			Diyaloglar.hata(ana, "Ürün içeriği okunamadı", e);
		}
	}

	private void fiyatGuncelle() {
		Urun u = seciliUrun();
		if (u == null) return;
		if (new Diyaloglar.FiyatDialog(ana, u).ac()) {
			ana.yenile();
			sec(u.id());
			Urun yeni = model.urun(model.satirBul(u.id()));
			ana.basari("“" + u.ad() + "” fiyatı " + Tema.para(yeni.fiyat()) + " olarak güncellendi.");
		}
	}

	private void sil() {
		Urun u = seciliUrun();
		if (u == null) return;
		if (Diyaloglar.onayla(ana, "Ürünü Sil",
				"“" + u.ad() + "” menüden kalıcı olarak silinecek.\nBu işlem geri alınamaz.", "Evet, Sil",
				Tema.KIRMIZI)) {
			try {
				ana.db.urunSil(u.id());
				ana.yenile();
				ana.basari("“" + u.ad() + "” menüden silindi.");
			} catch (SQLException e) {
				Diyaloglar.hata(ana, "Ürün silinemedi", e);
			}
		}
	}

	private void hazirla(Urun u) {
		AtomicReference<List<EksikMalzeme>> sonuc = new AtomicReference<>(List.of());
		String bilgi = "Hazırlanan porsiyonlar ürün stoğuna eklenir, kullanılan malzemeler malzeme stoğundan düşülür.<br>"
				+ (u.hazirlanabilir() < 0 ? "<b>Bu ürünün içeriği tanımlı değil.</b>"
						: "Mevcut malzemelerle en fazla <b>" + u.hazirlanabilir() + " porsiyon</b> hazırlanabilir.");
		Diyaloglar.MiktarDialog d = new Diyaloglar.MiktarDialog(ana, "Ürün Hazırla", u.ad() + " · mutfakta hazırla",
				Simge.Tur.TENCERE, "Hazırla", Tema.KAHVE, bilgi, "porsiyon",
				Math.max(1, Math.min(10, u.hazirlanabilir())), 1, true, null, null,
				(miktar, not) -> sonuc.set(ana.db.hazirla(u.id(), (int) miktar)));
		if (!d.ac()) return;
		int adet = (int) d.miktar();
		if (sonuc.get().isEmpty()) {
			ana.yenile();
			sec(u.id());
			ana.basari(adet + " porsiyon “" + u.ad() + "” hazırlandı, malzemeler stoktan düşüldü.");
		} else {
			eksikleriGoster(u, adet, sonuc.get());
		}
	}

	/** Hazırlamaya yetmeyen malzemeleri gösterir ve sipariş verip tekrar hazırlamayı önerir. */
	private void eksikleriGoster(Urun u, int adet, List<EksikMalzeme> eksikler) {
		List<Diyaloglar.TopluSiparisDialog.Satir> satirlar = new ArrayList<>();
		for (EksikMalzeme e : eksikler) {
			satirlar.add(new Diyaloglar.TopluSiparisDialog.Satir(e.malzemeId(), e.ad(), e.birim(), e.mevcut(),
					e.gereken(), Math.ceil(e.eksik() + e.minStok())));
		}
		Diyaloglar.TopluSiparisDialog d = new Diyaloglar.TopluSiparisDialog(ana, "Eksik Malzemeler",
				"“" + u.ad() + "” · " + adet + " porsiyon",
				"<b>" + eksikler.size() + " malzeme</b> yetersiz olduğu için ürün hazırlanamadı; stoklara dokunulmadı. "
						+ "Önerilen miktarlar eksik kadar + kritik seviye kadar yedek içerir. "
						+ "Sipariş verildikten sonra ürün otomatik olarak hazırlanır.",
				satirlar, "Gereken", "Sipariş Ver ve Hazırla", "“" + u.ad() + "” hazırlığı için eksik malzeme");
		if (!d.ac()) {
			ana.yenile();
			return;
		}
		try {
			List<EksikMalzeme> tekrar = ana.db.hazirla(u.id(), adet);
			ana.yenile();
			sec(u.id());
			if (tekrar.isEmpty()) {
				ana.basari("Eksikler sipariş edildi ve " + adet + " porsiyon “" + u.ad() + "” hazırlandı.");
			} else {
				eksikleriGoster(u, adet, tekrar);
			}
		} catch (Exception e) {
			ana.yenile();
			Diyaloglar.hata(ana, "Ürün hazırlanamadı", e);
		}
	}

	private void siparisVer(Urun u) {
		int oneri = Math.max(u.minStok() * 3 - u.stok(), Math.max(1, u.minStok()));
		String bilgi = "Stokta <b>" + u.stok() + " adet</b> var · kritik seviye " + u.minStok() + " adet.<br>"
				+ "Hazır ürün siparişi (ör. tedarikçiden) doğrudan ürün stoğuna eklenir. "
				+ "Kendi mutfağınızda üretmek için “Hazırla”yı kullanın.";
		Diyaloglar.MiktarDialog d = new Diyaloglar.MiktarDialog(ana, "Sipariş Ver", u.ad(), Simge.Tur.KAMYON,
				"Sipariş Ver", Tema.KARAMEL, bilgi, "adet", oneri, 1, true, "Tedarikçi adı veya not", null,
				(miktar, not) -> ana.db.siparisVer(true, u.id(), miktar, not));
		if (d.ac()) {
			ana.yenile();
			sec(u.id());
			ana.basari((int) d.miktar() + " adet “" + u.ad() + "” sipariş edildi, stok güncellendi.");
		}
	}

	private void satisYap(Urun u) {
		if (u.stok() <= 0) {
			Diyaloglar.uyari(ana, "Stok yok", "“" + u.ad() + "” stokta kalmadı. Önce hazırlayın veya sipariş verin.");
			return;
		}
		Diyaloglar.MiktarDialog d = new Diyaloglar.MiktarDialog(ana, "Satış Yap", u.ad(), Simge.Tur.FIS,
				"Satışı Kaydet", Tema.YAPRAK, "Stokta <b>" + u.stok() + " adet</b> var. Birim fiyat: <b>"
						+ Tema.para(u.fiyat()) + "</b>",
				"adet", 1, 1, true, null,
				m -> "Toplam tutar: " + Tema.para(u.fiyat().multiply(BigDecimal.valueOf((long) m))),
				(miktar, not) -> ana.db.sat(u.id(), (int) miktar));
		if (d.ac()) {
			ana.yenile();
			sec(u.id());
			ana.basari((int) d.miktar() + " adet “" + u.ad() + "” satıldı.");
		}
	}

	// =====================================================================
	// Tablo modeli
	// =====================================================================

	static final class UrunModeli extends AbstractTableModel {
		private static final String[] SUTUNLAR = { "ID", "Ürün Adı", "Kategori", "Fiyat", "Ürün İçeriği",
				"Alerjenler", "Vegan / Vejetaryen", "Stok" };
		private List<Urun> veri = new ArrayList<>();

		void ayarla(List<Urun> liste) {
			veri = new ArrayList<>(liste);
			fireTableDataChanged();
		}

		Urun urun(int modelSatiri) {
			return veri.get(modelSatiri);
		}

		int satirBul(int id) {
			for (int i = 0; i < veri.size(); i++) {
				if (veri.get(i).id() == id) return i;
			}
			return -1;
		}

		@Override
		public int getRowCount() {
			return veri.size();
		}

		@Override
		public int getColumnCount() {
			return SUTUNLAR.length;
		}

		@Override
		public String getColumnName(int c) {
			return SUTUNLAR[c];
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return switch (c) {
				case 0, 6 -> Integer.class;
				case 3 -> BigDecimal.class;
				case 7 -> Tablolar.StokGorunumu.class;
				default -> String.class;
			};
		}

		@Override
		public Object getValueAt(int r, int c) {
			Urun u = veri.get(r);
			return switch (c) {
				case 0 -> u.id();
				case 1 -> u.ad();
				case 2 -> u.kategori();
				case 3 -> u.fiyat();
				case 4 -> u.icerik();
				case 5 -> u.alerjenler();
				case 6 -> u.vegan() ? 2 : u.vejetaryen() ? 1 : 0;
				default -> new Tablolar.StokGorunumu(u.stok(), u.minStok(), "adet");
			};
		}
	}

	// =====================================================================
	// Sağdaki ürün detay paneli
	// =====================================================================

	/** Kaydırılabilir panelin genişliği görünüme uysun diye. */
	private static final class DikeyPanel extends JPanel implements Scrollable {
		DikeyPanel() {
			setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
			return 120;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	private final class UrunDetayi extends Kart {
		private static final int GENISLIK = 290;
		private final CardLayout kartlar;
		private Urun urun;

		private final RozetEtiketi kategoriRozeti = new RozetEtiketi();
		private final JLabel kodEtiketi = Bilesenler.etiket("", Font.BOLD, 12.5f, Tema.SOLUK);
		private final JLabel adEtiketi = Bilesenler.etiket("", Font.BOLD, 21f, Tema.METIN);
		private final JLabel fiyatEtiketi = Bilesenler.etiket("", Font.BOLD, 28f, Tema.KARAMEL_KOYU);
		private final RozetEtiketi beslenmeRozetleri = new RozetEtiketi();
		private final JPanel icerikListesi = new JPanel(new GridLayout(0, 1, 0, 2));
		private final Kutu alerjenKutusu = new Kutu(new BorderLayout(10, 0), Tema.KIRMIZI_ACIK);
		private final JLabel alerjenSimgesi = new JLabel();
		private final JLabel alerjenMetni = new JLabel();
		private final JLabel stokSayisi = Bilesenler.etiket("", Font.BOLD, 26f, Tema.METIN);
		private final RozetEtiketi stokRozeti = new RozetEtiketi();
		private final JLabel stokAciklama = new JLabel();
		private final DikeyPanel icerik = new DikeyPanel();
		private final JScrollPane kaydirma;

		UrunDetayi() {
			super(new CardLayout());
			kartlar = (CardLayout) getLayout();
			setPreferredSize(new Dimension(GENISLIK + 58, 10));
			setBorder(new EmptyBorder(18, 20, 20, 10));

			// ---- Seçim yokken ----
			JPanel bos = new JPanel(new GridBagLayout());
			bos.setOpaque(false);
			JComponent logo = Simge.logo(64, new Color(0xF1E6DA), Tema.KARAMEL);
			JLabel b1 = Bilesenler.etiket("Bir ürün seçin", Font.BOLD, 16f, Tema.METIN);
			JLabel b2 = Bilesenler.paragraf("Ürünün içeriğini, alerjenlerini ve stok durumunu burada görürsünüz. "
					+ "Hazırlama, sipariş ve satış işlemleri de buradan yapılır.", 230, 13f, Tema.SOLUK);
			for (JComponent c : new JComponent[] { logo, b1, b2 }) c.setAlignmentX(Component.CENTER_ALIGNMENT);
			b2.setHorizontalAlignment(SwingConstants.CENTER);
			JPanel bosIcerik = new JPanel();
			bosIcerik.setLayout(new javax.swing.BoxLayout(bosIcerik, javax.swing.BoxLayout.Y_AXIS));
			bosIcerik.setOpaque(false);
			bosIcerik.add(logo);
			bosIcerik.add(Box.createVerticalStrut(14));
			bosIcerik.add(b1);
			bosIcerik.add(Box.createVerticalStrut(6));
			bosIcerik.add(b2);
			bos.add(bosIcerik);
			add(bos, "bos");

			// ---- Ürün seçiliyken ----
			JPanel ustSatir = new JPanel(new BorderLayout());
			ustSatir.setOpaque(false);
			ustSatir.add(kategoriRozeti, BorderLayout.WEST);
			ustSatir.add(kodEtiketi, BorderLayout.EAST);
			ustSatir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

			icerikListesi.setOpaque(false);

			alerjenMetni.setFont(Tema.font(Font.PLAIN, 13f));
			alerjenKutusu.add(alerjenSimgesi, BorderLayout.WEST);
			alerjenKutusu.add(alerjenMetni, BorderLayout.CENTER);

			JPanel stokSatiri = new JPanel(new BorderLayout(8, 0));
			stokSatiri.setOpaque(false);
			JLabel adetEtiketi = Bilesenler.etiket("adet hazır", Font.PLAIN, 13f, Tema.SOLUK);
			stokSatiri.add(Bilesenler.yatay(6, stokSayisi, adetEtiketi), BorderLayout.WEST);
			stokSatiri.add(ortalaDikey(stokRozeti), BorderLayout.EAST);
			stokSatiri.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

			Buton hazirlaButonu = new Buton("Hazırla (malzemeden üret)", Simge.Tur.TENCERE, Tema.KAHVE, Buton.Stil.DOLU);
			Buton siparisButonu = new Buton("Sipariş Ver", Simge.Tur.KAMYON, Tema.KARAMEL, Buton.Stil.DOLU);
			Buton satisButonu = new Buton("Satış Yap", Simge.Tur.FIS, Tema.YAPRAK, Buton.Stil.CERCEVE);
			hazirlaButonu.setToolTipText("Malzemeleri stoktan düşerek porsiyon hazırla");
			siparisButonu.setToolTipText("Hazır ürünü tedarikçiden sipariş et, stoğa ekle");
			satisButonu.setToolTipText("Satışı kaydet, ürün stoğunu azalt");
			for (Buton b : new Buton[] { siparisButonu, satisButonu }) b.setBorder(new EmptyBorder(9, 10, 9, 12));
			hazirlaButonu.addActionListener(e -> {
				if (urun != null) hazirla(urun);
			});
			siparisButonu.addActionListener(e -> {
				if (urun != null) siparisVer(urun);
			});
			satisButonu.addActionListener(e -> {
				if (urun != null) satisYap(urun);
			});
			JPanel altButonlar = new JPanel(new GridLayout(1, 2, 8, 0));
			altButonlar.setOpaque(false);
			altButonlar.add(siparisButonu);
			altButonlar.add(satisButonu);
			JPanel butonlar = new JPanel(new GridLayout(0, 1, 0, 8));
			butonlar.setOpaque(false);
			butonlar.add(hazirlaButonu);
			butonlar.add(altButonlar);
			butonlar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2 * 40 + 8));

			Buton icerikDuzenle = new Buton("Düzenle", Simge.Tur.KALEM, Tema.KAHVE, Buton.Stil.CERCEVE);
			icerikDuzenle.setFont(Tema.font(Font.BOLD, 12f));
			icerikDuzenle.setBorder(new EmptyBorder(5, 10, 5, 12));
			icerikDuzenle.addActionListener(e -> duzenle());
			JPanel icerikBaslik = new JPanel(new BorderLayout());
			icerikBaslik.setOpaque(false);
			icerikBaslik.add(ortalaDikey(Bilesenler.bolumBasligi("Ürün içeriği")), BorderLayout.WEST);
			icerikBaslik.add(icerikDuzenle, BorderLayout.EAST);
			icerikBaslik.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

			stokAciklama.setFont(Tema.font(Font.PLAIN, 12.5f));
			stokAciklama.setForeground(Tema.SOLUK);

			JComponent[] parcalar = { ustSatir, adEtiketi, fiyatEtiketi, beslenmeRozetleri, Bilesenler.ayirici(),
					Bilesenler.bolumBasligi("Stok durumu"), stokSatiri, stokAciklama, butonlar, Bilesenler.ayirici(),
					icerikBaslik, icerikListesi, alerjenKutusu };
			int[] bosluk = { 8, 0, 8, 14, 12, 2, 2, 12, 14, 10, 6, 10, 0 };
			for (int i = 0; i < parcalar.length; i++) {
				parcalar[i].setAlignmentX(Component.LEFT_ALIGNMENT);
				icerik.add(parcalar[i]);
				if (bosluk[i] > 0) icerik.add(Box.createVerticalStrut(bosluk[i]));
			}
			icerik.setBorder(new EmptyBorder(0, 0, 0, 8));

			kaydirma = new JScrollPane(icerik);
			kaydirma.setBorder(javax.swing.BorderFactory.createEmptyBorder());
			kaydirma.setViewportBorder(null);
			kaydirma.setOpaque(false);
			kaydirma.getViewport().setOpaque(false);
			kaydirma.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			kaydirma.getVerticalScrollBar().setUnitIncrement(16);
			add(kaydirma, "dolu");
			kartlar.show(this, "bos");
		}

		private JComponent ortalaDikey(JComponent c) {
			JPanel p = new JPanel(new GridBagLayout());
			p.setOpaque(false);
			p.add(c);
			return p;
		}

		void goster(Urun u) {
			urun = u;
			if (u == null) {
				kartlar.show(this, "bos");
				return;
			}
			List<ReceteKalemi> recete;
			try {
				recete = ana.db.receteGetir(u.id());
			} catch (SQLException e) {
				recete = List.of();
			}

			kategoriRozeti.ayarla(Tablolar.kategoriRozeti(u.kategori()));
			kodEtiketi.setText(u.kod());
			adEtiketi.setText(Bilesenler.html(Tema.html(u.ad()), GENISLIK));
			fiyatEtiketi.setText(Tema.para(u.fiyat()));

			List<Rozet> beslenme = new ArrayList<>();
			if (u.vegan()) beslenme.add(new Rozet("Vegan", Tema.YESIL, Tema.YESIL_ACIK));
			if (u.vejetaryen()) beslenme.add(new Rozet("Vejetaryen", Tema.YAPRAK, Tema.YAPRAK_ACIK));
			if (!u.vegan() && !u.vejetaryen()) beslenme.add(new Rozet("Et / balık içerir", Tema.SOLUK, new Color(0xF1EAE3)));
			if (!u.alerjenVar()) beslenme.add(new Rozet("Alerjensiz", Tema.MAVI, Tema.MAVI_ACIK));
			beslenmeRozetleri.ayarla(beslenme);

			icerikListesi.removeAll();
			if (recete.isEmpty()) {
				icerikListesi.add(Bilesenler.etiket("İçerik tanımlı değil", Font.ITALIC, 13f, Tema.SOLUK));
			}
			for (ReceteKalemi k : recete) icerikListesi.add(icerikSatiri(k));

			if (u.alerjenVar()) {
				alerjenKutusu.zemin(Tema.KIRMIZI_ACIK);
				alerjenSimgesi.setIcon(new Simge(Simge.Tur.UYARI, 18, Tema.KIRMIZI));
				alerjenMetni.setForeground(Tema.KIRMIZI);
				alerjenMetni.setText(Bilesenler.html("<b>Alerjen uyarısı:</b> " + Tema.html(u.alerjenler()) + " içerir.",
						GENISLIK - 60));
			} else {
				alerjenKutusu.zemin(Tema.MAVI_ACIK);
				alerjenSimgesi.setIcon(new Simge(Simge.Tur.ONAY, 18, Tema.MAVI));
				alerjenMetni.setForeground(Tema.MAVI);
				alerjenMetni.setText(Bilesenler.html("Bilinen bir alerjen içermez.", GENISLIK - 60));
			}

			StokDurumu d = u.durum();
			stokSayisi.setText(String.valueOf(u.stok()));
			stokSayisi.setForeground(d.dusukMu() ? d.renk : Tema.METIN);
			stokRozeti.ayarla(List.of(new Rozet(d.etiket, d.renk, d.acikRenk)));
			String hazirlanabilir;
			if (u.hazirlanabilir() < 0) {
				hazirlanabilir = "İçerik tanımlı olmadığı için hazırlanamaz.";
			} else if (u.hazirlanabilir() == 0) {
				hazirlanabilir = "<span style='color:" + Tema.renkKodu(Tema.KIRMIZI) + "'><b>Malzeme yetersiz:</b> "
						+ "şu an hazırlanamaz, eksikleri sipariş edin.</span>";
			} else {
				hazirlanabilir = "Mevcut malzemelerle <b>" + u.hazirlanabilir() + " porsiyon</b> daha hazırlanabilir.";
			}
			stokAciklama.setText(Bilesenler.html("Kritik seviye: " + u.minStok() + " adet<br>" + hazirlanabilir,
					GENISLIK));

			kartlar.show(this, "dolu");
			icerik.revalidate();
			icerik.repaint();
			SwingUtilities.invokeLater(() -> kaydirma.getVerticalScrollBar().setValue(0));
		}

		private JComponent icerikSatiri(ReceteKalemi k) {
			Color nokta;
			String ipucu;
			if (k.malzemeStok() < k.miktar()) {
				nokta = Tema.KIRMIZI;
				ipucu = "Eksik! Bir porsiyon için bile yetmiyor.";
			} else if (k.malzemeStok() <= k.malzemeMinStok()) {
				nokta = Tema.TURUNCU;
				ipucu = "Malzeme kritik seviyede.";
			} else {
				nokta = new Color(0x66BB6A);
				ipucu = "Malzeme yeterli.";
			}
			JPanel p = new JPanel(new BorderLayout(10, 0));
			p.setOpaque(false);
			p.setBorder(new EmptyBorder(3, 0, 3, 0));
			p.setToolTipText(ipucu + " Stokta: " + Tema.miktar(k.malzemeStok()) + " " + k.birim());
			JLabel isaret = new JLabel(new NoktaSimgesi(nokta));
			String ad = Tema.html(k.malzemeAdi());
			if (k.alerjen() != null && !k.alerjen().isBlank()) {
				ad += " <span style='color:" + Tema.renkKodu(Tema.KIRMIZI) + "'>(" + Tema.html(k.alerjen()) + ")</span>";
			}
			JLabel adEtk = new JLabel("<html>" + ad + "</html>");
			adEtk.setFont(Tema.font(Font.PLAIN, 13f));
			adEtk.setForeground(Tema.METIN);
			JLabel miktar = Bilesenler.etiket(Tema.miktar(k.miktar()) + " " + k.birim(), Font.BOLD, 12.5f, Tema.SOLUK);
			p.add(isaret, BorderLayout.WEST);
			p.add(adEtk, BorderLayout.CENTER);
			p.add(miktar, BorderLayout.EAST);
			return p;
		}
	}

	/** Malzeme satırlarının başındaki renkli nokta. */
	private record NoktaSimgesi(Color renk) implements Icon {
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = Tema.g2(g);
			g2.setColor(renk);
			g2.fillOval(x, y + 2, 8, 8);
			g2.dispose();
		}

		@Override
		public int getIconWidth() {
			return 8;
		}

		@Override
		public int getIconHeight() {
			return 12;
		}
	}
}

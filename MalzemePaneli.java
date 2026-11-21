package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.Hap;
import KafeMenu.Bilesenler.Kart;

/** "Malzemeler & Stok" sekmesi: yemek ve tatlılarda kullanılan tüm malzemeler, stokları ve siparişleri. */
final class MalzemePaneli extends JPanel {

	private final AnaPencere ana;
	private final MalzemeModeli model = new MalzemeModeli();
	private final Tablolar.BosDurumTablo tablo = new Tablolar.BosDurumTablo(model);
	private final TableRowSorter<MalzemeModeli> siralayici = new TableRowSorter<>(model);

	private final Alan arama = new Alan("Malzeme veya alerjen ara…   (ör. süt, gluten, avokado)", Simge.Tur.ARA);
	private final JLabel sonucEtiketi = Bilesenler.etiket(" ", Font.PLAIN, 12.5f, Tema.SOLUK);
	private final Hap dusukHap = new Hap("Kritik stok", Tema.TURUNCU);
	private final Hap alerjenHap = new Hap("Alerjen içeren", Tema.KIRMIZI);
	private final Hap kullanilmayanHap = new Hap("Hiçbir üründe kullanılmayan", Tema.KAHVE);

	private final Buton duzenleButonu = new Buton("Düzenle", Simge.Tur.KALEM, Tema.KAHVE, Buton.Stil.CERCEVE);
	private final Buton silButonu = new Buton("Sil", Simge.Tur.COP, Tema.KIRMIZI, Buton.Stil.CERCEVE);
	private final Buton siparisButonu = new Buton("Sipariş Ver", Simge.Tur.KAMYON, Tema.KARAMEL, Buton.Stil.DOLU);

	MalzemePaneli(AnaPencere ana) {
		super(new BorderLayout(0, 14));
		this.ana = ana;
		setOpaque(false);
		add(filtreKarti(), BorderLayout.NORTH);
		add(tabloKarti(), BorderLayout.CENTER);
		tabloyuAyarla();
		butonlariGuncelle();
	}

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

		for (Hap h : new Hap[] { dusukHap, alerjenHap, kullanilmayanHap }) h.addActionListener(e -> filtrele(false));
		JPanel satir2 = new JPanel(new BorderLayout());
		satir2.setOpaque(false);
		satir2.add(Bilesenler.yatay(8, Bilesenler.etiket("Filtre", Font.BOLD, 12.5f, Tema.KAHVE), dusukHap, alerjenHap,
				kullanilmayanHap), BorderLayout.WEST);
		k.add(satir1, BorderLayout.NORTH);
		k.add(satir2, BorderLayout.CENTER);
		return k;
	}

	private JComponent tabloKarti() {
		Kart k = new Kart(new BorderLayout(0, 12));
		JLabel baslik = Bilesenler.etiket("Malzemeler ve Stok", Font.BOLD, 17f, Tema.METIN);
		JLabel alt = Bilesenler.etiket("MySQL tablosu: kafe_malzemeler  ·  Yemek, içecek ve tatlılarda kullanılan tüm malzemeler",
				Font.PLAIN, 12f, Tema.SOLUK);
		Buton yeni = new Buton("Yeni Malzeme", Simge.Tur.ARTI, Tema.KARAMEL, Buton.Stil.DOLU);
		Buton toplu = new Buton("Kritikleri Toplu Sipariş Ver", Simge.Tur.KAMYON, Tema.TURUNCU, Buton.Stil.CERCEVE);
		yeni.addActionListener(e -> yeniMalzeme());
		duzenleButonu.addActionListener(e -> duzenle());
		silButonu.addActionListener(e -> sil());
		siparisButonu.addActionListener(e -> siparisVer());
		toplu.addActionListener(e -> topluSiparis());
		JPanel ust = new JPanel(new BorderLayout(16, 0));
		ust.setOpaque(false);
		ust.add(Bilesenler.dikey(2, baslik, alt), BorderLayout.CENTER);
		ust.add(Bilesenler.yatay(8, yeni, duzenleButonu, silButonu, Box.createHorizontalStrut(6),
				Bilesenler.dikeyAyirici(), Box.createHorizontalStrut(6), siparisButonu, toplu), BorderLayout.EAST);
		k.add(ust, BorderLayout.NORTH);
		k.add(Tablolar.kaydirma(tablo), BorderLayout.CENTER);
		return k;
	}

	private void tabloyuAyarla() {
		Tablolar.bicimle(tablo, 42);
		tablo.setRowSorter(siralayici);
		tablo.bosMesaj("Aramanıza uyan malzeme bulunamadı");
		Tablolar.sutun(tablo, 0, 70, new Tablolar.MetinRenderer(Font.BOLD, Tema.SOLUK,
				v -> String.format("M-%03d", (Integer) v)));
		Tablolar.sutun(tablo, 1, 200, new Tablolar.MetinRenderer(Font.BOLD, Tema.METIN, null));
		Tablolar.sutun(tablo, 2, 190, new Tablolar.StokRenderer(false));
		Tablolar.sutun(tablo, 3, 110, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
		Tablolar.sutun(tablo, 4, 100, new Tablolar.RozetRenderer(Tablolar::durumRozeti));
		Tablolar.sutun(tablo, 5, 190, new Tablolar.RozetRenderer(Tablolar::alerjenRozetleri));
		Tablolar.sutun(tablo, 6, 80, new Tablolar.RozetRenderer(Tablolar.evetRozeti("Vegan", Tema.YESIL, Tema.YESIL_ACIK)));
		Tablolar.sutun(tablo, 7, 100,
				new Tablolar.RozetRenderer(Tablolar.evetRozeti("Vejetaryen", Tema.YAPRAK, Tema.YAPRAK_ACIK)));
		Tablolar.sutun(tablo, 8, 130, new Tablolar.MetinRenderer(Font.PLAIN, Tema.METIN,
				v -> (Integer) v == 0 ? "—" : v + " üründe"));

		tablo.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) butonlariGuncelle();
		});
		tablo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && tablo.rowAtPoint(e.getPoint()) >= 0) duzenle();
			}
		});
		tablo.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "sil");
		tablo.getActionMap().put("sil", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sil();
			}
		});
	}

	void veriYukle(List<Malzeme> malzemeler) {
		Malzeme secili = secili();
		model.ayarla(malzemeler);
		filtrele(false);
		if (secili != null) sec(secili.id());
	}

	void filtreleriTemizle() {
		arama.setText("");
		for (Hap h : new Hap[] { dusukHap, alerjenHap, kullanilmayanHap }) h.setSelected(false);
		filtrele(false);
	}

	void dusukStokGoster() {
		filtreleriTemizle();
		dusukHap.setSelected(true);
		filtrele(false);
	}

	private void filtrele(boolean araButonu) {
		String[] kelimeler = Tema.kucuk(arama.getText()).trim().split("\\s+");
		boolean dusuk = dusukHap.isSelected(), alerjen = alerjenHap.isSelected();
		boolean kullanilmayan = kullanilmayanHap.isSelected();
		siralayici.setRowFilter(new RowFilter<MalzemeModeli, Integer>() {
			@Override
			public boolean include(Entry<? extends MalzemeModeli, ? extends Integer> giris) {
				Malzeme m = model.malzeme(giris.getIdentifier());
				if (dusuk && !m.durum().dusukMu()) return false;
				if (alerjen && !m.alerjenVar()) return false;
				if (kullanilmayan && m.kullanimSayisi() > 0) return false;
				String metin = Tema.kucuk(m.kod() + " " + m.ad() + " " + m.birim() + " "
						+ (m.alerjen() == null ? "" : m.alerjen()) + " " + m.durum().etiket);
				for (String k : kelimeler) {
					if (!k.isEmpty() && !metin.contains(k)) return false;
				}
				return true;
			}
		});
		int gorunen = tablo.getRowCount();
		sonucEtiketi.setText(gorunen == model.getRowCount() ? model.getRowCount() + " malzeme listeleniyor"
				: model.getRowCount() + " malzemeden " + gorunen + " tanesi gösteriliyor");
		if (araButonu) {
			if (gorunen > 0) {
				tablo.setRowSelectionInterval(0, 0);
				tablo.scrollRectToVisible(tablo.getCellRect(0, 0, true));
				tablo.requestFocusInWindow();
			} else {
				Bilesenler.bildirim(ana, "“" + arama.getText().trim() + "” için malzeme bulunamadı.", false);
			}
		}
	}

	private Malzeme secili() {
		int satir = tablo.getSelectedRow();
		return satir < 0 ? null : model.malzeme(tablo.convertRowIndexToModel(satir));
	}

	private void sec(int id) {
		int m = model.satirBul(id);
		if (m < 0) return;
		int v = tablo.convertRowIndexToView(m);
		if (v < 0) return;
		tablo.setRowSelectionInterval(v, v);
		tablo.scrollRectToVisible(tablo.getCellRect(v, 0, true));
	}

	private void butonlariGuncelle() {
		boolean var = secili() != null;
		duzenleButonu.setEnabled(var);
		silButonu.setEnabled(var);
		siparisButonu.setEnabled(var);
	}

	// ---------------- İşlemler ----------------

	private void yeniMalzeme() {
		MalzemeDialog d = new MalzemeDialog(ana, null);
		if (d.ac()) {
			filtreleriTemizle();
			ana.yenile();
			sec(d.kaydedilenId());
			ana.basari("“" + d.kaydedilenAd() + "” malzemelere eklendi.");
		}
	}

	private void duzenle() {
		Malzeme m = secili();
		if (m == null) return;
		MalzemeDialog d = new MalzemeDialog(ana, m);
		if (d.ac()) {
			ana.yenile();
			sec(m.id());
			ana.basari("“" + d.kaydedilenAd() + "” güncellendi.");
		}
	}

	private void sil() {
		Malzeme m = secili();
		if (m == null) return;
		if (m.kullanimSayisi() > 0) {
			Diyaloglar.uyari(ana, "Malzeme kullanımda", "“" + m.ad() + "” " + m.kullanimSayisi()
					+ " ürünün içeriğinde kullanılıyor. Silmeden önce bu ürünlerin içeriğinden çıkarın.");
			return;
		}
		if (Diyaloglar.onayla(ana, "Malzemeyi Sil", "“" + m.ad() + "” kalıcı olarak silinecek.\nBu işlem geri alınamaz.",
				"Evet, Sil", Tema.KIRMIZI)) {
			try {
				ana.db.malzemeSil(m.id());
				ana.yenile();
				ana.basari("“" + m.ad() + "” silindi.");
			} catch (Exception e) {
				Diyaloglar.hata(ana, "Malzeme silinemedi", e);
			}
		}
	}

	private void siparisVer() {
		Malzeme m = secili();
		if (m == null) return;
		boolean kucukBirim = "gr".equals(m.birim()) || "ml".equals(m.birim());
		double oneri = Math.ceil(Math.max(m.minStok() * 3 - m.stok(), Math.max(m.minStok(), 1)));
		String bilgi = "Stokta <b>" + Tema.miktar(m.stok()) + " " + Tema.html(m.birim()) + "</b> var · kritik seviye "
				+ Tema.miktar(m.minStok()) + " " + Tema.html(m.birim()) + ".<br>Sipariş edilen miktar stoğa eklenir.";
		Diyaloglar.MiktarDialog d = new Diyaloglar.MiktarDialog(ana, "Malzeme Siparişi", m.ad(), Simge.Tur.KAMYON,
				"Sipariş Ver", Tema.KARAMEL, bilgi, m.birim(), oneri, kucukBirim ? 100 : 1, false,
				"Tedarikçi adı veya not",
				miktar -> "Sipariş sonrası stok: " + Tema.miktar(m.stok() + miktar) + " " + m.birim(),
				(miktar, not) -> ana.db.siparisVer(false, m.id(), miktar, not));
		if (d.ac()) {
			ana.yenile();
			sec(m.id());
			ana.basari(Tema.miktar(d.miktar()) + " " + m.birim() + " “" + m.ad() + "” sipariş edildi.");
		}
	}

	private void topluSiparis() {
		List<Diyaloglar.TopluSiparisDialog.Satir> satirlar = new ArrayList<>();
		for (Malzeme m : ana.malzemeler()) {
			if (m.durum().dusukMu()) {
				double hedef = m.minStok() * 3;
				satirlar.add(new Diyaloglar.TopluSiparisDialog.Satir(m.id(), m.ad(), m.birim(), m.stok(), hedef,
						Math.ceil(Math.max(hedef - m.stok(), 1))));
			}
		}
		if (satirlar.isEmpty()) {
			Diyaloglar.bilgi(ana, "Kritik malzeme yok", "Tüm malzemelerin stoğu kritik seviyenin üzerinde.");
			return;
		}
		Diyaloglar.TopluSiparisDialog d = new Diyaloglar.TopluSiparisDialog(ana, "Kritik Malzemeleri Sipariş Ver",
				satirlar.size() + " malzeme kritik seviyede",
				"Önerilen miktarlar her malzemeyi <b>kritik seviyenin 3 katına</b> tamamlar. "
						+ "İstediğiniz miktarı hücreye tıklayarak değiştirebilirsiniz; 0 yazdığınız satır sipariş edilmez.",
				satirlar, "Hedef stok", "Hepsini Sipariş Ver", "Toplu kritik stok siparişi");
		if (d.ac()) {
			ana.yenile();
			ana.basari(satirlar.size() + " kritik malzeme sipariş edildi, stoklar güncellendi.");
		}
	}

	// ---------------- Tablo modeli ----------------

	static final class MalzemeModeli extends AbstractTableModel {
		private static final String[] SUTUNLAR = { "ID", "Malzeme", "Stok", "Kritik Seviye", "Durum", "Alerjen",
				"Vegan", "Vejetaryen", "Kullanıldığı Ürün" };
		private List<Malzeme> veri = new ArrayList<>();

		void ayarla(List<Malzeme> liste) {
			veri = new ArrayList<>(liste);
			fireTableDataChanged();
		}

		Malzeme malzeme(int satir) {
			return veri.get(satir);
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
				case 0, 8 -> Integer.class;
				case 2 -> Tablolar.StokGorunumu.class;
				case 4 -> StokDurumu.class;
				case 6, 7 -> Boolean.class;
				default -> String.class;
			};
		}

		@Override
		public Object getValueAt(int r, int c) {
			Malzeme m = veri.get(r);
			return switch (c) {
				case 0 -> m.id();
				case 1 -> m.ad();
				case 2 -> new Tablolar.StokGorunumu(m.stok(), m.minStok(), m.birim());
				case 3 -> Tema.miktar(m.minStok()) + " " + m.birim();
				case 4 -> m.durum();
				case 5 -> m.alerjen();
				case 6 -> m.vegan();
				case 7 -> m.vejetaryen();
				default -> m.kullanimSayisi();
			};
		}
	}
}

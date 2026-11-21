package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.Rozet;
import KafeMenu.Bilesenler.RozetEtiketi;

/**
 * Yeni ürün ekleme ve mevcut ürünü düzenleme penceresi.
 * Ürün içeriği (malzemeler) burada değiştirilir; alerjen, vegan ve vejetaryen bilgisi otomatik hesaplanır.
 */
final class UrunDialog extends Diyaloglar.FormDialog {

	/** İçerik tablosundaki bir satır. */
	private static final class Satir {
		Malzeme malzeme;
		double miktar;

		Satir(Malzeme malzeme, double miktar) {
			this.malzeme = malzeme;
			this.miktar = miktar;
		}
	}

	private final AnaPencere ana;
	private final Urun mevcut;
	private final List<Malzeme> tumMalzemeler;

	private final Alan adAlani = new Alan("ör. Karamel Latte", null);
	private final JComboBox<String> kategoriKutusu = Bilesenler.secimKutusu(new JComboBox<>(Urun.KATEGORILER));
	private final Alan fiyatAlani = new Alan("ör. 95,50", null);
	private final JSpinner stokAlani = Bilesenler.spinner(new JSpinner(new SpinnerNumberModel(10, 0, 100000, 1)), null);
	private final JSpinner minStokAlani = Bilesenler.spinner(new JSpinner(new SpinnerNumberModel(5, 0, 100000, 1)), null);

	private final List<Satir> satirlar = new ArrayList<>();
	private final IcerikModeli icerikModeli = new IcerikModeli();
	private final Tablolar.BosDurumTablo icerikTablosu = new Tablolar.BosDurumTablo(icerikModeli);
	private final RozetEtiketi ozetRozetleri = new RozetEtiketi();

	private int kaydedilenId;
	private String kaydedilenAd;

	UrunDialog(AnaPencere ana, Urun mevcut, List<ReceteKalemi> recete) {
		super(ana, mevcut == null ? "Yeni Ürün" : "Ürünü Düzenle",
				mevcut == null ? "Menüye yeni bir yemek, içecek veya tatlı ekleyin" : mevcut.kod() + " · " + mevcut.ad(),
				mevcut == null ? Simge.Tur.ARTI : Simge.Tur.KALEM,
				mevcut == null ? "Ürünü Ekle" : "Değişiklikleri Kaydet", Tema.KARAMEL);
		this.ana = ana;
		this.mevcut = mevcut;
		this.tumMalzemeler = ana.malzemeler();

		if (mevcut != null) {
			adAlani.setText(mevcut.ad());
			kategoriKutusu.setSelectedItem(mevcut.kategori());
			fiyatAlani.setText(Tema.paraSade(mevcut.fiyat()));
			stokAlani.setValue(mevcut.stok());
			minStokAlani.setValue(mevcut.minStok());
			for (ReceteKalemi k : recete) {
				Malzeme m = malzemeBul(k.malzemeId());
				if (m != null) satirlar.add(new Satir(m, k.miktar()));
			}
		}

		// ---- Temel bilgiler ----
		JPanel satir1 = new JPanel(new BorderLayout(12, 0));
		satir1.setOpaque(false);
		satir1.add(Bilesenler.alanBlok("Ürün adı", adAlani), BorderLayout.CENTER);
		JPanel kategoriBlok = Bilesenler.alanBlok("Kategori", kategoriKutusu);
		kategoriBlok.setPreferredSize(new Dimension(200, kategoriBlok.getPreferredSize().height));
		satir1.add(kategoriBlok, BorderLayout.EAST);
		satir1.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(satir1);
		govde.add(Box.createVerticalStrut(14));

		JPanel satir2 = new JPanel(new GridLayout(1, 3, 12, 0));
		satir2.setOpaque(false);
		satir2.add(Bilesenler.alanBlok("Fiyat (₺)", fiyatAlani));
		satir2.add(Bilesenler.alanBlok("Hazır stok (adet / porsiyon)", stokAlani));
		satir2.add(Bilesenler.alanBlok("Kritik stok seviyesi", minStokAlani));
		satir2.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(satir2);
		govde.add(Box.createVerticalStrut(20));

		// ---- Ürün içeriği ----
		govde.add(Bilesenler.bolumBasligi("Ürün içeriği (malzemeler)"));
		govde.add(Box.createVerticalStrut(4));
		govde.add(Bilesenler.paragraf("Porsiyon başına kullanılan malzemeleri ve miktarlarını girin. Malzemeyi veya "
				+ "miktarı değiştirmek için hücreye tıklayın. Alerjen, vegan ve vejetaryen bilgisi otomatik hesaplanır.",
				640, 12.5f, Tema.SOLUK));
		govde.add(Box.createVerticalStrut(10));

		Tablolar.bicimle(icerikTablosu, 34);
		icerikTablosu.bosMesaj("Henüz malzeme eklenmedi");
		Tablolar.sutun(icerikTablosu, 0, 220, new Tablolar.MetinRenderer(Font.BOLD, Tema.METIN, null));
		Tablolar.sutun(icerikTablosu, 1, 110,
				new Tablolar.MetinRenderer(Font.BOLD, Tema.KARAMEL_KOYU, v -> Tema.miktar((Double) v)));
		Tablolar.sutun(icerikTablosu, 2, 60, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
		Tablolar.sutun(icerikTablosu, 3, 150, new Tablolar.RozetRenderer(Tablolar::alerjenRozetleri));
		Tablolar.sutun(icerikTablosu, 4, 110, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
		JComboBox<Malzeme> malzemeSecici = new JComboBox<>(tumMalzemeler.toArray(new Malzeme[0]));
		malzemeSecici.setFont(Tema.font(Font.PLAIN, 13f));
		malzemeSecici.setMaximumRowCount(14);
		icerikTablosu.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(malzemeSecici));
		icerikTablosu.getColumnModel().getColumn(1).setCellEditor(new Tablolar.SayiEditor());
		icerikTablosu.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		JScrollPane sp = Tablolar.kaydirma(icerikTablosu);
		sp.setBorder(new LineBorder(Tema.CIZGI, 1, true));
		sp.setPreferredSize(new Dimension(680, 230));
		sp.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(sp);
		govde.add(Box.createVerticalStrut(10));

		Buton ekle = new Buton("Malzeme Ekle", Simge.Tur.ARTI, Tema.KAHVE, Buton.Stil.CERCEVE);
		Buton cikar = new Buton("Seçileni Çıkar", Simge.Tur.COP, Tema.KIRMIZI, Buton.Stil.CERCEVE);
		ekle.addActionListener(e -> malzemeEkle());
		cikar.addActionListener(e -> seciliyiCikar());
		JPanel butonlar = Bilesenler.yatay(8, ekle, cikar);
		butonlar.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(butonlar);
		govde.add(Box.createVerticalStrut(16));

		JPanel ozet = Bilesenler.yatay(10, Bilesenler.etiket("Otomatik hesaplanan:", Font.BOLD, 12.5f, Tema.KAHVE),
				ozetRozetleri);
		ozet.setAlignmentX(Component.LEFT_ALIGNMENT);
		govde.add(ozet);

		icerikModeli.addTableModelListener(e -> ozetiGuncelle());
		ozetiGuncelle();
		SwingUtilities.invokeLater(adAlani::requestFocusInWindow);
	}

	int kaydedilenId() {
		return kaydedilenId;
	}

	String kaydedilenAd() {
		return kaydedilenAd;
	}

	private Malzeme malzemeBul(int id) {
		for (Malzeme m : tumMalzemeler) {
			if (m.id() == id) return m;
		}
		return null;
	}

	private void malzemeEkle() {
		if (icerikTablosu.isEditing()) icerikTablosu.getCellEditor().stopCellEditing();
		Set<Integer> kullanilan = new HashSet<>();
		for (Satir s : satirlar) kullanilan.add(s.malzeme.id());
		Malzeme sec = null;
		for (Malzeme m : tumMalzemeler) {
			if (!kullanilan.contains(m.id())) {
				sec = m;
				break;
			}
		}
		if (sec == null) {
			durum("Tüm malzemeler zaten içerikte.", Tema.TURUNCU);
			return;
		}
		satirlar.add(new Satir(sec, 1));
		icerikModeli.fireTableDataChanged();
		int satir = satirlar.size() - 1;
		icerikTablosu.setRowSelectionInterval(satir, satir);
		icerikTablosu.scrollRectToVisible(icerikTablosu.getCellRect(satir, 0, true));
		icerikTablosu.editCellAt(satir, 0);
		icerikTablosu.requestFocusInWindow();
	}

	private void seciliyiCikar() {
		if (icerikTablosu.isEditing()) icerikTablosu.getCellEditor().cancelCellEditing();
		int satir = icerikTablosu.getSelectedRow();
		if (satir < 0) {
			durum("Önce tablodan çıkarılacak malzemeyi seçin.", Tema.TURUNCU);
			return;
		}
		satirlar.remove(satir);
		icerikModeli.fireTableDataChanged();
	}

	private void ozetiGuncelle() {
		List<Malzeme> liste = new ArrayList<>();
		for (Satir s : satirlar) liste.add(s.malzeme);
		Veritabani.Ozet o = Veritabani.ozetHesapla(liste);
		List<Rozet> rozetler = new ArrayList<>();
		if (liste.isEmpty()) {
			rozetler.add(new Rozet("Malzeme ekleyin", Tema.SOLUK, Tema.CIZGI));
		} else {
			rozetler.add(o.vegan() ? new Rozet("Vegan", Tema.YESIL, Tema.YESIL_ACIK)
					: new Rozet("Vegan değil", Tema.SOLUK, Tema.CIZGI));
			rozetler.add(o.vejetaryen() ? new Rozet("Vejetaryen", Tema.YAPRAK, Tema.YAPRAK_ACIK)
					: new Rozet("Vejetaryen değil", Tema.SOLUK, Tema.CIZGI));
			if (o.alerjenler().isEmpty()) {
				rozetler.add(new Rozet("Alerjen yok", Tema.MAVI, Tema.MAVI_ACIK));
			} else {
				for (String a : o.alerjenler().split(", ")) rozetler.add(new Rozet("Alerjen: " + a, Tema.KIRMIZI, Tema.KIRMIZI_ACIK));
			}
		}
		ozetRozetleri.ayarla(rozetler);
	}

	@Override
	protected String kaydet() throws Exception {
		if (icerikTablosu.isEditing() && !icerikTablosu.getCellEditor().stopCellEditing()) {
			return "İçerikte geçersiz bir miktar var.";
		}
		String ad = adAlani.getText().trim();
		if (ad.isEmpty()) return "Ürün adı boş olamaz.";
		if (ad.length() > 100) return "Ürün adı en fazla 100 karakter olabilir.";

		BigDecimal fiyat;
		try {
			fiyat = Tema.paraCoz(fiyatAlani.getText());
		} catch (NumberFormatException e) {
			return "Fiyatı sayı olarak yazın (ör. 95,50).";
		}
		if (fiyat.signum() <= 0) return "Fiyat sıfırdan büyük olmalı.";
		if (fiyat.compareTo(new BigDecimal("99999999")) > 0) return "Fiyat çok büyük.";

		try {
			stokAlani.commitEdit();
			minStokAlani.commitEdit();
		} catch (ParseException e) {
			return "Stok değerlerini sayı olarak yazın.";
		}

		if (satirlar.isEmpty()) return "Ürün içeriğine en az bir malzeme ekleyin.";
		Set<Integer> gorulen = new HashSet<>();
		List<ReceteKalemi> recete = new ArrayList<>();
		for (Satir s : satirlar) {
			if (!gorulen.add(s.malzeme.id())) return "“" + s.malzeme.ad() + "” içerikte birden fazla kez var.";
			if (s.miktar <= 0) return "“" + s.malzeme.ad() + "” için miktar sıfırdan büyük olmalı.";
			recete.add(new ReceteKalemi(s.malzeme.id(), s.malzeme.ad(), s.malzeme.birim(), s.miktar, s.malzeme.stok(),
					s.malzeme.minStok(), s.malzeme.alerjen()));
		}

		Urun u = new Urun(mevcut == null ? 0 : mevcut.id(), ad, (String) kategoriKutusu.getSelectedItem(), fiyat, "",
				"", false, false, ((Number) stokAlani.getValue()).intValue(),
				((Number) minStokAlani.getValue()).intValue(), -1);
		kaydedilenId = ana.db.urunKaydet(u, recete);
		kaydedilenAd = ad;
		return null;
	}

	/** İçerik tablosunun modeli. */
	private final class IcerikModeli extends AbstractTableModel {
		private final String[] sutunlar = { "Malzeme", "Miktar", "Birim", "Alerjen", "Stokta" };

		@Override
		public int getRowCount() {
			return satirlar.size();
		}

		@Override
		public int getColumnCount() {
			return sutunlar.length;
		}

		@Override
		public String getColumnName(int c) {
			return sutunlar[c];
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return c == 0 ? Malzeme.class : c == 1 ? Double.class : String.class;
		}

		@Override
		public boolean isCellEditable(int r, int c) {
			return c == 0 || c == 1;
		}

		@Override
		public Object getValueAt(int r, int c) {
			Satir s = satirlar.get(r);
			return switch (c) {
				case 0 -> s.malzeme;
				case 1 -> s.miktar;
				case 2 -> s.malzeme.birim();
				case 3 -> s.malzeme.alerjen();
				default -> Tema.miktar(s.malzeme.stok()) + " " + s.malzeme.birim();
			};
		}

		@Override
		public void setValueAt(Object deger, int r, int c) {
			Satir s = satirlar.get(r);
			if (c == 0 && deger instanceof Malzeme m) s.malzeme = m;
			if (c == 1 && deger instanceof Double d) s.miktar = d;
			fireTableRowsUpdated(r, r);
		}
	}
}

package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Hap;
import KafeMenu.Bilesenler.Kart;

/**
 * "İşlem Geçmişi" sekmesi: ekleme, düzenleme, fiyat, silme, sipariş, hazırlama ve satış kayıtları.
 * Kayıtlar MySQL'de tutulduğu için program kapatılıp açıldığında da burada görünür.
 */
final class GecmisPaneli extends JPanel {

	private static final DateTimeFormatter BICIM = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm");

	private final GecmisModeli model = new GecmisModeli();
	private final Tablolar.BosDurumTablo tablo = new Tablolar.BosDurumTablo(model);
	private final TableRowSorter<GecmisModeli> siralayici = new TableRowSorter<>(model);
	private final Alan arama = new Alan("Ürün, malzeme veya açıklama ara…", Simge.Tur.ARA);
	private final JLabel sonucEtiketi = Bilesenler.etiket(" ", Font.PLAIN, 12.5f, Tema.SOLUK);
	private String tur = "Tümü";

	GecmisPaneli(AnaPencere ana) {
		super(new BorderLayout(0, 14));
		setOpaque(false);

		Kart filtre = new Kart(new BorderLayout(0, 12));
		filtre.setBorder(new EmptyBorder(14, 18, 16, 18));
		arama.setPreferredSize(new Dimension(470, 40));
		arama.setMaximumSize(new Dimension(470, 40));
		arama.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				filtrele();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				filtrele();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				filtrele();
			}
		});
		JPanel satir1 = new JPanel(new BorderLayout());
		satir1.setOpaque(false);
		satir1.add(Bilesenler.yatay(8, arama), BorderLayout.WEST);
		satir1.add(sonucEtiketi, BorderLayout.EAST);

		ButtonGroup grup = new ButtonGroup();
		List<JComponent> haplar = new ArrayList<>();
		haplar.add(Bilesenler.etiket("İşlem", Font.BOLD, 12.5f, Tema.KAHVE));
		for (String ad : new String[] { "Tümü", "Ekleme", "Düzenleme", "Fiyat", "Silme", "Sipariş", "Hazırlama",
				"Satış" }) {
			Hap h = new Hap(ad, "Tümü".equals(ad) ? Tema.ESPRESSO : Tema.islemRenk(ad)[0]);
			grup.add(h);
			h.setSelected("Tümü".equals(ad));
			h.addActionListener(e -> {
				tur = ad;
				filtrele();
			});
			haplar.add(h);
		}
		JPanel satir2 = new JPanel(new BorderLayout());
		satir2.setOpaque(false);
		satir2.add(Bilesenler.yatay(8, haplar.toArray(new JComponent[0])), BorderLayout.WEST);
		filtre.add(satir1, BorderLayout.NORTH);
		filtre.add(satir2, BorderLayout.CENTER);
		add(filtre, BorderLayout.NORTH);

		Kart k = new Kart(new BorderLayout(0, 12));
		JLabel baslik = Bilesenler.etiket("İşlem Geçmişi", Font.BOLD, 17f, Tema.METIN);
		JLabel alt = Bilesenler.etiket("MySQL tablosu: kafe_islem_gecmisi  ·  Son 1000 işlem, en yenisi üstte",
				Font.PLAIN, 12f, Tema.SOLUK);
		k.add(Bilesenler.dikey(2, baslik, alt), BorderLayout.NORTH);
		k.add(Tablolar.kaydirma(tablo), BorderLayout.CENTER);
		add(k, BorderLayout.CENTER);

		Tablolar.bicimle(tablo, 40);
		tablo.setRowSorter(siralayici);
		tablo.bosMesaj("Kayıt bulunamadı");
		Tablolar.sutun(tablo, 0, 150, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK,
				v -> ((LocalDateTime) v).format(BICIM)));
		Tablolar.sutun(tablo, 1, 110, new Tablolar.RozetRenderer(Tablolar::islemRozeti));
		Tablolar.sutun(tablo, 2, 90, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
		Tablolar.sutun(tablo, 3, 220, new Tablolar.MetinRenderer(Font.BOLD, Tema.METIN, null));
		Tablolar.sutun(tablo, 4, 120, new Tablolar.MetinRenderer(Font.PLAIN, Tema.METIN, null));
		Tablolar.sutun(tablo, 5, 420, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
	}

	void veriYukle(List<Islem> islemler) {
		model.ayarla(islemler);
		filtrele();
	}

	private void filtrele() {
		String[] kelimeler = Tema.kucuk(arama.getText()).trim().split("\\s+");
		siralayici.setRowFilter(new RowFilter<GecmisModeli, Integer>() {
			@Override
			public boolean include(Entry<? extends GecmisModeli, ? extends Integer> giris) {
				Islem i = model.islem(giris.getIdentifier());
				if (!"Tümü".equals(tur) && !tur.equals(i.tur())) return false;
				String metin = Tema.kucuk(i.kalemAdi() + " " + i.kalemTuru() + " " + i.tur() + " "
						+ (i.aciklama() == null ? "" : i.aciklama()));
				for (String k : kelimeler) {
					if (!k.isEmpty() && !metin.contains(k)) return false;
				}
				return true;
			}
		});
		sonucEtiketi.setText(tablo.getRowCount() + " kayıt");
	}

	static final class GecmisModeli extends AbstractTableModel {
		private static final String[] SUTUNLAR = { "Tarih", "İşlem", "Tür", "Ürün / Malzeme", "Miktar", "Açıklama" };
		private List<Islem> veri = new ArrayList<>();

		void ayarla(List<Islem> liste) {
			veri = new ArrayList<>(liste);
			fireTableDataChanged();
		}

		Islem islem(int satir) {
			return veri.get(satir);
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
			return c == 0 ? LocalDateTime.class : String.class;
		}

		@Override
		public Object getValueAt(int r, int c) {
			Islem i = veri.get(r);
			return switch (c) {
				case 0 -> i.tarih();
				case 1 -> i.tur();
				case 2 -> i.kalemTuru();
				case 3 -> i.kalemAdi();
				case 4 -> i.miktar() == null ? "" : Tema.miktar(i.miktar()) + (i.birim() == null ? "" : " " + i.birim());
				default -> i.aciklama();
			};
		}
	}
}

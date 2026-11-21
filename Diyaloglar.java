package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.Kutu;

/** Uygulamadaki açılır pencereler (form, onay, mesaj, fiyat, miktar, toplu sipariş, sistem kontrolü). */
final class Diyaloglar {

	private Diyaloglar() {
	}

	static Window pencere(Component c) {
		if (c == null) return null;
		return c instanceof Window w ? w : SwingUtilities.getWindowAncestor(c);
	}

	// =====================================================================
	// Tüm formların ortak iskeleti: başlık + içerik + (hata mesajı, Vazgeç, Kaydet)
	// =====================================================================

	abstract static class FormDialog extends JDialog {
		protected final JPanel govde = new JPanel();
		protected final Buton tamamButonu;
		protected final Buton iptalButonu;
		private final JLabel durumEtiketi = new JLabel(" ");
		private boolean onaylandi;

		FormDialog(Window sahip, String baslik, String altBaslik, Simge.Tur simge, String tamamMetni, Color renk) {
			super(sahip, baslik, ModalityType.APPLICATION_MODAL);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);

			JPanel kok = new JPanel(new BorderLayout());
			kok.setBackground(Tema.ARKA);
			kok.add(baslikPaneli(baslik, altBaslik, simge, renk), BorderLayout.NORTH);

			govde.setOpaque(false);
			govde.setLayout(new BoxLayout(govde, BoxLayout.Y_AXIS));
			govde.setBorder(new EmptyBorder(18, 24, 6, 24));
			kok.add(govde, BorderLayout.CENTER);

			tamamButonu = new Buton(tamamMetni, null, renk, Buton.Stil.DOLU);
			iptalButonu = new Buton("Vazgeç", null, Tema.KAHVE, Buton.Stil.CERCEVE);
			durumEtiketi.setFont(Tema.font(Font.BOLD, 12.5f));
			durumEtiketi.setForeground(Tema.KIRMIZI);

			JPanel alt = new JPanel(new BorderLayout(14, 0));
			alt.setOpaque(false);
			alt.setBorder(new EmptyBorder(12, 24, 20, 24));
			alt.add(durumEtiketi, BorderLayout.CENTER);
			alt.add(Bilesenler.yatay(8, iptalButonu, tamamButonu), BorderLayout.EAST);
			kok.add(alt, BorderLayout.SOUTH);
			setContentPane(kok);

			tamamButonu.addActionListener(e -> tamam());
			iptalButonu.addActionListener(e -> dispose());
			getRootPane().setDefaultButton(tamamButonu);
			getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
			setIconImages(Simge.uygulamaSimgeleri());
		}

		private static JComponent baslikPaneli(String baslik, String altBaslik, Simge.Tur simge, Color renk) {
			JPanel p = new JPanel(new BorderLayout(14, 0));
			p.setBackground(Tema.KART);
			p.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.CIZGI),
					new EmptyBorder(18, 24, 16, 24)));
			if (simge != null) {
				JComponent daire = new JComponent() {
					@Override
					protected void paintComponent(java.awt.Graphics g) {
						java.awt.Graphics2D g2 = Tema.g2(g);
						g2.setColor(Tema.karistir(renk, Color.WHITE, 0.86));
						g2.fillOval(0, 0, 42, 42);
						new Simge(simge, 20, renk).paintIcon(this, g2, 11, 11);
						g2.dispose();
					}
				};
				daire.setPreferredSize(new Dimension(42, 42));
				p.add(daire, BorderLayout.WEST);
			}
			JLabel b = Bilesenler.etiket(baslik, Font.BOLD, 17f, Tema.METIN);
			JLabel a = Bilesenler.etiket(altBaslik == null ? " " : altBaslik, Font.PLAIN, 12.5f, Tema.SOLUK);
			p.add(Bilesenler.dikey(2, b, a), BorderLayout.CENTER);
			return p;
		}

		/**
		 * Kaydet'e basılınca çalışır.
		 * @return null -> başarılı, pencere kapanır; metin -> hata mesajı gösterilir; "" -> pencere açık kalır
		 */
		protected abstract String kaydet() throws Exception;

		private void tamam() {
			String sonuc;
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				sonuc = kaydet();
			} catch (Exception e) {
				sonuc = Veritabani.hataMesaji(e);
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
			if (sonuc == null) {
				onaylandi = true;
				dispose();
			} else if (!sonuc.isEmpty()) {
				durum("<html>" + Tema.html(sonuc) + "</html>", Tema.KIRMIZI);
			}
		}

		protected void durum(String metin, Color renk) {
			durumEtiketi.setForeground(renk);
			durumEtiketi.setText(metin);
		}

		/** Pencereyi gösterir ve kapanana kadar bekler. Kaydet ile kapandıysa true döner. */
		boolean ac() {
			pack();
			setResizable(false);
			setLocationRelativeTo(getOwner());
			setVisible(true);
			return onaylandi;
		}
	}

	// =====================================================================
	// Mesaj, onay, metin isteme
	// =====================================================================

	private static FormDialog mesajPenceresi(Component sahip, String baslik, String mesaj, Simge.Tur simge,
			Color renk, String tamamMetni, boolean iptalVar) {
		FormDialog d = new FormDialog(pencere(sahip), baslik, null, simge, tamamMetni, renk) {
			@Override
			protected String kaydet() {
				return null;
			}
		};
		d.govde.add(Bilesenler.paragraf(Tema.html(mesaj).replace("\n", "<br>"), 380, 13.5f, Tema.METIN));
		d.iptalButonu.setVisible(iptalVar);
		return d;
	}

	static void bilgi(Component sahip, String baslik, String mesaj) {
		mesajPenceresi(sahip, baslik, mesaj, Simge.Tur.ONAY, Tema.YESIL, "Tamam", false).ac();
	}

	static void uyari(Component sahip, String baslik, String mesaj) {
		mesajPenceresi(sahip, baslik, mesaj, Simge.Tur.UYARI, Tema.TURUNCU, "Tamam", false).ac();
	}

	static void hata(Component sahip, String baslik, Throwable e) {
		mesajPenceresi(sahip, baslik, Veritabani.hataMesaji(e), Simge.Tur.UYARI, Tema.KIRMIZI, "Tamam", false).ac();
	}

	static boolean onayla(Component sahip, String baslik, String mesaj, String evetMetni, Color renk) {
		return mesajPenceresi(sahip, baslik, mesaj, Simge.Tur.UYARI, renk, evetMetni, true).ac();
	}

	/** Tek satırlık metin ister. Vazgeçilirse null döner. */
	static String metinIste(Component sahip, String baslik, String aciklama, String mevcut) {
		Alan alan = new Alan(null, null);
		alan.setText(mevcut);
		String[] sonuc = new String[1];
		FormDialog d = new FormDialog(pencere(sahip), baslik, aciklama, Simge.Tur.KALEM, "Kaydet", Tema.KARAMEL) {
			@Override
			protected String kaydet() {
				String s = alan.getText().trim();
				if (s.isEmpty()) return "Boş bırakılamaz.";
				if (s.length() > 60) return "En fazla 60 karakter olabilir.";
				sonuc[0] = s;
				return null;
			}
		};
		alan.setPreferredSize(new Dimension(380, 38));
		d.govde.add(Bilesenler.alanBlok("Kafe adı", alan));
		SwingUtilities.invokeLater(alan::selectAll);
		return d.ac() ? sonuc[0] : null;
	}

	// =====================================================================
	// Fiyat güncelleme
	// =====================================================================

	static final class FiyatDialog extends FormDialog {
		private final AnaPencere ana;
		private final Urun urun;
		private final Alan yeniFiyat = new Alan("ör. 95,50", null);
		private final JLabel degisim = Bilesenler.etiket(" ", Font.BOLD, 13f, Tema.SOLUK);

		FiyatDialog(AnaPencere ana, Urun urun) {
			super(ana, "Fiyat Güncelle", urun.kod() + " · " + urun.ad(), Simge.Tur.ETIKET, "Fiyatı Kaydet", Tema.MOR);
			this.ana = ana;
			this.urun = urun;

			JLabel mevcut = Bilesenler.etiket(Tema.para(urun.fiyat()), Font.BOLD, 26f, Tema.KARAMEL_KOYU);
			govde.add(Bilesenler.alanBlok("Mevcut fiyat", mevcut));
			govde.add(javax.swing.Box.createVerticalStrut(14));

			yeniFiyat.setText(Tema.paraSade(urun.fiyat()));
			yeniFiyat.setPreferredSize(new Dimension(220, 38));
			JPanel hizli = Bilesenler.yatay(6, hizliButon("+%5", 1.05), hizliButon("+%10", 1.10),
					hizliButon("+%20", 1.20), hizliButon("−%10", 0.90));
			JPanel satir = Bilesenler.yatay(12, yeniFiyat, hizli);
			govde.add(Bilesenler.alanBlok("Yeni fiyat (₺)", satir));
			govde.add(javax.swing.Box.createVerticalStrut(10));
			degisim.setAlignmentX(Component.LEFT_ALIGNMENT);
			govde.add(degisim);

			yeniFiyat.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					onizle();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					onizle();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					onizle();
				}
			});
			onizle();
			SwingUtilities.invokeLater(() -> {
				yeniFiyat.requestFocusInWindow();
				yeniFiyat.selectAll();
			});
		}

		private Buton hizliButon(String metin, double carpan) {
			Buton b = new Buton(metin, null, Tema.MOR, Buton.Stil.CERCEVE);
			b.setBorder(new EmptyBorder(8, 10, 8, 10));
			b.setFont(Tema.font(Font.BOLD, 12f));
			b.addActionListener(e -> {
				BigDecimal yeni = urun.fiyat().multiply(BigDecimal.valueOf(carpan)).setScale(0, RoundingMode.HALF_UP);
				yeniFiyat.setText(Tema.paraSade(yeni));
			});
			return b;
		}

		private void onizle() {
			try {
				BigDecimal yeni = Tema.paraCoz(yeniFiyat.getText());
				BigDecimal fark = yeni.subtract(urun.fiyat());
				if (fark.signum() == 0) {
					degisim.setForeground(Tema.SOLUK);
					degisim.setText("Fiyat değişmedi");
					return;
				}
				double yuzde = fark.doubleValue() / urun.fiyat().doubleValue() * 100;
				degisim.setForeground(fark.signum() > 0 ? Tema.YESIL : Tema.KIRMIZI);
				degisim.setText(String.format(Tema.TR, "Değişim: %s%s  (%s%%%.1f)", fark.signum() > 0 ? "+" : "−",
						Tema.para(fark.abs()), fark.signum() > 0 ? "+" : "−", Math.abs(yuzde)));
			} catch (NumberFormatException e) {
				degisim.setForeground(Tema.KIRMIZI);
				degisim.setText("Geçerli bir fiyat yazın (ör. 95,50)");
			}
		}

		@Override
		protected String kaydet() throws Exception {
			BigDecimal yeni;
			try {
				yeni = Tema.paraCoz(yeniFiyat.getText());
			} catch (NumberFormatException e) {
				return "Fiyatı sayı olarak yazın (ör. 95,50).";
			}
			if (yeni.signum() <= 0) return "Fiyat sıfırdan büyük olmalı.";
			if (yeni.compareTo(new BigDecimal("99999999")) > 0) return "Fiyat çok büyük.";
			if (yeni.compareTo(urun.fiyat()) == 0) return "Yeni fiyat mevcut fiyatla aynı.";
			ana.db.fiyatGuncelle(urun.id(), yeni);
			return null;
		}
	}

	// =====================================================================
	// Miktar isteyen pencere (sipariş, hazırlama, satış)
	// =====================================================================

	@FunctionalInterface
	interface MiktarIslemi {
		void uygula(double miktar, String not) throws Exception;
	}

	static final class MiktarDialog extends FormDialog {
		private final JSpinner sayac;
		private final Alan notAlani;
		private final JLabel onizleme = Bilesenler.etiket(" ", Font.BOLD, 13.5f, Tema.KAHVE);
		private final DoubleFunction<String> onizlemeMetni;
		private final MiktarIslemi islem;
		private final boolean tamSayi;

		MiktarDialog(Window sahip, String baslik, String altBaslik, Simge.Tur simge, String tamamMetni, Color renk,
				String bilgiHtml, String miktarEtiketi, double baslangic, double adim, boolean tamSayi,
				String notIpucu, DoubleFunction<String> onizlemeMetni, MiktarIslemi islem) {
			super(sahip, baslik, altBaslik, simge, tamamMetni, renk);
			this.islem = islem;
			this.onizlemeMetni = onizlemeMetni;
			this.tamSayi = tamSayi;

			if (bilgiHtml != null) {
				Kutu bilgi = new Kutu(new BorderLayout(), Tema.karistir(renk, Color.WHITE, 0.9));
				bilgi.add(Bilesenler.paragraf(bilgiHtml, 380, 13f, Tema.METIN));
				bilgi.setAlignmentX(Component.LEFT_ALIGNMENT);
				govde.add(bilgi);
				govde.add(javax.swing.Box.createVerticalStrut(16));
			}
			sayac = Bilesenler.spinner(new JSpinner(new SpinnerNumberModel(Math.max(0, baslangic), 0d, 10_000_000d, adim)),
					tamSayi ? "#,##0" : "#,##0.##");
			sayac.setPreferredSize(new Dimension(180, 38));
			JPanel sayacSatiri = Bilesenler.yatay(10, sayac, Bilesenler.etiket(miktarEtiketi, Font.PLAIN, 13f, Tema.SOLUK));
			govde.add(Bilesenler.alanBlok("Miktar", sayacSatiri));

			if (notIpucu != null) {
				notAlani = new Alan(notIpucu, null);
				notAlani.setPreferredSize(new Dimension(380, 38));
				govde.add(javax.swing.Box.createVerticalStrut(12));
				govde.add(Bilesenler.alanBlok("Not (isteğe bağlı)", notAlani));
			} else {
				notAlani = null;
			}
			if (onizlemeMetni != null) {
				govde.add(javax.swing.Box.createVerticalStrut(12));
				onizleme.setAlignmentX(Component.LEFT_ALIGNMENT);
				govde.add(onizleme);
				sayac.addChangeListener(e -> onizle());
				onizle();
			}
			SwingUtilities.invokeLater(() -> {
				JComponent tf = ((JSpinner.DefaultEditor) sayac.getEditor()).getTextField();
				tf.requestFocusInWindow();
				((javax.swing.JFormattedTextField) tf).selectAll();
			});
		}

		private void onizle() {
			onizleme.setText(onizlemeMetni.apply(miktar()));
		}

		double miktar() {
			double m = ((Number) sayac.getValue()).doubleValue();
			return tamSayi ? Math.round(m) : m;
		}

		@Override
		protected String kaydet() throws Exception {
			try {
				sayac.commitEdit();
			} catch (ParseException e) {
				return "Miktarı sayı olarak yazın.";
			}
			double m = miktar();
			if (m <= 0) return "Miktar sıfırdan büyük olmalı.";
			islem.uygula(m, notAlani == null ? null : notAlani.getText());
			return null;
		}
	}

	// =====================================================================
	// Toplu malzeme siparişi (eksik malzemeler / kritik malzemeler)
	// =====================================================================

	static final class TopluSiparisDialog extends FormDialog {

		static final class Satir {
			final int malzemeId;
			final String ad;
			final String birim;
			final double mevcut;
			final double hedef;
			double miktar;

			Satir(int malzemeId, String ad, String birim, double mevcut, double hedef, double miktar) {
				this.malzemeId = malzemeId;
				this.ad = ad;
				this.birim = birim;
				this.mevcut = mevcut;
				this.hedef = hedef;
				this.miktar = miktar;
			}
		}

		private final AnaPencere ana;
		private final List<Satir> satirlar;
		private final String not;
		private final Tablolar.BosDurumTablo tablo;

		TopluSiparisDialog(AnaPencere ana, String baslik, String altBaslik, String bilgiHtml, List<Satir> satirlar,
				String hedefBasligi, String tamamMetni, String not) {
			super(ana, baslik, altBaslik, Simge.Tur.KAMYON, tamamMetni, Tema.KARAMEL);
			this.ana = ana;
			this.satirlar = satirlar;
			this.not = not;

			Kutu bilgi = new Kutu(new BorderLayout(), Tema.TURUNCU_ACIK);
			bilgi.add(Bilesenler.paragraf(bilgiHtml, 560, 13f, Tema.METIN));
			bilgi.setAlignmentX(Component.LEFT_ALIGNMENT);
			govde.add(bilgi);
			govde.add(javax.swing.Box.createVerticalStrut(14));

			String[] sutunlar = { "Malzeme", "Stokta", hedefBasligi, "Sipariş Miktarı (tıkla, değiştir)" };
			AbstractTableModel model = new AbstractTableModel() {
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
					return c == 3 ? Double.class : String.class;
				}

				@Override
				public boolean isCellEditable(int r, int c) {
					return c == 3;
				}

				@Override
				public Object getValueAt(int r, int c) {
					Satir s = satirlar.get(r);
					return switch (c) {
						case 0 -> s.ad;
						case 1 -> Tema.miktar(s.mevcut) + " " + s.birim;
						case 2 -> Tema.miktar(s.hedef) + " " + s.birim;
						default -> s.miktar;
					};
				}

				@Override
				public void setValueAt(Object v, int r, int c) {
					if (v instanceof Double d) satirlar.get(r).miktar = d;
					fireTableRowsUpdated(r, r);
				}
			};
			tablo = new Tablolar.BosDurumTablo(model);
			Tablolar.bicimle(tablo, 36);
			Tablolar.sutun(tablo, 0, 190, new Tablolar.MetinRenderer(Font.BOLD, Tema.METIN, null));
			Tablolar.sutun(tablo, 1, 110, new Tablolar.MetinRenderer(Font.PLAIN, Tema.KIRMIZI, null));
			Tablolar.sutun(tablo, 2, 110, new Tablolar.MetinRenderer(Font.PLAIN, Tema.SOLUK, null));
			Tablolar.sutun(tablo, 3, 200, new Tablolar.MetinRenderer(Font.BOLD, Tema.KARAMEL_KOYU,
					v -> Tema.miktar((Double) v)));
			tablo.getColumnModel().getColumn(3).setCellEditor(new Tablolar.SayiEditor());
			tablo.setToolTipText("Sipariş miktarını değiştirmek için hücreye tıklayın");
			JScrollPane sp = Tablolar.kaydirma(tablo);
			sp.setBorder(new LineBorder(Tema.CIZGI, 1, true));
			sp.setPreferredSize(new Dimension(580, Math.min(300, 48 + satirlar.size() * 37)));
			sp.setAlignmentX(Component.LEFT_ALIGNMENT);
			govde.add(sp);
		}

		@Override
		protected String kaydet() throws Exception {
			if (tablo.isEditing() && !tablo.getCellEditor().stopCellEditing()) return "Geçersiz bir miktar var.";
			Map<Integer, Double> miktarlar = new LinkedHashMap<>();
			for (Satir s : satirlar) {
				if (s.miktar > 0) miktarlar.put(s.malzemeId, s.miktar);
			}
			if (miktarlar.isEmpty()) return "En az bir malzeme için miktar girin.";
			ana.db.malzemeSiparisleri(miktarlar, not);
			return null;
		}
	}

	// =====================================================================
	// Sistem kontrolü
	// =====================================================================

	static final class SistemKontroluDialog extends FormDialog {
		private final AnaPencere ana;
		private final JPanel liste = new JPanel(new GridLayout(0, 1, 0, 8));

		SistemKontroluDialog(AnaPencere ana) {
			super(ana, "Sistem Kontrolü", "Ekle → kapatıp aç → fiyat güncelle → sil adımlarını otomatik dener",
					Simge.Tur.KALKAN, "Kontrolü Başlat", Tema.YESIL);
			this.ana = ana;
			iptalButonu.setText("Kapat");
			govde.add(Bilesenler.paragraf("Bu kontrol MySQL'de <b>geçici bir test ürünü</b> oluşturur, programı kapatıp "
					+ "açmış gibi <b>yeni bir bağlantıyla</b> tekrar okur, fiyatını günceller ve siler. "
					+ "Menünüzdeki gerçek ürünlere dokunulmaz.", 470, 13f, Tema.METIN));
			govde.add(javax.swing.Box.createVerticalStrut(16));
			liste.setOpaque(false);
			liste.setAlignmentX(Component.LEFT_ALIGNMENT);
			for (String ad : new String[] { "MySQL bağlantısı", "Tablolar", "Ürün ekle", "Kapatıp açınca kayıt duruyor mu?",
					"Fiyat güncelle", "Ürün sil" }) {
				liste.add(satir(null, ad, "Bekliyor"));
			}
			govde.add(liste);
		}

		private JComponent satir(Boolean basarili, String ad, String detay) {
			Color renk = basarili == null ? Tema.SOLUK : basarili ? Tema.YESIL : Tema.KIRMIZI;
			Kutu k = new Kutu(new BorderLayout(12, 0),
					basarili == null ? new Color(0xF3EDE7) : basarili ? Tema.YESIL_ACIK : Tema.KIRMIZI_ACIK);
			JLabel simge = new JLabel(new Simge(basarili == null ? Simge.Tur.SAAT
					: basarili ? Simge.Tur.ONAY : Simge.Tur.UYARI, 18, renk));
			k.add(simge, BorderLayout.WEST);
			k.add(Bilesenler.dikey(2, Bilesenler.etiket(ad, Font.BOLD, 13.5f, Tema.METIN),
					Bilesenler.etiket(detay, Font.PLAIN, 12.5f, Tema.SOLUK)), BorderLayout.CENTER);
			k.setPreferredSize(new Dimension(470, k.getPreferredSize().height));
			return k;
		}

		@Override
		protected String kaydet() {
			List<KontrolAdimi> sonuclar = ana.db.sistemKontrolu();
			liste.removeAll();
			boolean hepsi = true;
			for (KontrolAdimi a : sonuclar) {
				liste.add(satir(a.basarili(), a.ad(), a.detay()));
				hepsi &= a.basarili();
			}
			tamamButonu.setText("Tekrar Çalıştır");
			durum(hepsi ? "Tüm adımlar başarılı: veriler MySQL'de kalıcı." : "Bazı adımlar başarısız oldu.",
					hepsi ? Tema.YESIL : Tema.KIRMIZI);
			pack();
			return "";
		}
	}
}

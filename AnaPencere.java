package KafeMenu;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.Hap;

/** Uygulamanın ana penceresi: başlık, sekmeler (Menü, Malzemeler, İşlem Geçmişi) ve durum çubuğu. */
final class AnaPencere extends JFrame {

	final Veritabani db;
	private List<Urun> urunler = List.of();
	private List<Malzeme> malzemeler = List.of();

	private final JLabel kafeAdiEtiketi = Bilesenler.etiket("", Font.BOLD, 26f, Tema.KREM_METIN);
	private final JLabel durumEtiketi = Bilesenler.etiket(" ", Font.PLAIN, 12.5f, Tema.SOLUK);

	private final CardLayout kartlar = new CardLayout();
	private final JPanel sayfalar = new JPanel(kartlar);
	private final Hap sekmeMenu = new Hap("Kafe Menüsü", Tema.ESPRESSO);
	private final Hap sekmeMalzeme = new Hap("Malzemeler & Stok", Tema.ESPRESSO);
	private final Hap sekmeGecmis = new Hap("İşlem Geçmişi", Tema.ESPRESSO);

	final MenuPaneli menuPaneli;
	final MalzemePaneli malzemePaneli;
	final GecmisPaneli gecmisPaneli;

	private final IstatistikKutusu urunKutusu;
	private final IstatistikKutusu malzemeKutusu;
	private final IstatistikKutusu kritikUrunKutusu;
	private final IstatistikKutusu kritikMalzemeKutusu;

	AnaPencere(Veritabani db, boolean ornekYuklendi) {
		super("Kafe Menü Atölyesi");
		this.db = db;
		setIconImages(Simge.uygulamaSimgeleri());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				db.kapat();
				System.exit(0);
			}
		});

		urunKutusu = new IstatistikKutusu("Menüdeki ürün", () -> listeGoster("menu", false));
		malzemeKutusu = new IstatistikKutusu("Malzeme çeşidi", () -> listeGoster("malzeme", false));
		kritikUrunKutusu = new IstatistikKutusu("Kritik stokta ürün", () -> listeGoster("menu", true));
		kritikMalzemeKutusu = new IstatistikKutusu("Kritik malzeme", () -> listeGoster("malzeme", true));

		menuPaneli = new MenuPaneli(this);
		malzemePaneli = new MalzemePaneli(this);
		gecmisPaneli = new GecmisPaneli(this);

		JPanel kok = new JPanel(new BorderLayout());
		kok.setBackground(Tema.ARKA);
		JPanel ust = new JPanel(new BorderLayout());
		ust.setOpaque(false);
		ust.add(baslikCubugu(), BorderLayout.NORTH);
		ust.add(sekmeCubugu(), BorderLayout.CENTER);
		kok.add(ust, BorderLayout.NORTH);

		sayfalar.setOpaque(false);
		sayfalar.setBorder(new EmptyBorder(0, 20, 14, 20));
		sayfalar.add(menuPaneli, "menu");
		sayfalar.add(malzemePaneli, "malzeme");
		sayfalar.add(gecmisPaneli, "gecmis");
		kok.add(sayfalar, BorderLayout.CENTER);
		kok.add(durumCubugu(), BorderLayout.SOUTH);
		setContentPane(kok);

		setMinimumSize(new Dimension(1240, 760));
		setSize(1440, 900);
		setLocationRelativeTo(null);
		setExtendedState(MAXIMIZED_BOTH);

		kafeAdiniYukle();
		yenile();
		if (ornekYuklendi) {
			durum("İlk açılış: tablolar oluşturuldu ve örnek menü yüklendi.");
		} else {
			durum("MySQL'den " + urunler.size() + " ürün ve " + malzemeler.size() + " malzeme yüklendi.");
		}
	}

	// =====================================================================
	// Veriler
	// =====================================================================

	/** Tüm listeleri MySQL'den yeniden okur ve ekranları günceller. */
	void yenile() {
		try {
			urunler = db.urunleriGetir();
			malzemeler = db.malzemeleriGetir();
			menuPaneli.veriYukle(urunler);
			malzemePaneli.veriYukle(malzemeler);
			gecmisPaneli.veriYukle(db.gecmisiGetir());
			long kritikUrun = urunler.stream().filter(u -> u.durum().dusukMu()).count();
			long kritikMalzeme = malzemeler.stream().filter(m -> m.durum().dusukMu()).count();
			urunKutusu.ayarla(urunler.size(), false);
			malzemeKutusu.ayarla(malzemeler.size(), false);
			kritikUrunKutusu.ayarla(kritikUrun, kritikUrun > 0);
			kritikMalzemeKutusu.ayarla(kritikMalzeme, kritikMalzeme > 0);
		} catch (SQLException e) {
			Diyaloglar.hata(this, "Veriler yüklenemedi", e);
		}
	}

	List<Malzeme> malzemeler() {
		return malzemeler;
	}

	List<Urun> urunler() {
		return urunler;
	}

	/** Başarılı bir işlemden sonra durum çubuğuna yazar ve sağ altta bildirim gösterir. */
	void basari(String mesaj) {
		durum(mesaj);
		Bilesenler.bildirim(this, mesaj, true);
	}

	void durum(String mesaj) {
		durumEtiketi.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "   " + mesaj);
	}

	/** Başlıktaki sayı kutularına tıklanınca ilgili listeyi (isteğe bağlı yalnızca kritik stok) gösterir. */
	private void listeGoster(String sekme, boolean sadeceKritik) {
		sekmeGoster(sekme);
		if ("menu".equals(sekme)) {
			if (sadeceKritik) menuPaneli.dusukStokGoster();
			else menuPaneli.filtreleriTemizle();
		} else {
			if (sadeceKritik) malzemePaneli.dusukStokGoster();
			else malzemePaneli.filtreleriTemizle();
		}
	}

	void sekmeGoster(String ad) {
		kartlar.show(sayfalar, ad);
		sekmeMenu.setSelected("menu".equals(ad));
		sekmeMalzeme.setSelected("malzeme".equals(ad));
		sekmeGecmis.setSelected("gecmis".equals(ad));
	}

	private void kafeAdiniYukle() {
		try {
			String ad = db.kafeAdi();
			kafeAdiEtiketi.setText(ad);
			setTitle(ad + " · Kafe Menü Atölyesi");
		} catch (SQLException e) {
			kafeAdiEtiketi.setText("Kafe Menü Atölyesi");
		}
	}

	private void kafeAdiniDegistir() {
		String yeni = Diyaloglar.metinIste(this, "Kafe Adını Değiştir", "Başlıkta görünecek kafe adı",
				kafeAdiEtiketi.getText());
		if (yeni == null) return;
		try {
			db.kafeAdiKaydet(yeni);
			kafeAdiniYukle();
			basari("Kafe adı “" + yeni + "” olarak kaydedildi.");
		} catch (SQLException e) {
			Diyaloglar.hata(this, "Kafe adı kaydedilemedi", e);
		}
	}

	// =====================================================================
	// Görünüm
	// =====================================================================

	private JComponent baslikCubugu() {
		JPanel p = new JPanel(new BorderLayout(20, 0)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Tema.g2(g);
				g2.setPaint(new GradientPaint(0, 0, Tema.ESPRESSO, getWidth(), getHeight(), Tema.KAHVE));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(new Color(255, 255, 255, 10));
				g2.fillOval(getWidth() - 300, -110, 260, 260);
				g2.fillOval(getWidth() / 2, 40, 180, 180);
				g2.dispose();
			}
		};
		p.setBorder(new EmptyBorder(18, 24, 18, 24));

		JComponent logo = Simge.logo(56, Tema.KARAMEL, Color.WHITE);
		kafeAdiEtiketi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		kafeAdiEtiketi.setToolTipText("Kafe adını değiştirmek için tıklayın");
		kafeAdiEtiketi.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				kafeAdiniDegistir();
			}
		});
		JLabel alt = Bilesenler.etiket("Menü & Stok Yönetimi  ·  " + db.baglantiBilgisi(), Font.PLAIN, 13f,
				new Color(255, 240, 225, 175));
		JPanel yazilar = Bilesenler.dikey(2, kafeAdiEtiketi, alt);
		JPanel sol = Bilesenler.yatay(16, logo, yazilar);

		Buton yenileButonu = new Buton("Yenile", Simge.Tur.YENILE, Color.WHITE, Buton.Stil.SEFFAF);
		yenileButonu.setToolTipText("Tüm verileri MySQL'den yeniden oku");
		yenileButonu.addActionListener(e -> {
			yenile();
			basari("Veriler MySQL'den yeniden okundu.");
		});
		Buton kontrolButonu = new Buton("Sistem Kontrolü", Simge.Tur.KALKAN, Color.WHITE, Buton.Stil.SEFFAF);
		kontrolButonu.setToolTipText("Ekle, fiyat güncelle, sil ve kalıcılık testini otomatik çalıştır");
		kontrolButonu.addActionListener(e -> {
			new Diyaloglar.SistemKontroluDialog(this).ac();
			yenile();
		});
		JPanel sag = Bilesenler.yatay(10, urunKutusu, malzemeKutusu, kritikUrunKutusu, kritikMalzemeKutusu,
				Box.createHorizontalStrut(10), yenileButonu, kontrolButonu);

		p.add(ortala(sol), BorderLayout.WEST);
		p.add(ortala(sag), BorderLayout.EAST);
		return p;
	}

	private static JPanel ortala(JComponent c) {
		JPanel p = new JPanel(new GridBagLayout());
		p.setOpaque(false);
		p.add(c);
		return p;
	}

	private JComponent sekmeCubugu() {
		JPanel p = new JPanel(new BorderLayout());
		p.setOpaque(false);
		p.setBorder(new EmptyBorder(14, 20, 12, 20));
		ButtonGroup grup = new ButtonGroup();
		sekmeMenu.setIcon(new Simge(Simge.Tur.LISTE, 15, null));
		sekmeMalzeme.setIcon(new Simge(Simge.Tur.KUTU, 15, null));
		sekmeGecmis.setIcon(new Simge(Simge.Tur.SAAT, 15, null));
		for (Hap h : new Hap[] { sekmeMenu, sekmeMalzeme, sekmeGecmis }) {
			grup.add(h);
			h.setFont(Tema.font(Font.BOLD, 13.5f));
			h.setBorder(new EmptyBorder(9, 18, 9, 20));
		}
		sekmeMenu.setSelected(true);
		sekmeMenu.addActionListener(e -> sekmeGoster("menu"));
		sekmeMalzeme.addActionListener(e -> sekmeGoster("malzeme"));
		sekmeGecmis.addActionListener(e -> sekmeGoster("gecmis"));
		p.add(Bilesenler.yatay(8, sekmeMenu, sekmeMalzeme, sekmeGecmis), BorderLayout.WEST);
		JLabel ipucu = Bilesenler.etiket("İpucu: Bir satıra çift tıklayarak düzenleyebilir, Delete tuşuyla silebilirsiniz.",
				Font.PLAIN, 12.5f, Tema.SOLUK);
		p.add(ortala(ipucu), BorderLayout.EAST);
		return p;
	}

	private JComponent durumCubugu() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Tema.KART);
		p.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Tema.CIZGI), new EmptyBorder(7, 20, 7, 20)));
		JLabel baglanti = Bilesenler.etiket("MySQL'e bağlı: " + db.baglantiBilgisi(), Font.PLAIN, 12.5f, Tema.SOLUK);
		baglanti.setIcon(new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2 = Tema.g2(g);
				g2.setColor(new Color(0x43A047));
				g2.fillOval(x, y + 1, 8, 8);
				g2.dispose();
			}

			@Override
			public int getIconWidth() {
				return 8;
			}

			@Override
			public int getIconHeight() {
				return 10;
			}
		});
		baglanti.setIconTextGap(8);
		p.add(baglanti, BorderLayout.WEST);
		p.add(durumEtiketi, BorderLayout.EAST);
		return p;
	}

	/** Başlıktaki tıklanabilir sayı kutuları. */
	private static final class IstatistikKutusu extends JComponent {
		private final String etiket;
		private String deger = "–";
		private boolean uyari;
		private boolean ustunde;

		IstatistikKutusu(String etiket, Runnable tiklaninca) {
			this.etiket = etiket;
			Dimension d = new Dimension(132, 58);
			setPreferredSize(d);
			setMaximumSize(d);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Listede göster");
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					ustunde = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					ustunde = false;
					repaint();
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					tiklaninca.run();
				}
			});
		}

		void ayarla(long sayi, boolean uyariVar) {
			deger = String.valueOf(sayi);
			uyari = uyariVar;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 14, 14);
			g2.setColor(uyari ? new Color(255, 167, 38, ustunde ? 70 : 48) : new Color(255, 255, 255, ustunde ? 34 : 20));
			g2.fill(r);
			g2.setColor(uyari ? new Color(255, 183, 77, 140) : new Color(255, 255, 255, 40));
			g2.draw(r);
			g2.setFont(Tema.font(Font.BOLD, 21f));
			FontMetrics fm = g2.getFontMetrics();
			g2.setColor(uyari ? new Color(0xFFCC80) : Color.WHITE);
			g2.drawString(deger, 14, 8 + fm.getAscent());
			g2.setFont(Tema.font(Font.PLAIN, 11.5f));
			g2.setColor(new Color(255, 240, 225, 200));
			g2.drawString(etiket, 14, getHeight() - 11);
			g2.dispose();
		}
	}
}

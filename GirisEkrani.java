package KafeMenu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import KafeMenu.Bilesenler.Alan;
import KafeMenu.Bilesenler.Buton;
import KafeMenu.Bilesenler.SifreAlani;

/**
 * MySQL giriş ekranı. Şifre KODDA YAZMAZ ve DİSKE KAYDEDİLMEZ;
 * kullanıcı her açılışta bu ekrandan girer. Sunucu, port, veritabanı ve kullanıcı adı ise kolaylık olsun
 * diye hatırlanır.
 */
final class GirisEkrani extends JFrame {

	private final Alan sunucuAlani = new Alan("localhost", Simge.Tur.SUNUCU);
	private final Alan portAlani = new Alan("3306", null);
	private final Alan veritabaniAlani = new Alan("java_kursu", null);
	private final Alan kullaniciAlani = new Alan("root", null);
	private final SifreAlani sifreAlani = new SifreAlani("MySQL şifreniz");
	private final JLabel hataEtiketi = Bilesenler.etiket(" ", Font.BOLD, 12.5f, Tema.KIRMIZI);
	private final Buton baglanButonu = new Buton("Bağlan", Simge.Tur.KILIT, Tema.KAHVE, Buton.Stil.DOLU);
	private final Buton gosterButonu = new Buton(null, Simge.Tur.GOZ, Tema.SOLUK, Buton.Stil.CERCEVE);
	private final Preferences tercihler = Preferences.userRoot().node("kafe-menu-atolyesi");
	private final BiConsumer<Veritabani, Boolean> basarili;

	/** basarili: bağlantı kurulunca çağrılır (veritabanı, örnek menü yüklendi mi). */
	GirisEkrani(BiConsumer<Veritabani, Boolean> basarili) {
		super("Kafe Menü Atölyesi · MySQL Girişi");
		this.basarili = basarili;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setIconImages(Simge.uygulamaSimgeleri());

		JPanel kok = new JPanel(new BorderLayout());
		kok.setBackground(Tema.KART);
		kok.add(markaPaneli(), BorderLayout.WEST);
		kok.add(formPaneli(), BorderLayout.CENTER);
		setContentPane(kok);

		sunucuAlani.setText(tercihler.get("sunucu", "localhost"));
		portAlani.setText(tercihler.get("port", "3306"));
		veritabaniAlani.setText(tercihler.get("veritabani", "java_kursu"));
		kullaniciAlani.setText(tercihler.get("kullanici", "root"));

		getRootPane().setDefaultButton(baglanButonu);
		baglanButonu.addActionListener(e -> baglan());
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				sifreAlani.requestFocusInWindow();
			}
		});
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
	}

	private JComponent markaPaneli() {
		JPanel p = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Tema.g2(g);
				g2.setPaint(new GradientPaint(0, 0, Tema.ESPRESSO, getWidth(), getHeight(), Tema.KAHVE));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(new Color(255, 255, 255, 14));
				g2.fillOval(-80, getHeight() - 170, 240, 240);
				g2.fillOval(getWidth() - 120, -60, 200, 200);
				g2.dispose();
			}
		};
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(new EmptyBorder(40, 34, 30, 34));
		p.setPreferredSize(new Dimension(320, 470));

		JComponent logo = Simge.logo(86, Tema.KARAMEL, Color.WHITE);
		JLabel baslik = Bilesenler.etiket("Kafe Menü", Font.BOLD, 28f, Tema.KREM_METIN);
		JLabel baslik2 = Bilesenler.etiket("Atölyesi", Font.PLAIN, 28f, new Color(0xE9C9A8));
		JLabel slogan = Bilesenler.paragraf("Menünü tasarla, stoğunu yönet.<br>Java + MySQL ile çalışır.", 240, 14f,
				new Color(255, 240, 225, 200));
		JLabel guvenlik = Bilesenler.paragraf("Şifreniz kodda veya diskte saklanmaz; yalnızca bu oturum boyunca "
				+ "bellekte tutulur.", 240, 12f, new Color(255, 240, 225, 150));
		JLabel kilit = new JLabel(new Simge(Simge.Tur.KILIT, 16, new Color(0xE9C9A8)));

		for (JComponent c : new JComponent[] { logo, baslik, baslik2, slogan, kilit, guvenlik }) {
			c.setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		p.add(logo);
		p.add(Box.createVerticalStrut(22));
		p.add(baslik);
		p.add(baslik2);
		p.add(Box.createVerticalStrut(12));
		p.add(slogan);
		p.add(Box.createVerticalGlue());
		p.add(kilit);
		p.add(Box.createVerticalStrut(6));
		p.add(guvenlik);
		return p;
	}

	private JComponent formPaneli() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(Tema.KART);
		p.setBorder(new EmptyBorder(38, 40, 30, 40));

		JLabel baslik = Bilesenler.etiket("MySQL'e Bağlan", Font.BOLD, 23f, Tema.METIN);
		JLabel alt = Bilesenler.etiket("Menüyü yönetmek için veritabanı bilgilerinizi girin.", Font.PLAIN, 13.5f,
				Tema.SOLUK);

		JPanel sunucuSatiri = new JPanel(new BorderLayout(10, 0));
		sunucuSatiri.setOpaque(false);
		sunucuSatiri.add(Bilesenler.alanBlok("Sunucu", sunucuAlani), BorderLayout.CENTER);
		JPanel portBlok = Bilesenler.alanBlok("Port", portAlani);
		portBlok.setPreferredSize(new Dimension(96, portBlok.getPreferredSize().height));
		sunucuSatiri.add(portBlok, BorderLayout.EAST);

		JPanel ikinciSatir = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
		ikinciSatir.setOpaque(false);
		ikinciSatir.add(Bilesenler.alanBlok("Veritabanı", veritabaniAlani));
		ikinciSatir.add(Bilesenler.alanBlok("Kullanıcı adı", kullaniciAlani));

		gosterButonu.setToolTipText("Şifreyi göster / gizle");
		gosterButonu.setBorder(new EmptyBorder(10, 11, 10, 11));
		gosterButonu.setFocusable(false);
		gosterButonu.addActionListener(e -> {
			boolean gizli = sifreAlani.getEchoChar() != 0;
			sifreAlani.setEchoChar(gizli ? (char) 0 : '•');
			sifreAlani.requestFocusInWindow();
		});
		JPanel sifreSatiri = new JPanel(new BorderLayout(8, 0));
		sifreSatiri.setOpaque(false);
		sifreSatiri.add(sifreAlani, BorderLayout.CENTER);
		sifreSatiri.add(gosterButonu, BorderLayout.EAST);

		Dimension butonBoyutu = new Dimension(Integer.MAX_VALUE, 44);
		baglanButonu.setMaximumSize(butonBoyutu);
		baglanButonu.setFont(Tema.font(Font.BOLD, 14f));
		baglanButonu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		Buton cikis = new Buton("Çıkış", null, Tema.KAHVE, Buton.Stil.CERCEVE);
		cikis.setMaximumSize(butonBoyutu);
		cikis.addActionListener(e -> dispose());

		JComponent[] parcalar = { baslik, alt, sunucuSatiri, ikinciSatir, Bilesenler.alanBlok("Şifre", sifreSatiri),
				hataEtiketi, baglanButonu, cikis };
		int[] bosluklar = { 4, 26, 14, 14, 10, 12, 8, 0 };
		for (int i = 0; i < parcalar.length; i++) {
			parcalar[i].setAlignmentX(Component.LEFT_ALIGNMENT);
			p.add(parcalar[i]);
			if (bosluklar[i] > 0) p.add(Box.createVerticalStrut(bosluklar[i]));
		}
		p.setPreferredSize(new Dimension(450, 470));
		return p;
	}

	private void hata(String mesaj) {
		hataEtiketi.setText(Bilesenler.html(Tema.html(mesaj), 360));
		pack();
	}

	private void kilitle(boolean kilitli) {
		for (JComponent c : new JComponent[] { sunucuAlani, portAlani, veritabaniAlani, kullaniciAlani, sifreAlani,
				baglanButonu, gosterButonu }) {
			c.setEnabled(!kilitli);
		}
		baglanButonu.setText(kilitli ? "Bağlanılıyor…" : "Bağlan");
		setCursor(kilitli ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
	}

	private void baglan() {
		String sunucu = sunucuAlani.getText().trim();
		String veritabani = veritabaniAlani.getText().trim();
		String kullanici = kullaniciAlani.getText().trim();
		if (sunucu.isEmpty() || veritabani.isEmpty() || kullanici.isEmpty()) {
			hata("Sunucu, veritabanı ve kullanıcı adı boş olamaz.");
			return;
		}
		if (!veritabani.matches("[A-Za-z0-9_]+") || !sunucu.matches("[A-Za-z0-9_.\\-]+")) {
			hata("Sunucu ve veritabanı adında yalnızca harf, rakam, nokta, tire ve alt çizgi kullanın.");
			return;
		}
		int port;
		try {
			port = Integer.parseInt(portAlani.getText().trim());
			if (port < 1 || port > 65535) throw new NumberFormatException();
		} catch (NumberFormatException e) {
			hata("Port 1 ile 65535 arasında bir sayı olmalı (MySQL için genelde 3306).");
			return;
		}
		char[] sifre = sifreAlani.getPassword();
		hataEtiketi.setText(" ");
		kilitle(true);

		// Bağlantı birkaç saniye sürebilir; pencere donmasın diye arka planda yapılır.
		new SwingWorker<Boolean, Void>() {
			private Veritabani db;

			@Override
			protected Boolean doInBackground() throws Exception {
				db = new Veritabani(sunucu, port, veritabani, kullanici, new String(sifre));
				db.baglan();
				return db.tablolariHazirla();
			}

			@Override
			protected void done() {
				Arrays.fill(sifre, '\0');
				try {
					boolean ornekYuklendi = get();
					tercihler.put("sunucu", sunucu);
					tercihler.put("port", String.valueOf(port));
					tercihler.put("veritabani", veritabani);
					tercihler.put("kullanici", kullanici);
					dispose();
					basarili.accept(db, ornekYuklendi);
				} catch (ExecutionException ex) {
					if (db != null) db.kapat();
					kilitle(false);
					hata(Veritabani.hataMesaji(ex.getCause()));
					sifreAlani.setText("");
					sifreAlani.requestFocusInWindow();
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}.execute();
	}
}

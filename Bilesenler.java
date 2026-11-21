package KafeMenu;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.text.JTextComponent;

/** Kafe temasına uygun, kendi çizdiğimiz Swing bileşenleri ve küçük yerleşim yardımcıları. */
final class Bilesenler {

	private Bilesenler() {
	}

	/** Hap şeklinde küçük renkli etiket (kategori, alerjen, vegan...). */
	record Rozet(String metin, Color yazi, Color zemin) {
	}

	static final Font ROZET_FONTU = Tema.font(Font.BOLD, 11.5f);

	// =====================================================================
	// Düğme
	// =====================================================================

	static class Buton extends JButton {

		enum Stil {
			/** İçi renkli dolu düğme */
			DOLU,
			/** Beyaz zemin, renkli çerçeve */
			CERCEVE,
			/** Koyu başlık üzerinde yarı saydam düğme */
			SEFFAF
		}

		private final Color renk;
		private final Stil stil;

		Buton(String metin, Simge.Tur simge, Color renk, Stil stil) {
			super(metin);
			this.renk = renk;
			this.stil = stil;
			setUI(new BasicButtonUI() {
				@Override
				protected void paintText(Graphics g, JComponent c, Rectangle r, String t) {
					yaziCiz(g, (AbstractButton) c, r, t, c.isEnabled() ? c.getForeground() : Tema.SOLUK);
				}
			});
			if (simge != null) setIcon(new Simge(simge, 16, null));
			setForeground(stil == Stil.DOLU ? Color.WHITE : stil == Stil.SEFFAF ? Tema.KREM_METIN : renk);
			setFont(Tema.font(Font.BOLD, 13f));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setBorder(new EmptyBorder(9, 16, 9, 18));
			setIconTextGap(8);
			setRolloverEnabled(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			ButtonModel m = getModel();
			RoundRectangle2D sekil = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 12, 12);
			if (!isEnabled()) {
				g2.setColor(stil == Stil.SEFFAF ? new Color(255, 255, 255, 18) : new Color(0xEEE7E0));
				g2.fill(sekil);
			} else if (stil == Stil.DOLU) {
				g2.setColor(m.isPressed() ? Tema.karistir(renk, Color.BLACK, 0.18)
						: m.isRollover() ? Tema.karistir(renk, Color.WHITE, 0.14) : renk);
				g2.fill(sekil);
			} else if (stil == Stil.CERCEVE) {
				g2.setColor(m.isPressed() ? Tema.karistir(renk, Color.WHITE, 0.78)
						: m.isRollover() ? Tema.karistir(renk, Color.WHITE, 0.9) : Tema.KART);
				g2.fill(sekil);
				g2.setColor(Tema.karistir(renk, Color.WHITE, 0.5));
				g2.draw(sekil);
			} else {
				g2.setColor(new Color(255, 255, 255, m.isPressed() ? 60 : m.isRollover() ? 42 : 24));
				g2.fill(sekil);
				g2.setColor(new Color(255, 255, 255, 55));
				g2.draw(sekil);
			}
			if (isFocusOwner()) {
				g2.setColor(stil == Stil.SEFFAF ? Color.WHITE : Tema.karistir(renk, Color.BLACK, 0.25));
				g2.setStroke(new BasicStroke(1.6f));
				g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3f, getHeight() - 3f, 11, 11));
			}
			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static void yaziCiz(Graphics g, AbstractButton b, Rectangle r, String metin, Color renk) {
		Graphics2D g2 = (Graphics2D) g;
		FontMetrics fm = b.getFontMetrics(b.getFont());
		g2.setColor(renk);
		g2.setFont(b.getFont());
		BasicGraphicsUtils.drawStringUnderlineCharAt(b, g2, metin, -1, r.x, r.y + fm.getAscent());
	}

	// =====================================================================
	// Hap: seçilebilir yuvarlak filtre / sekme düğmesi
	// =====================================================================

	static class Hap extends JToggleButton {
		private final Color vurgu;

		Hap(String metin, Color vurgu) {
			super(metin);
			this.vurgu = vurgu;
			setUI(new BasicToggleButtonUI() {
				@Override
				protected void paintText(Graphics g, JComponent c, Rectangle r, String t) {
					AbstractButton b = (AbstractButton) c;
					yaziCiz(g, b, r, t, b.isSelected() ? Color.WHITE : Tema.METIN);
				}
			});
			setFont(Tema.font(Font.BOLD, 12.5f));
			setForeground(Tema.METIN);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setBorder(new EmptyBorder(7, 15, 7, 15));
			setIconTextGap(7);
			setRolloverEnabled(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		Hap(String metin) {
			this(metin, Tema.ESPRESSO);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			float h = getHeight() - 1f;
			RoundRectangle2D sekil = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, h, h, h);
			if (isSelected()) {
				g2.setColor(getModel().isRollover() ? Tema.karistir(vurgu, Color.WHITE, 0.12) : vurgu);
				g2.fill(sekil);
			} else {
				boolean ustunde = getModel().isRollover();
				g2.setColor(ustunde ? Tema.karistir(vurgu, Color.WHITE, 0.9) : Tema.KART);
				g2.fill(sekil);
				g2.setColor(ustunde ? Tema.karistir(vurgu, Color.WHITE, 0.45) : Tema.CIZGI);
				g2.draw(sekil);
			}
			if (isFocusOwner()) {
				g2.setColor(Tema.KARAMEL);
				g2.setStroke(new BasicStroke(1.5f));
				g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3f, h - 2f, h - 2f, h - 2f));
			}
			g2.dispose();
			super.paintComponent(g);
		}
	}

	// =====================================================================
	// Yazı kutuları
	// =====================================================================

	/** Yuvarlak köşeli, isteğe bağlı simgeli ve ipucu yazılı metin kutusu. */
	static class Alan extends JTextField {
		private final String ipucu;
		private final Simge simge;

		Alan(String ipucu, Simge.Tur simgeTuru) {
			this.ipucu = ipucu;
			this.simge = simgeTuru == null ? null : new Simge(simgeTuru, 16, Tema.SOLUK);
			setUI(new BasicTextFieldUI());
			alanAyarla(this, simge != null);
		}

		@Override
		protected void paintComponent(Graphics g) {
			alanZeminiCiz(g, this);
			super.paintComponent(g);
			if (simge != null) simge.paintIcon(this, g, 12, (getHeight() - 16) / 2);
			if (getText().isEmpty()) ipucuCiz(g, this, ipucu);
		}
	}

	/** Şifre kutusu: yazılan karakterler • olarak görünür. */
	static class SifreAlani extends JPasswordField {
		private final String ipucu;
		private final Simge simge = new Simge(Simge.Tur.KILIT, 16, Tema.SOLUK);

		SifreAlani(String ipucu) {
			this.ipucu = ipucu;
			setUI(new BasicPasswordFieldUI());
			alanAyarla(this, true);
			setEchoChar('•');
		}

		@Override
		protected void paintComponent(Graphics g) {
			alanZeminiCiz(g, this);
			super.paintComponent(g);
			simge.paintIcon(this, g, 12, (getHeight() - 16) / 2);
			if (getPassword().length == 0) ipucuCiz(g, this, ipucu);
		}
	}

	private static void alanAyarla(JTextComponent t, boolean simgeli) {
		t.setOpaque(false);
		t.setFont(Tema.font(Font.PLAIN, 13.5f));
		t.setForeground(Tema.METIN);
		t.setCaretColor(Tema.METIN);
		t.setSelectionColor(Tema.KARAMEL_ACIK);
		t.setSelectedTextColor(Tema.METIN);
		t.setDisabledTextColor(Tema.SOLUK);
		t.setBorder(new EmptyBorder(9, simgeli ? 36 : 12, 9, 12));
		t.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				t.repaint();
			}

			@Override
			public void focusLost(FocusEvent e) {
				t.repaint();
			}
		});
	}

	private static void alanZeminiCiz(Graphics g, JComponent c) {
		Graphics2D g2 = Tema.g2(g);
		RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1f, c.getHeight() - 1f, 12, 12);
		g2.setColor(c.isEnabled() ? Tema.KART : Tema.ARKA);
		g2.fill(r);
		boolean odak = c.hasFocus();
		g2.setColor(odak ? Tema.KARAMEL : Tema.CIZGI);
		g2.setStroke(new BasicStroke(odak ? 1.7f : 1f));
		g2.draw(r);
		g2.dispose();
	}

	private static void ipucuCiz(Graphics g, JTextComponent t, String ipucu) {
		if (ipucu == null) return;
		Graphics2D g2 = Tema.g2(g);
		g2.setColor(new Color(0xB2A398));
		g2.setFont(t.getFont());
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(ipucu, t.getInsets().left, (t.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
		g2.dispose();
	}

	/** JSpinner'ı diğer kutularla aynı yükseklik ve yazı tipine getirir. */
	static JSpinner spinner(JSpinner s, String desen) {
		if (desen != null) s.setEditor(new JSpinner.NumberEditor(s, desen));
		s.setFont(Tema.font(Font.PLAIN, 13.5f));
		JFormattedTextField tf = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
		tf.setFont(Tema.font(Font.PLAIN, 13.5f));
		tf.setHorizontalAlignment(SwingConstants.LEFT);
		tf.setColumns(6);
		s.setPreferredSize(new Dimension(Math.max(120, s.getPreferredSize().width), 38));
		return s;
	}

	static <T> JComboBox<T> secimKutusu(JComboBox<T> k) {
		k.setFont(Tema.font(Font.PLAIN, 13.5f));
		k.setPreferredSize(new Dimension(Math.max(140, k.getPreferredSize().width), 38));
		k.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return k;
	}

	// =====================================================================
	// Paneller
	// =====================================================================

	/** Hafif gölgeli, yuvarlak köşeli beyaz kart. */
	static class Kart extends JPanel {
		Kart(LayoutManager yerlesim) {
			super(yerlesim);
			setOpaque(false);
			setBorder(new EmptyBorder(16, 18, 18, 18));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			int w = getWidth(), h = getHeight();
			g2.setColor(new Color(80, 45, 20, 10));
			g2.fill(new RoundRectangle2D.Float(0, 2, w, h - 2, 20, 20));
			g2.setColor(new Color(80, 45, 20, 14));
			g2.fill(new RoundRectangle2D.Float(1, 2, w - 2, h - 3, 18, 18));
			RoundRectangle2D kart = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1.5f, h - 3.5f, 18, 18);
			g2.setColor(Tema.KART);
			g2.fill(kart);
			g2.setColor(Tema.CIZGI);
			g2.draw(kart);
			g2.dispose();
		}
	}

	/** Renkli zeminli küçük bilgi kutusu (uyarı, ipucu...). */
	static class Kutu extends JPanel {
		private Color zemin;

		Kutu(LayoutManager yerlesim, Color zemin) {
			super(yerlesim);
			this.zemin = zemin;
			setOpaque(false);
			setBorder(new EmptyBorder(10, 12, 10, 12));
		}

		void zemin(Color c) {
			zemin = c;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			g2.setColor(zemin);
			g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
			g2.dispose();
		}
	}

	/** Yan yana rozetler gösteren bileşen. */
	static class RozetEtiketi extends JComponent {
		private List<Rozet> rozetler = List.of();

		RozetEtiketi() {
			setAlignmentX(LEFT_ALIGNMENT);
		}

		void ayarla(List<Rozet> yeni) {
			rozetler = yeni;
			revalidate();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			FontMetrics fm = getFontMetrics(ROZET_FONTU);
			int w = 0;
			for (Rozet r : rozetler) w += rozetGenisligi(fm, r.metin()) + 6;
			return new Dimension(Math.max(0, w - 6), 24);
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			int x = 0;
			for (Rozet r : rozetler) x += rozetCiz(g2, r, x, getHeight() / 2) + 6;
			g2.dispose();
		}
	}

	static int rozetGenisligi(FontMetrics fm, String metin) {
		return fm.stringWidth(metin) + 18;
	}

	/** Rozeti çizer ve genişliğini döndürür. */
	static int rozetCiz(Graphics2D g2, Rozet r, int x, int ortaY) {
		g2.setFont(ROZET_FONTU);
		FontMetrics fm = g2.getFontMetrics();
		int w = rozetGenisligi(fm, r.metin()), h = 22;
		g2.setColor(r.zemin());
		g2.fill(new RoundRectangle2D.Float(x, ortaY - h / 2f, w, h, h, h));
		g2.setColor(r.yazi());
		g2.drawString(r.metin(), x + 9, ortaY - fm.getHeight() / 2 + fm.getAscent());
		return w;
	}

	// =====================================================================
	// Yerleşim yardımcıları
	// =====================================================================

	/** Bileşenleri soldan sağa, aralarında boşlukla dizer. */
	static JPanel yatay(int bosluk, Component... parcalar) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setOpaque(false);
		for (int i = 0; i < parcalar.length; i++) {
			if (i > 0) p.add(Box.createHorizontalStrut(bosluk));
			p.add(parcalar[i]);
		}
		return p;
	}

	/** Bileşenleri yukarıdan aşağıya, sola hizalı dizer. */
	static JPanel dikey(int bosluk, JComponent... parcalar) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		for (int i = 0; i < parcalar.length; i++) {
			if (i > 0) p.add(Box.createVerticalStrut(bosluk));
			parcalar[i].setAlignmentX(Component.LEFT_ALIGNMENT);
			p.add(parcalar[i]);
		}
		return p;
	}

	/** Üstte küçük başlık, altında giriş kutusu. */
	static JPanel alanBlok(String etiket, JComponent alan) {
		JPanel p = new JPanel(new BorderLayout(0, 6));
		p.setOpaque(false);
		p.add(etiket(etiket, Font.BOLD, 12.5f, Tema.KAHVE), BorderLayout.NORTH);
		p.add(alan, BorderLayout.CENTER);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	static JLabel etiket(String metin, int stil, float boyut, Color renk) {
		JLabel l = new JLabel(metin);
		l.setFont(Tema.font(stil, boyut));
		l.setForeground(renk);
		return l;
	}

	/** "ÜRÜN İÇERİĞİ" gibi küçük bölüm başlığı. */
	static JLabel bolumBasligi(String metin) {
		JLabel l = new JLabel(metin.toUpperCase(Tema.TR));
		l.setFont(Tema.font(Font.BOLD, 11.5f));
		l.setForeground(Tema.SOLUK);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	/**
	 * Satır kaydıran HTML metni. genislik ekran pikseli cinsindendir.
	 * (Swing'in HTML motoru CSS "px" değerini 4/3 büyüttüğü için burada 3/4 ile çarpıyoruz.)
	 */
	static String html(String icerik, int genislik) {
		return "<html><div style='width:" + Math.round(genislik * 0.75f) + "px'>" + icerik + "</div></html>";
	}

	/** Belirli genişlikte satır kaydıran HTML etiketi. */
	static JLabel paragraf(String html, int genislik, float boyut, Color renk) {
		JLabel l = new JLabel(html(html, genislik));
		l.setFont(Tema.font(Font.PLAIN, boyut));
		l.setForeground(renk);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	static JComponent ayirici() {
		JPanel p = new JPanel();
		p.setBackground(Tema.CIZGI);
		p.setPreferredSize(new Dimension(1, 1));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	static JComponent dikeyAyirici() {
		JPanel p = new JPanel();
		p.setBackground(Tema.CIZGI);
		Dimension d = new Dimension(1, 24);
		p.setPreferredSize(d);
		p.setMaximumSize(d);
		return p;
	}

	static JPanel bosPanel() {
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setBorder(BorderFactory.createEmptyBorder());
		return p;
	}

	// =====================================================================
	// Bildirim (sağ altta birkaç saniye görünen kutu)
	// =====================================================================

	private static JComponent sonBildirim;

	static void bildirim(JFrame pencere, String mesaj, boolean basarili) {
		JLayeredPane katman = pencere.getLayeredPane();
		if (sonBildirim != null) {
			katman.remove(sonBildirim);
			katman.repaint();
		}
		Font font = Tema.font(Font.BOLD, 13.5f);
		FontMetrics fm = katman.getFontMetrics(font);
		int w = Math.min(fm.stringWidth(mesaj) + 76, Math.max(200, katman.getWidth() - 48));
		int h = 52;
		JComponent kutu = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = Tema.g2(g);
				int kh = getHeight() - 6;
				g2.setColor(new Color(0, 0, 0, 40));
				g2.fill(new RoundRectangle2D.Float(2, 5, getWidth() - 4, kh, 16, 16));
				g2.setColor(Tema.ESPRESSO);
				g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 4, kh, 16, 16));
				g2.setColor(basarili ? new Color(0x8BCB8F) : new Color(0xFFB74D));
				g2.fillOval(16, kh / 2 - 12, 24, 24);
				new Simge(basarili ? Simge.Tur.ONAY : Simge.Tur.UYARI, 14, Tema.ESPRESSO).paintIcon(this, g2, 21,
						kh / 2 - 7);
				g2.setColor(Color.WHITE);
				g2.setFont(font);
				g2.drawString(mesaj, 52, kh / 2 - fm.getHeight() / 2 + fm.getAscent());
				g2.dispose();
			}
		};
		kutu.setBounds(katman.getWidth() - w - 24, katman.getHeight() - h - 46, w, h);
		katman.add(kutu, JLayeredPane.POPUP_LAYER);
		katman.repaint();
		sonBildirim = kutu;
		Timer zamanlayici = new Timer(3800, e -> {
			katman.remove(kutu);
			katman.repaint();
			if (sonBildirim == kutu) sonBildirim = null;
		});
		zamanlayici.setRepeats(false);
		zamanlayici.start();
	}
}

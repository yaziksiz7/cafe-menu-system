package KafeMenu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

/**
 * Kodla çizilen basit simgeler. Dışarıdan resim dosyası gerekmez, her boyutta keskin görünür.
 * Simgeler 16x16'lık hayali bir kareye çizilir ve istenen boyuta ölçeklenir.
 */
final class Simge implements Icon {

	enum Tur {
		ARTI, KALEM, COP, ETIKET, KAMYON, TENCERE, FIS, ARA, YENILE, ONAY, KAHVE, KALKAN, UYARI, GOZ, KILIT,
		KUTU, SAAT, CARPI, LISTE, SUNUCU
	}

	private final Tur tur;
	private final int boyut;
	private final Color renk;

	/** renk null ise simge, üzerinde durduğu bileşenin yazı rengini kullanır. */
	Simge(Tur tur, int boyut, Color renk) {
		this.tur = tur;
		this.boyut = boyut;
		this.renk = renk;
	}

	@Override
	public int getIconWidth() {
		return boyut;
	}

	@Override
	public int getIconHeight() {
		return boyut;
	}

	private Color renkSec(Component c) {
		if (renk != null) return renk;
		if (c == null) return Tema.METIN;
		if (!c.isEnabled()) return Tema.SOLUK;
		if (c instanceof JToggleButton t && t.isSelected()) return Color.WHITE;
		return c.getForeground();
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(x, y);
		double olcek = boyut / 16.0;
		g2.scale(olcek, olcek);
		g2.setColor(renkSec(c));
		g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		ciz(g2);
		g2.dispose();
	}

	private static void cizgi(Graphics2D g, double x1, double y1, double x2, double y2) {
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	private static Path2D yol(double... noktalar) {
		Path2D p = new Path2D.Double();
		p.moveTo(noktalar[0], noktalar[1]);
		for (int i = 2; i < noktalar.length; i += 2) p.lineTo(noktalar[i], noktalar[i + 1]);
		return p;
	}

	private static Path2D kapali(double... noktalar) {
		Path2D p = yol(noktalar);
		p.closePath();
		return p;
	}

	private void ciz(Graphics2D g) {
		switch (tur) {
			case ARTI -> {
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				cizgi(g, 8, 3, 8, 13);
				cizgi(g, 3, 8, 13, 8);
			}
			case KALEM -> {
				g.draw(kapali(11, 2.5, 13.5, 5, 5.5, 13, 2.5, 13.5, 3, 10.5));
				cizgi(g, 9.3, 4.2, 11.8, 6.7);
			}
			case COP -> {
				cizgi(g, 2.5, 4.5, 13.5, 4.5);
				g.draw(yol(6, 4.5, 6, 2.5, 10, 2.5, 10, 4.5));
				g.draw(yol(4, 4.5, 4.8, 14, 11.2, 14, 12, 4.5));
				cizgi(g, 6.8, 7, 6.8, 11.5);
				cizgi(g, 9.2, 7, 9.2, 11.5);
			}
			case ETIKET -> {
				g.draw(kapali(8.5, 1.8, 14.2, 1.8, 14.2, 7.5, 7.5, 14.2, 1.8, 8.5));
				g.fill(new Ellipse2D.Double(10.2, 4.1, 2, 2));
			}
			case KAMYON -> {
				g.draw(new RoundRectangle2D.Double(1, 3.5, 9, 8, 1.5, 1.5));
				g.draw(yol(10, 6, 12.8, 6, 15, 8.8, 15, 11.5, 10, 11.5));
				g.fill(new Ellipse2D.Double(2.6, 10.6, 3.6, 3.6));
				g.fill(new Ellipse2D.Double(10.4, 10.6, 3.6, 3.6));
			}
			case TENCERE -> {
				g.draw(new RoundRectangle2D.Double(2.5, 6.5, 11, 7, 3, 3));
				cizgi(g, 1, 6.5, 15, 6.5);
				cizgi(g, 6.5, 4.5, 9.5, 4.5);
				cizgi(g, 5, 1.2, 5, 2.6);
				cizgi(g, 11, 1.2, 11, 2.6);
			}
			case FIS -> {
				g.draw(kapali(3, 1.5, 13, 1.5, 13, 14.5, 11.3, 13.3, 9.6, 14.5, 8, 13.3, 6.4, 14.5, 4.7, 13.3, 3,
						14.5));
				cizgi(g, 5.5, 4.8, 10.5, 4.8);
				cizgi(g, 5.5, 7.6, 10.5, 7.6);
				cizgi(g, 5.5, 10.4, 8.5, 10.4);
			}
			case ARA -> {
				g.draw(new Ellipse2D.Double(2, 2, 9, 9));
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				cizgi(g, 10, 10, 14, 14);
			}
			case YENILE -> {
				g.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 70, 290, Arc2D.OPEN));
				g.draw(yol(7.2, 0.9, 9.9, 2.9, 7.4, 5.2));
			}
			case ONAY -> {
				g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(yol(3, 8.5, 6.5, 12, 13, 4.5));
			}
			case KAHVE -> {
				Path2D fincan = new Path2D.Double();
				fincan.moveTo(2.5, 6);
				fincan.lineTo(11.5, 6);
				fincan.lineTo(11, 11);
				fincan.quadTo(10.6, 13, 8.5, 13);
				fincan.lineTo(5.5, 13);
				fincan.quadTo(3.4, 13, 3, 11);
				fincan.closePath();
				g.draw(fincan);
				g.draw(new Arc2D.Double(9.6, 6.8, 4.4, 4.4, -90, 180, Arc2D.OPEN));
				cizgi(g, 1.5, 15, 12.5, 15);
				Path2D buhar = new Path2D.Double();
				buhar.moveTo(5.5, 4.2);
				buhar.quadTo(4.6, 3.1, 5.5, 2.1);
				buhar.quadTo(6.3, 1.2, 5.6, 0.4);
				buhar.moveTo(8.5, 4.2);
				buhar.quadTo(7.6, 3.1, 8.5, 2.1);
				buhar.quadTo(9.3, 1.2, 8.6, 0.4);
				g.draw(buhar);
			}
			case KALKAN -> {
				Path2D k = new Path2D.Double();
				k.moveTo(8, 1.5);
				k.lineTo(13.5, 3.5);
				k.lineTo(13.5, 8);
				k.quadTo(13.5, 12.5, 8, 14.5);
				k.quadTo(2.5, 12.5, 2.5, 8);
				k.lineTo(2.5, 3.5);
				k.closePath();
				g.draw(k);
				g.draw(yol(5.5, 8, 7.3, 9.9, 10.8, 6));
			}
			case UYARI -> {
				g.draw(kapali(8, 1.8, 14.6, 13.6, 1.4, 13.6));
				g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				cizgi(g, 8, 6.3, 8, 9.3);
				g.fill(new Ellipse2D.Double(7, 10.9, 2, 2));
			}
			case GOZ -> {
				Path2D goz = new Path2D.Double();
				goz.moveTo(1.2, 8);
				goz.quadTo(8, 0.8, 14.8, 8);
				goz.quadTo(8, 15.2, 1.2, 8);
				g.draw(goz);
				g.fill(new Ellipse2D.Double(5.7, 5.7, 4.6, 4.6));
			}
			case KILIT -> {
				g.draw(new RoundRectangle2D.Double(3, 7, 10, 7.5, 2.5, 2.5));
				g.draw(new Arc2D.Double(5, 2, 6, 7, 0, 180, Arc2D.OPEN));
				cizgi(g, 5, 5.5, 5, 7);
				cizgi(g, 11, 5.5, 11, 7);
				g.fill(new Ellipse2D.Double(7, 9.6, 2, 2));
			}
			case KUTU -> {
				g.draw(kapali(8, 1.5, 14, 4.5, 14, 11.5, 8, 14.5, 2, 11.5, 2, 4.5));
				g.draw(yol(2, 4.5, 8, 7.5, 14, 4.5));
				cizgi(g, 8, 7.5, 8, 14.5);
			}
			case SAAT -> {
				g.draw(new Ellipse2D.Double(1.8, 1.8, 12.4, 12.4));
				g.draw(yol(8, 4.6, 8, 8, 10.6, 9.6));
			}
			case CARPI -> {
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				cizgi(g, 4, 4, 12, 12);
				cizgi(g, 12, 4, 4, 12);
			}
			case LISTE -> {
				for (double y : new double[] { 4, 8, 12 }) {
					g.fill(new Ellipse2D.Double(1.6, y - 1.1, 2.2, 2.2));
					cizgi(g, 6, y, 14.2, y);
				}
			}
			case SUNUCU -> {
				g.draw(new RoundRectangle2D.Double(2, 2, 12, 5, 2, 2));
				g.draw(new RoundRectangle2D.Double(2, 9, 12, 5, 2, 2));
				g.fill(new Ellipse2D.Double(4, 3.8, 1.6, 1.6));
				g.fill(new Ellipse2D.Double(4, 10.8, 1.6, 1.6));
			}
		}
	}

	// ---------------- Logo ----------------

	/** Karamel daire içinde beyaz kahve fincanı. */
	static void logoCiz(Graphics2D g, int x, int y, int boyut, Color daire, Color fincan) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(daire);
		g2.fill(new Ellipse2D.Double(x, y, boyut, boyut));
		int ic = (int) Math.round(boyut * 0.58);
		new Simge(Tur.KAHVE, ic, fincan).paintIcon(null, g2, x + (boyut - ic) / 2, y + (boyut - ic) / 2);
		g2.dispose();
	}

	/** Ekranlarda kullanılan logo bileşeni. */
	static JComponent logo(int boyut, Color daire, Color fincan) {
		JComponent c = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				logoCiz((Graphics2D) g, 0, 0, boyut, daire, fincan);
			}
		};
		Dimension d = new Dimension(boyut, boyut);
		c.setPreferredSize(d);
		c.setMinimumSize(d);
		c.setMaximumSize(d);
		return c;
	}

	/** Pencere ve görev çubuğu simgeleri. */
	static List<Image> uygulamaSimgeleri() {
		List<Image> liste = new ArrayList<>();
		for (int s : new int[] { 16, 24, 32, 48, 64, 128 }) {
			BufferedImage resim = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = resim.createGraphics();
			logoCiz(g, 0, 0, s, Tema.KARAMEL, Color.WHITE);
			g.dispose();
			liste.add(resim);
		}
		return liste;
	}
}

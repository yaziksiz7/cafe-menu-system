package KafeMenu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import KafeMenu.Bilesenler.Rozet;

/** Tabloların görünümü: başlık, satır renkleri, rozetli ve stok çubuklu hücreler. */
final class Tablolar {

	private Tablolar() {
	}

	/** Bir tabloyu kafe temasına göre biçimlendirir. */
	static void bicimle(JTable t, int satirYuksekligi) {
		t.setRowHeight(satirYuksekligi);
		t.setShowVerticalLines(false);
		t.setShowHorizontalLines(true);
		t.setGridColor(new Color(0xF1E9E1));
		t.setIntercellSpacing(new Dimension(0, 1));
		t.setSelectionBackground(Tema.KARAMEL_ACIK);
		t.setSelectionForeground(Tema.METIN);
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.setFillsViewportHeight(true);
		t.setBackground(Tema.KART);
		t.setFont(Tema.font(Font.PLAIN, 13f));
		t.getTableHeader().setDefaultRenderer(new BaslikRenderer());
		t.getTableHeader().setReorderingAllowed(false);
		t.setDefaultRenderer(Object.class, new MetinRenderer());
	}

	/** Sütunun genişliğini ve hücre çizicisini ayarlar. */
	static void sutun(JTable t, int indeks, int genislik, TableCellRenderer cizici) {
		TableColumn c = t.getColumnModel().getColumn(indeks);
		c.setPreferredWidth(genislik);
		c.setCellRenderer(cizici);
	}

	/** Pencere daralınca bu sütun en az enGenislik kadar kalır (diğer sütunlar daralır). */
	static void sutun(JTable t, int indeks, int genislik, int enGenislik, TableCellRenderer cizici) {
		sutun(t, indeks, genislik, cizici);
		t.getColumnModel().getColumn(indeks).setMinWidth(enGenislik);
	}

	/** Vegan / Vejetaryen sütunu: 2 = vegan, 1 = vejetaryen, 0 = ikisi de değil. */
	static List<Rozet> beslenmeRozeti(Object deger) {
		int d = (Integer) deger;
		if (d == 2) return List.of(new Rozet("Vegan", Tema.YESIL, Tema.YESIL_ACIK));
		if (d == 1) return List.of(new Rozet("Vejetaryen", Tema.YAPRAK, Tema.YAPRAK_ACIK));
		return List.of();
	}

	static JScrollPane kaydirma(JTable t) {
		JScrollPane sp = new JScrollPane(t);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.setViewportBorder(null);
		sp.getViewport().setBackground(Tema.KART);
		sp.setOpaque(false);
		sp.getVerticalScrollBar().setUnitIncrement(16);
		return sp;
	}

	/** Zebra desenli satır zemini. */
	static Color zemin(int satir, boolean secili) {
		return secili ? Tema.KARAMEL_ACIK : (satir % 2 == 0 ? Tema.KART : Tema.KART_ALT);
	}

	// ---------------- Hazır rozet listeleri ----------------

	static List<Rozet> kategoriRozeti(Object deger) {
		String k = (String) deger;
		Color[] r = Tema.kategoriRenk(k);
		return List.of(new Rozet(k, r[0], r[1]));
	}

	static List<Rozet> alerjenRozetleri(Object deger) {
		String s = (String) deger;
		List<Rozet> liste = new ArrayList<>();
		if (s == null) return liste;
		for (String a : s.split(",")) {
			if (!a.isBlank()) liste.add(new Rozet(a.trim(), Tema.KIRMIZI, Tema.KIRMIZI_ACIK));
		}
		return liste;
	}

	static Function<Object, List<Rozet>> evetRozeti(String metin, Color yazi, Color zemin) {
		return deger -> Boolean.TRUE.equals(deger) ? List.of(new Rozet(metin, yazi, zemin)) : List.of();
	}

	static List<Rozet> durumRozeti(Object deger) {
		StokDurumu d = (StokDurumu) deger;
		return List.of(new Rozet(d.etiket, d.renk, d.acikRenk));
	}

	static List<Rozet> islemRozeti(Object deger) {
		String s = (String) deger;
		Color[] r = Tema.islemRenk(s);
		return List.of(new Rozet(s, r[0], r[1]));
	}

	// =====================================================================
	// Başlık
	// =====================================================================

	static final class BaslikRenderer extends JLabel implements TableCellRenderer {
		BaslikRenderer() {
			setOpaque(true);
			setBackground(new Color(0xF6EEE5));
			setForeground(Tema.KAHVE);
			setFont(Tema.font(Font.BOLD, 12.5f));
			setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.CIZGI), new EmptyBorder(0, 14, 0, 8)));
			setHorizontalTextPosition(SwingConstants.LEADING);
			setIconTextGap(6);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object deger, boolean secili, boolean odak,
				int satir, int sutun) {
			setText(deger == null ? "" : deger.toString());
			setIcon(null);
			if (t != null && t.getRowSorter() != null) {
				List<? extends RowSorter.SortKey> anahtarlar = t.getRowSorter().getSortKeys();
				if (!anahtarlar.isEmpty() && anahtarlar.get(0).getColumn() == t.convertColumnIndexToModel(sutun)
						&& anahtarlar.get(0).getSortOrder() != SortOrder.UNSORTED) {
					setIcon(new OkSimgesi(anahtarlar.get(0).getSortOrder() == SortOrder.ASCENDING));
				}
			}
			return this;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			d.height = 40;
			return d;
		}
	}

	/** Sıralama yönünü gösteren küçük üçgen. */
	private record OkSimgesi(boolean artan) implements Icon {
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = Tema.g2(g);
			Path2D p = new Path2D.Double();
			if (artan) {
				p.moveTo(x, y + 6);
				p.lineTo(x + 8, y + 6);
				p.lineTo(x + 4, y + 1);
			} else {
				p.moveTo(x, y + 2);
				p.lineTo(x + 8, y + 2);
				p.lineTo(x + 4, y + 7);
			}
			p.closePath();
			g2.setColor(Tema.KARAMEL);
			g2.fill(p);
			g2.dispose();
		}

		@Override
		public int getIconWidth() {
			return 8;
		}

		@Override
		public int getIconHeight() {
			return 8;
		}
	}

	// =====================================================================
	// Düz metin hücresi
	// =====================================================================

	static final class MetinRenderer extends DefaultTableCellRenderer {
		private final int stil;
		private final Color renk;
		private final Function<Object, String> bicim;

		MetinRenderer(int stil, Color renk, Function<Object, String> bicim) {
			this.stil = stil;
			this.renk = renk;
			this.bicim = bicim;
		}

		MetinRenderer() {
			this(Font.PLAIN, Tema.METIN, null);
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object deger, boolean secili, boolean odak,
				int satir, int sutun) {
			super.getTableCellRendererComponent(t, deger, secili, false, satir, sutun);
			String metin = deger == null ? "" : bicim != null ? bicim.apply(deger) : deger.toString();
			setText(metin);
			setToolTipText(metin.length() > 28 ? metin : null);
			setFont(Tema.font(stil, 13f));
			setForeground(renk);
			setBackground(zemin(satir, secili));
			setHorizontalAlignment(SwingConstants.LEADING);
			setBorder(new EmptyBorder(0, 14, 0, 12));
			return this;
		}
	}

	// =====================================================================
	// Rozetli hücre
	// =====================================================================

	static final class RozetRenderer extends JComponent implements TableCellRenderer {
		private final Function<Object, List<Rozet>> donustur;
		private List<Rozet> rozetler = List.of();
		private Color zemin = Tema.KART;

		RozetRenderer(Function<Object, List<Rozet>> donustur) {
			this.donustur = donustur;
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object deger, boolean secili, boolean odak,
				int satir, int sutun) {
			rozetler = deger == null ? List.of() : donustur.apply(deger);
			zemin = zemin(satir, secili);
			if (rozetler.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for (Rozet r : rozetler) sb.append(sb.length() == 0 ? "" : ", ").append(r.metin());
				setToolTipText(sb.toString());
			} else {
				setToolTipText(null);
			}
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			g2.setColor(zemin);
			g2.fillRect(0, 0, getWidth(), getHeight());
			int x = 12, orta = getHeight() / 2;
			if (rozetler.isEmpty()) {
				g2.setColor(new Color(0xC2B4A8));
				g2.setFont(Tema.font(Font.PLAIN, 13f));
				FontMetrics fm = g2.getFontMetrics();
				g2.drawString("—", x + 2, orta - fm.getHeight() / 2 + fm.getAscent());
			}
			FontMetrics fm = g2.getFontMetrics(Bilesenler.ROZET_FONTU);
			for (int i = 0; i < rozetler.size(); i++) {
				Rozet r = rozetler.get(i);
				int kalan = rozetler.size() - i;
				int gerekli = Bilesenler.rozetGenisligi(fm, r.metin()) + (kalan > 1 ? 40 : 0);
				if (i > 0 && x + gerekli > getWidth() - 6) {
					Bilesenler.rozetCiz(g2, new Rozet("+" + kalan, Tema.SOLUK, new Color(0xF1EAE3)), x, orta);
					break;
				}
				x += Bilesenler.rozetCiz(g2, r, x, orta) + 6;
			}
			g2.dispose();
		}
	}

	// =====================================================================
	// Stok hücresi: sayı + renkli doluluk çubuğu
	// =====================================================================

	/** Stok sütununun değeri. Sıralama stok miktarına göre yapılır. */
	record StokGorunumu(double deger, double min, String birim) implements Comparable<StokGorunumu> {
		StokDurumu durum() {
			return StokDurumu.hesapla(deger, min);
		}

		@Override
		public int compareTo(StokGorunumu o) {
			return Double.compare(deger, o.deger);
		}
	}

	static final class StokRenderer extends JComponent implements TableCellRenderer {
		private final boolean durumYaz;
		private StokGorunumu stok;
		private Color zemin = Tema.KART;

		StokRenderer(boolean durumYaz) {
			this.durumYaz = durumYaz;
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object deger, boolean secili, boolean odak,
				int satir, int sutun) {
			stok = (StokGorunumu) deger;
			zemin = zemin(satir, secili);
			setToolTipText(stok == null ? null
					: "Kritik seviye: " + Tema.miktar(stok.min()) + " " + stok.birim() + " · " + stok.durum().etiket);
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = Tema.g2(g);
			int w = getWidth(), h = getHeight();
			g2.setColor(zemin);
			g2.fillRect(0, 0, w, h);
			if (stok == null) {
				g2.dispose();
				return;
			}
			StokDurumu d = stok.durum();
			int taban = h / 2 + 1;
			String deger = Tema.miktar(stok.deger());
			g2.setFont(Tema.font(Font.BOLD, 13f));
			g2.setColor(d.dusukMu() ? d.renk : Tema.METIN);
			g2.drawString(deger, 14, taban);
			int x = 14 + g2.getFontMetrics().stringWidth(deger);
			g2.setFont(Tema.font(Font.PLAIN, 12f));
			g2.setColor(Tema.SOLUK);
			g2.drawString(" " + stok.birim(), x, taban);
			if (durumYaz) {
				g2.setFont(Tema.font(Font.BOLD, 11f));
				g2.setColor(d.renk);
				g2.drawString(d.etiket, w - 12 - g2.getFontMetrics().stringWidth(d.etiket), taban);
			}
			int cx = 14, cy = taban + 6, cw = Math.max(10, w - 26);
			g2.setColor(new Color(0xEFE6DD));
			g2.fillRoundRect(cx, cy, cw, 5, 5, 5);
			double oran = stok.min() <= 0 ? (stok.deger() > 0 ? 1 : 0)
					: Math.min(1, stok.deger() / (stok.min() * 3));
			if (oran > 0) {
				g2.setColor(d.renk);
				g2.fillRoundRect(cx, cy, (int) Math.max(5, cw * oran), 5, 5, 5);
			}
			g2.dispose();
		}
	}

	// =====================================================================
	// Boş tabloda bilgi mesajı gösteren JTable
	// =====================================================================

	static class BosDurumTablo extends JTable {
		private String bosMesaj = "Gösterilecek kayıt yok";

		BosDurumTablo(TableModel model) {
			super(model);
		}

		void bosMesaj(String mesaj) {
			bosMesaj = mesaj;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getRowCount() > 0) return;
			Graphics2D g2 = Tema.g2(g);
			int orta = getParent() != null ? getParent().getWidth() / 2 : getWidth() / 2;
			new Simge(Simge.Tur.ARA, 34, new Color(0xCDBFB2)).paintIcon(this, g2, orta - 17, 46);
			g2.setFont(Tema.font(Font.BOLD, 14.5f));
			g2.setColor(Tema.SOLUK);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(bosMesaj, orta - fm.stringWidth(bosMesaj) / 2, 110);
			g2.dispose();
		}
	}

	// =====================================================================
	// Sayı düzenleyici: "18,5" ve "18.5" yazımlarını kabul eder
	// =====================================================================

	static final class SayiEditor extends DefaultCellEditor {
		private final JTextField kutu;

		SayiEditor() {
			super(new JTextField());
			kutu = (JTextField) getComponent();
			kutu.setFont(Tema.font(Font.PLAIN, 13f));
			kutu.setBorder(new LineBorder(Tema.KARAMEL, 2));
			setClickCountToStart(1);
		}

		@Override
		public Component getTableCellEditorComponent(JTable t, Object deger, boolean secili, int satir, int sutun) {
			super.getTableCellEditorComponent(t, deger, secili, satir, sutun);
			kutu.setBorder(new LineBorder(Tema.KARAMEL, 2));
			kutu.setText(deger instanceof Double d ? Tema.miktar(d).replace(".", "") : "");
			javax.swing.SwingUtilities.invokeLater(kutu::selectAll);
			return kutu;
		}

		@Override
		public Object getCellEditorValue() {
			try {
				return Tema.sayiCoz(kutu.getText());
			} catch (NumberFormatException e) {
				return null;
			}
		}

		@Override
		public boolean stopCellEditing() {
			Object deger = getCellEditorValue();
			if (deger == null || (Double) deger < 0) {
				kutu.setBorder(new LineBorder(Tema.KIRMIZI, 2));
				return false;
			}
			return super.stopCellEditing();
		}
	}
}

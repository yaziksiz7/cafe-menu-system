package KafeMenu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Uygulamanın renkleri, yazı tipi ve sayı/para biçimlendirme yardımcıları.
 * Renkleri değiştirmek isterseniz yalnızca bu dosyayı düzenlemeniz yeterli.
 */
final class Tema {

	private Tema() {
	}

	// ---- Kahve tonları ----
	static final Color ESPRESSO = new Color(0x3B2A20);
	static final Color KAHVE = new Color(0x6B4A36);
	static final Color KARAMEL = new Color(0xC07A45);
	static final Color KARAMEL_KOYU = new Color(0x9C5A2B);
	static final Color KARAMEL_ACIK = new Color(0xF6E8DA);

	// ---- Zemin ve yazı ----
	static final Color ARKA = new Color(0xF6F1EB);
	static final Color KART = Color.WHITE;
	static final Color KART_ALT = new Color(0xFCFAF7);
	static final Color CIZGI = new Color(0xEADFD3);
	static final Color METIN = new Color(0x2D211B);
	static final Color SOLUK = new Color(0x8C7B6F);
	static final Color KREM_METIN = new Color(0xFFF5E9);

	// ---- Durum renkleri (koyu yazı rengi + açık zemin rengi) ----
	static final Color YESIL = new Color(0x2E7D32), YESIL_ACIK = new Color(0xE4F3E6);
	static final Color YAPRAK = new Color(0x5D8C2A), YAPRAK_ACIK = new Color(0xEEF5E2);
	static final Color KIRMIZI = new Color(0xC62828), KIRMIZI_ACIK = new Color(0xFDE8E6);
	static final Color TURUNCU = new Color(0xC8641A), TURUNCU_ACIK = new Color(0xFFF0DE);
	static final Color MAVI = new Color(0x1F6FA6), MAVI_ACIK = new Color(0xE3EFF8);
	static final Color MOR = new Color(0x7E3F9E), MOR_ACIK = new Color(0xF2E7F7);

	static final Locale TR = Locale.forLanguageTag("tr-TR");
	private static final DecimalFormatSymbols TR_SEMBOLLERI = DecimalFormatSymbols.getInstance(TR);

	/** Windows'ta Segoe UI, yoksa sırayla diğerleri. */
	static final String YAZI_TIPI = yaziTipiSec();

	private static String yaziTipiSec() {
		List<String> mevcut = Arrays.asList(
				GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		for (String aday : new String[] { "Segoe UI", "Inter", "Helvetica Neue", "Arial" }) {
			if (mevcut.contains(aday)) return aday;
		}
		return Font.DIALOG;
	}

	static Font font(int stil, float boyut) {
		return new Font(YAZI_TIPI, stil, 12).deriveFont(boyut);
	}

	/** Nimbus görünümünü kahve tonlarına boyar. Pencereler açılmadan önce bir kez çağrılır. */
	static void uygula() {
		UIManager.put("control", ARKA);
		UIManager.put("nimbusBase", new Color(0xA88566));
		UIManager.put("nimbusBlueGrey", new Color(0xD9C9B9));
		UIManager.put("nimbusFocus", KARAMEL);
		UIManager.put("nimbusSelectionBackground", KARAMEL);
		UIManager.put("nimbusSelection", KARAMEL);
		UIManager.put("nimbusLightBackground", Color.WHITE);
		UIManager.put("text", METIN);
		UIManager.put("defaultFont", font(Font.PLAIN, 13f));
		try {
			for (UIManager.LookAndFeelInfo bilgi : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(bilgi.getName())) {
					UIManager.setLookAndFeel(bilgi.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// Nimbus yoksa Java'nın varsayılan görünümü kullanılır; uygulama yine çalışır.
		}
	}

	/** Yumuşak kenarlı çizim için hazırlanmış bir Graphics2D kopyası döndürür. */
	static Graphics2D g2(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		Object masaustuIpuclari = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
		if (masaustuIpuclari instanceof Map<?, ?> ipuclari) {
			g2.addRenderingHints(ipuclari);
		} else {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		return g2;
	}

	/** İki rengi karıştırır. oran=0 -> a, oran=1 -> b */
	static Color karistir(Color a, Color b, double oran) {
		double t = Math.max(0, Math.min(1, oran));
		return new Color(
				(int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
				(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	/** Kategori için {yazı rengi, zemin rengi} */
	static Color[] kategoriRenk(String kategori) {
		return switch (kategori == null ? "" : kategori) {
			case "Yemek" -> new Color[] { TURUNCU, TURUNCU_ACIK };
			case "İçecek" -> new Color[] { MAVI, MAVI_ACIK };
			case "Tatlı" -> new Color[] { MOR, MOR_ACIK };
			default -> new Color[] { SOLUK, CIZGI };
		};
	}

	/** İşlem türü için {yazı rengi, zemin rengi} */
	static Color[] islemRenk(String tur) {
		return switch (tur == null ? "" : tur) {
			case "Ekleme" -> new Color[] { YESIL, YESIL_ACIK };
			case "Silme" -> new Color[] { KIRMIZI, KIRMIZI_ACIK };
			case "Fiyat" -> new Color[] { MOR, MOR_ACIK };
			case "Sipariş" -> new Color[] { KARAMEL_KOYU, KARAMEL_ACIK };
			case "Hazırlama" -> new Color[] { TURUNCU, TURUNCU_ACIK };
			case "Satış" -> new Color[] { YAPRAK, YAPRAK_ACIK };
			default -> new Color[] { MAVI, MAVI_ACIK };
		};
	}

	// ---------------- Sayı ve para ----------------

	/** 1250.5 -> "₺1.250,50" */
	static String para(BigDecimal tutar) {
		return "₺" + new DecimalFormat("#,##0.00", TR_SEMBOLLERI).format(tutar);
	}

	/** 1250.5 -> "1250,50" (düzenleme kutuları için, binlik ayırıcı olmadan) */
	static String paraSade(BigDecimal tutar) {
		return new DecimalFormat("0.00", TR_SEMBOLLERI).format(tutar);
	}

	/** 2000 -> "2.000", 18.5 -> "18,5" */
	static String miktar(double deger) {
		return new DecimalFormat("#,##0.##", TR_SEMBOLLERI).format(deger);
	}

	/**
	 * Kullanıcının yazdığı sayıyı okur. "18,5", "18.5", "1.250,75" ve "2.000" biçimlerini anlar.
	 * Sayı değilse NumberFormatException fırlatır.
	 */
	static double sayiCoz(String metin) {
		String s = metin == null ? "" : metin.replace("₺", "").replace(" ", "").trim();
		if (s.isEmpty()) throw new NumberFormatException("boş");
		if (s.contains(",")) {
			// Türkçe yazım: nokta binlik ayırıcı, virgül ondalık -> "1.250,75"
			s = s.replace(".", "").replace(",", ".");
		} else {
			long noktaSayisi = s.chars().filter(c -> c == '.').count();
			int nokta = s.lastIndexOf('.');
			boolean binlikGibi = noktaSayisi == 1 && s.length() - nokta - 1 == 3 && !s.startsWith("0");
			// "2.000" veya "1.250.000" binlik ayırıcıdır; "18.5" ise ondalıktır
			if (noktaSayisi > 1 || binlikGibi) s = s.replace(".", "");
		}
		double deger = Double.parseDouble(s);
		if (Double.isNaN(deger) || Double.isInfinite(deger)) throw new NumberFormatException(metin);
		return deger;
	}

	/** Para tutarını okur ve kuruşa (2 basamağa) yuvarlar. */
	static BigDecimal paraCoz(String metin) {
		return BigDecimal.valueOf(sayiCoz(metin)).setScale(2, RoundingMode.HALF_UP);
	}

	static String kucuk(String s) {
		return s == null ? "" : s.toLowerCase(TR);
	}

	/** HTML etiketlerinde güvenle göstermek için özel karakterleri kaçırır. */
	static String html(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	static String renkKodu(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}
}

package KafeMenu;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * MySQL ile konuşan TEK sınıf. Bütün SQL sorguları burada; ekranlar sadece bu sınıfın metotlarını çağırır.
 *
 * Tablolar (java_kursu veritabanında, ilk açılışta otomatik oluşturulur):
 *   kafe_menu_listesi      -> menüdeki ürünler (yemek, içecek, tatlı)
 *   kafe_malzemeler        -> mutfak malzemeleri ve stokları
 *   kafe_urun_malzemeleri  -> hangi üründe hangi malzemeden ne kadar var (ürün içeriği)
 *   kafe_islem_gecmisi     -> ekleme, silme, fiyat, sipariş, hazırlama, satış kayıtları
 *   kafe_ayarlar           -> kafe adı gibi ayarlar
 *
 * Kullanıcının girdiği her değer SQL'e PreparedStatement'taki '?' işaretleriyle gönderilir (SQL Injection'a karşı).
 * Birden fazla tabloyu değiştiren işlemler "transaction" içinde yapılır: ya hepsi kaydedilir ya hiçbiri.
 */
public class Veritabani {

	/** Malzemelerden hesaplanan ürün özeti. */
	public record Ozet(String icerik, String alerjenler, boolean vegan, boolean vejetaryen) {
	}

	private final String url;
	private final String kullanici;
	private final String sifre;
	private final String baglantiBilgisi;
	private Connection baglanti;

	/** Şifre yalnızca bu nesnenin içinde, program açık olduğu sürece bellekte tutulur; diske yazılmaz. */
	public Veritabani(String sunucu, int port, String veritabani, String kullanici, String sifre) {
		this.url = "jdbc:mysql://" + sunucu + ":" + port + "/" + veritabani
				+ "?characterEncoding=UTF-8&connectTimeout=5000";
		this.kullanici = kullanici;
		this.sifre = sifre;
		this.baglantiBilgisi = kullanici + "@" + sunucu + ":" + port + "/" + veritabani;
	}

	public String baglantiBilgisi() {
		return baglantiBilgisi;
	}

	public void baglan() throws SQLException {
		DriverManager.setLoginTimeout(5);
		baglanti = DriverManager.getConnection(url, kullanici, sifre);
	}

	/** Açık bağlantıyı verir; kopmuşsa (ör. uzun süre beklenince) yeniden bağlanır. */
	private Connection bag() throws SQLException {
		if (baglanti == null || !baglanti.isValid(2)) baglan();
		return baglanti;
	}

	public void kapat() {
		try {
			if (baglanti != null) baglanti.close();
		} catch (SQLException e) {
			// kapanırken oluşan hata önemsiz
		}
	}

	// =====================================================================
	// Tabloları oluşturma
	// =====================================================================

	private static final String[] TABLOLAR = {
			"""
			CREATE TABLE IF NOT EXISTS kafe_ayarlar (
			  anahtar VARCHAR(50)  PRIMARY KEY,
			  deger   VARCHAR(255) NOT NULL
			) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_turkish_ci
			""",
			"""
			CREATE TABLE IF NOT EXISTS kafe_malzemeler (
			  id          INT AUTO_INCREMENT PRIMARY KEY,
			  malzeme_adi VARCHAR(100)  NOT NULL UNIQUE,
			  birim       VARCHAR(20)   NOT NULL,
			  stok        DECIMAL(12,2) NOT NULL DEFAULT 0,
			  min_stok    DECIMAL(12,2) NOT NULL DEFAULT 0,
			  alerjen     VARCHAR(150)  NULL,
			  vegan       BOOLEAN       NOT NULL DEFAULT TRUE,
			  vejetaryen  BOOLEAN       NOT NULL DEFAULT TRUE,
			  CHECK (stok >= 0),
			  CHECK (min_stok >= 0)
			) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_turkish_ci
			""",
			"""
			CREATE TABLE IF NOT EXISTS kafe_menu_listesi (
			  id           INT AUTO_INCREMENT PRIMARY KEY,
			  urun_adi     VARCHAR(100)  NOT NULL UNIQUE,
			  kategori     ENUM('Yemek', 'İçecek', 'Tatlı') NOT NULL,
			  fiyat        DECIMAL(10,2) NOT NULL,
			  urun_icerigi VARCHAR(1000) NOT NULL DEFAULT '',
			  alerjenler   VARCHAR(255)  NOT NULL DEFAULT '',
			  vegan        BOOLEAN       NOT NULL DEFAULT FALSE,
			  vejetaryen   BOOLEAN       NOT NULL DEFAULT FALSE,
			  stok         INT           NOT NULL DEFAULT 0,
			  min_stok     INT           NOT NULL DEFAULT 5,
			  CHECK (fiyat > 0),
			  CHECK (stok >= 0),
			  CHECK (min_stok >= 0)
			) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_turkish_ci
			""",
			"""
			CREATE TABLE IF NOT EXISTS kafe_urun_malzemeleri (
			  urun_id    INT           NOT NULL,
			  malzeme_id INT           NOT NULL,
			  miktar     DECIMAL(10,3) NOT NULL,
			  sira       INT           NOT NULL DEFAULT 0,
			  PRIMARY KEY (urun_id, malzeme_id),
			  FOREIGN KEY (urun_id)    REFERENCES kafe_menu_listesi(id) ON DELETE CASCADE,
			  FOREIGN KEY (malzeme_id) REFERENCES kafe_malzemeler(id)   ON DELETE RESTRICT,
			  CHECK (miktar > 0)
			) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_turkish_ci
			""",
			"""
			CREATE TABLE IF NOT EXISTS kafe_islem_gecmisi (
			  id         INT AUTO_INCREMENT PRIMARY KEY,
			  tarih      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
			  islem_turu VARCHAR(20)   NOT NULL,
			  kalem_turu VARCHAR(20)   NOT NULL,
			  kalem_adi  VARCHAR(100)  NOT NULL,
			  miktar     DECIMAL(12,2) NULL,
			  birim      VARCHAR(20)   NULL,
			  aciklama   VARCHAR(255)  NULL
			) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_turkish_ci
			""" };

	/**
	 * Tablolar yoksa oluşturur. Malzeme tablosu boşsa örnek menüyü yükler.
	 * @return örnek menü bu açılışta yüklendiyse true
	 */
	public boolean tablolariHazirla() throws SQLException {
		try (Statement st = bag().createStatement()) {
			for (String sql : TABLOLAR) st.execute(sql);
			st.executeUpdate("INSERT IGNORE INTO kafe_ayarlar (anahtar, deger) VALUES ('kafe_adi', 'Mola Kafe')");
			try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM kafe_malzemeler")) {
				rs.next();
				if (rs.getInt(1) > 0) return false;
			}
		}
		ornekVerileriYukle();
		return true;
	}

	private void ornekVerileriYukle() throws SQLException {
		islemde(c -> {
			Map<String, Integer> idler = new HashMap<>();
			String sql = "INSERT INTO kafe_malzemeler (malzeme_adi, birim, stok, min_stok, alerjen, vegan, vejetaryen) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				for (OrnekVeri.M m : OrnekVeri.MALZEMELER) {
					ps.setString(1, m.ad());
					ps.setString(2, m.birim());
					ps.setDouble(3, m.stok());
					ps.setDouble(4, m.min());
					ps.setString(5, m.alerjen());
					ps.setBoolean(6, m.vegan());
					ps.setBoolean(7, m.vejetaryen());
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						rs.next();
						idler.put(m.ad(), rs.getInt(1));
					}
				}
			}
			for (OrnekVeri.U u : OrnekVeri.URUNLER) {
				List<ReceteKalemi> recete = new ArrayList<>();
				for (int i = 0; i < u.recete().length; i += 2) {
					String ad = (String) u.recete()[i];
					double miktar = ((Number) u.recete()[i + 1]).doubleValue();
					Integer id = idler.get(ad);
					if (id == null) throw new IllegalStateException("Örnek veride bilinmeyen malzeme: " + ad);
					recete.add(new ReceteKalemi(id, ad, "", miktar, 0, 0, null));
				}
				Urun urun = new Urun(0, u.ad(), u.kategori(), new BigDecimal(u.fiyat()), "", "", false, false, u.stok(),
						u.min(), -1);
				urunYaz(c, urun, recete);
			}
			gecmiseYaz(c, "Ekleme", "Sistem", "Örnek menü", null, null,
					OrnekVeri.URUNLER.size() + " ürün ve " + OrnekVeri.MALZEMELER.size() + " malzeme yüklendi");
			return null;
		});
	}

	// =====================================================================
	// Okuma
	// =====================================================================

	public List<Urun> urunleriGetir() throws SQLException {
		String sql = """
				SELECT u.*, h.hazirlanabilir
				FROM kafe_menu_listesi u
				LEFT JOIN (
				    SELECT um.urun_id, MIN(FLOOR(m.stok / um.miktar)) AS hazirlanabilir
				    FROM kafe_urun_malzemeleri um
				    JOIN kafe_malzemeler m ON m.id = um.malzeme_id
				    GROUP BY um.urun_id
				) h ON h.urun_id = u.id
				ORDER BY u.kategori, u.urun_adi
				""";
		List<Urun> liste = new ArrayList<>();
		try (PreparedStatement ps = bag().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int hazirlanabilir = rs.getInt("hazirlanabilir");
				if (rs.wasNull()) hazirlanabilir = -1;
				liste.add(new Urun(rs.getInt("id"), rs.getString("urun_adi"), rs.getString("kategori"),
						rs.getBigDecimal("fiyat"), rs.getString("urun_icerigi"), rs.getString("alerjenler"),
						rs.getBoolean("vegan"), rs.getBoolean("vejetaryen"), rs.getInt("stok"), rs.getInt("min_stok"),
						hazirlanabilir));
			}
		}
		return liste;
	}

	public List<Malzeme> malzemeleriGetir() throws SQLException {
		String sql = """
				SELECT m.*, (SELECT COUNT(*) FROM kafe_urun_malzemeleri um WHERE um.malzeme_id = m.id) AS kullanim
				FROM kafe_malzemeler m
				ORDER BY m.malzeme_adi
				""";
		List<Malzeme> liste = new ArrayList<>();
		try (PreparedStatement ps = bag().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) liste.add(malzemeOku(rs, rs.getInt("kullanim")));
		}
		return liste;
	}

	private static Malzeme malzemeOku(ResultSet rs, int kullanim) throws SQLException {
		return new Malzeme(rs.getInt("id"), rs.getString("malzeme_adi"), rs.getString("birim"), rs.getDouble("stok"),
				rs.getDouble("min_stok"), rs.getString("alerjen"), rs.getBoolean("vegan"), rs.getBoolean("vejetaryen"),
				kullanim);
	}

	/** Bir ürünün içeriği (malzemeleri ve porsiyon başına miktarları). */
	public List<ReceteKalemi> receteGetir(int urunId) throws SQLException {
		String sql = """
				SELECT m.id, m.malzeme_adi, m.birim, um.miktar, m.stok, m.min_stok, m.alerjen
				FROM kafe_urun_malzemeleri um
				JOIN kafe_malzemeler m ON m.id = um.malzeme_id
				WHERE um.urun_id = ?
				ORDER BY um.sira
				""";
		List<ReceteKalemi> liste = new ArrayList<>();
		try (PreparedStatement ps = bag().prepareStatement(sql)) {
			ps.setInt(1, urunId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					liste.add(new ReceteKalemi(rs.getInt("id"), rs.getString("malzeme_adi"), rs.getString("birim"),
							rs.getDouble("miktar"), rs.getDouble("stok"), rs.getDouble("min_stok"),
							rs.getString("alerjen")));
				}
			}
		}
		return liste;
	}

	public List<Islem> gecmisiGetir() throws SQLException {
		String sql = "SELECT * FROM kafe_islem_gecmisi ORDER BY tarih DESC, id DESC LIMIT 1000";
		List<Islem> liste = new ArrayList<>();
		try (PreparedStatement ps = bag().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				BigDecimal miktar = rs.getBigDecimal("miktar");
				liste.add(new Islem(rs.getInt("id"), rs.getObject("tarih", LocalDateTime.class),
						rs.getString("islem_turu"), rs.getString("kalem_turu"), rs.getString("kalem_adi"),
						miktar == null ? null : miktar.doubleValue(), rs.getString("birim"), rs.getString("aciklama")));
			}
		}
		return liste;
	}

	public String kafeAdi() throws SQLException {
		try (PreparedStatement ps = bag().prepareStatement("SELECT deger FROM kafe_ayarlar WHERE anahtar = ?")) {
			ps.setString(1, "kafe_adi");
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString(1) : "Mola Kafe";
			}
		}
	}

	public void kafeAdiKaydet(String ad) throws SQLException {
		try (PreparedStatement ps = bag().prepareStatement("REPLACE INTO kafe_ayarlar (anahtar, deger) VALUES (?, ?)")) {
			ps.setString(1, "kafe_adi");
			ps.setString(2, ad);
			ps.executeUpdate();
		}
	}

	// =====================================================================
	// Ürün: ekle, düzenle, fiyat güncelle, sil
	// =====================================================================

	/** id == 0 ise yeni ürün ekler, değilse günceller. Ürünün ID'sini döndürür. */
	public int urunKaydet(Urun u, List<ReceteKalemi> recete) throws SQLException {
		return islemde(c -> {
			boolean yeni = u.id() == 0;
			BigDecimal eskiFiyat = yeni ? null : urunFiyati(c, u.id());
			int id = urunYaz(c, u, recete);
			if (yeni) {
				gecmiseYaz(c, "Ekleme", "Ürün", u.ad(), (double) u.stok(), "adet",
						u.kategori() + " · " + Tema.para(u.fiyat()));
			} else {
				gecmiseYaz(c, "Düzenleme", "Ürün", u.ad(), null, null, "Ürün bilgileri ve içeriği güncellendi");
				if (eskiFiyat != null && eskiFiyat.compareTo(u.fiyat()) != 0) {
					gecmiseYaz(c, "Fiyat", "Ürün", u.ad(), null, null,
							Tema.para(eskiFiyat) + " → " + Tema.para(u.fiyat()));
				}
			}
			return id;
		});
	}

	/** Ürün satırını ve içeriğini yazar (transaction içinden çağrılır). */
	private int urunYaz(Connection c, Urun u, List<ReceteKalemi> recete) throws SQLException {
		int id = u.id();
		if (id == 0) {
			String sql = "INSERT INTO kafe_menu_listesi (urun_adi, kategori, fiyat, stok, min_stok) VALUES (?, ?, ?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, u.ad());
				ps.setString(2, u.kategori());
				ps.setBigDecimal(3, u.fiyat());
				ps.setInt(4, u.stok());
				ps.setInt(5, u.minStok());
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys()) {
					rs.next();
					id = rs.getInt(1);
				}
			}
		} else {
			String sql = "UPDATE kafe_menu_listesi SET urun_adi = ?, kategori = ?, fiyat = ?, stok = ?, min_stok = ? WHERE id = ?";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setString(1, u.ad());
				ps.setString(2, u.kategori());
				ps.setBigDecimal(3, u.fiyat());
				ps.setInt(4, u.stok());
				ps.setInt(5, u.minStok());
				ps.setInt(6, id);
				if (ps.executeUpdate() == 0) throw new IllegalStateException("Ürün bulunamadı (silinmiş olabilir).");
			}
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM kafe_urun_malzemeleri WHERE urun_id = ?")) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
		String sql = "INSERT INTO kafe_urun_malzemeleri (urun_id, malzeme_id, miktar, sira) VALUES (?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int sira = 0;
			for (ReceteKalemi k : recete) {
				ps.setInt(1, id);
				ps.setInt(2, k.malzemeId());
				ps.setDouble(3, k.miktar());
				ps.setInt(4, sira++);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		ozetGuncelle(c, id);
		return id;
	}

	/** Ürünün içerik, alerjen, vegan ve vejetaryen sütunlarını malzemelerinden yeniden hesaplar. */
	private void ozetGuncelle(Connection c, int urunId) throws SQLException {
		List<Malzeme> malzemeler = new ArrayList<>();
		String sql = """
				SELECT m.* FROM kafe_urun_malzemeleri um
				JOIN kafe_malzemeler m ON m.id = um.malzeme_id
				WHERE um.urun_id = ? ORDER BY um.sira
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, urunId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) malzemeler.add(malzemeOku(rs, 0));
			}
		}
		Ozet o = ozetHesapla(malzemeler);
		sql = "UPDATE kafe_menu_listesi SET urun_icerigi = ?, alerjenler = ?, vegan = ?, vejetaryen = ? WHERE id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, o.icerik().length() > 1000 ? o.icerik().substring(0, 1000) : o.icerik());
			ps.setString(2, o.alerjenler());
			ps.setBoolean(3, o.vegan());
			ps.setBoolean(4, o.vejetaryen());
			ps.setInt(5, urunId);
			ps.executeUpdate();
		}
	}

	/** İçerik metnini, alerjenleri ve vegan/vejetaryen bilgisini malzemelerden hesaplar. */
	public static Ozet ozetHesapla(List<Malzeme> malzemeler) {
		StringJoiner icerik = new StringJoiner(", ");
		Set<String> alerjenler = new LinkedHashSet<>();
		boolean vegan = !malzemeler.isEmpty();
		boolean vejetaryen = !malzemeler.isEmpty();
		for (Malzeme m : malzemeler) {
			icerik.add(m.ad());
			if (m.alerjen() != null) {
				for (String a : m.alerjen().split(",")) {
					if (!a.isBlank()) alerjenler.add(a.trim());
				}
			}
			vegan &= m.vegan();
			vejetaryen &= m.vejetaryen() || m.vegan();
		}
		return new Ozet(icerik.toString(), String.join(", ", alerjenler), vegan, vejetaryen);
	}

	public void fiyatGuncelle(int urunId, BigDecimal yeniFiyat) throws SQLException {
		islemde(c -> {
			String ad = urunAdi(c, urunId);
			BigDecimal eski = urunFiyati(c, urunId);
			try (PreparedStatement ps = c.prepareStatement("UPDATE kafe_menu_listesi SET fiyat = ? WHERE id = ?")) {
				ps.setBigDecimal(1, yeniFiyat);
				ps.setInt(2, urunId);
				ps.executeUpdate();
			}
			gecmiseYaz(c, "Fiyat", "Ürün", ad, null, null, Tema.para(eski) + " → " + Tema.para(yeniFiyat));
			return null;
		});
	}

	public void urunSil(int urunId) throws SQLException {
		islemde(c -> {
			String ad = urunAdi(c, urunId);
			// Ürünün içerik satırları "ON DELETE CASCADE" sayesinde otomatik silinir.
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM kafe_menu_listesi WHERE id = ?")) {
				ps.setInt(1, urunId);
				ps.executeUpdate();
			}
			gecmiseYaz(c, "Silme", "Ürün", ad, null, null, "Ürün menüden kaldırıldı");
			return null;
		});
	}

	// =====================================================================
	// Malzeme: ekle, düzenle, sil
	// =====================================================================

	public int malzemeKaydet(Malzeme m) throws SQLException {
		return islemde(c -> {
			String alerjen = m.alerjen() == null || m.alerjen().isBlank() ? null : m.alerjen();
			int id = m.id();
			if (id == 0) {
				String sql = "INSERT INTO kafe_malzemeler (malzeme_adi, birim, stok, min_stok, alerjen, vegan, vejetaryen) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
				try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					malzemeParametreleri(ps, m, alerjen);
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						rs.next();
						id = rs.getInt(1);
					}
				}
				gecmiseYaz(c, "Ekleme", "Malzeme", m.ad(), m.stok(), m.birim(), "Yeni malzeme eklendi");
			} else {
				String sql = "UPDATE kafe_malzemeler SET malzeme_adi = ?, birim = ?, stok = ?, min_stok = ?, "
						+ "alerjen = ?, vegan = ?, vejetaryen = ? WHERE id = ?";
				try (PreparedStatement ps = c.prepareStatement(sql)) {
					malzemeParametreleri(ps, m, alerjen);
					ps.setInt(8, id);
					if (ps.executeUpdate() == 0) throw new IllegalStateException("Malzeme bulunamadı (silinmiş olabilir).");
				}
				// Malzemenin adı, alerjeni veya vegan bilgisi değiştiyse onu kullanan ürünlerin özetini yenile
				for (int urunId : kullananUrunIdleri(c, id)) ozetGuncelle(c, urunId);
				gecmiseYaz(c, "Düzenleme", "Malzeme", m.ad(), null, null, "Malzeme bilgileri güncellendi");
			}
			return id;
		});
	}

	private static void malzemeParametreleri(PreparedStatement ps, Malzeme m, String alerjen) throws SQLException {
		ps.setString(1, m.ad());
		ps.setString(2, m.birim());
		ps.setDouble(3, m.stok());
		ps.setDouble(4, m.minStok());
		ps.setString(5, alerjen);
		ps.setBoolean(6, m.vegan());
		ps.setBoolean(7, m.vejetaryen() || m.vegan());
	}

	public void malzemeSil(int malzemeId) throws SQLException {
		islemde(c -> {
			List<String> urunler = new ArrayList<>();
			String sql = """
					SELECT u.urun_adi FROM kafe_urun_malzemeleri um
					JOIN kafe_menu_listesi u ON u.id = um.urun_id
					WHERE um.malzeme_id = ? ORDER BY u.urun_adi
					""";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, malzemeId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) urunler.add(rs.getString(1));
				}
			}
			if (!urunler.isEmpty()) {
				throw new IllegalStateException("Bu malzeme şu ürünlerin içeriğinde kullanılıyor: "
						+ String.join(", ", urunler) + ". Önce bu ürünlerin içeriğinden çıkarın.");
			}
			String ad = malzemeAdi(c, malzemeId);
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM kafe_malzemeler WHERE id = ?")) {
				ps.setInt(1, malzemeId);
				ps.executeUpdate();
			}
			gecmiseYaz(c, "Silme", "Malzeme", ad, null, null, "Malzeme listeden kaldırıldı");
			return null;
		});
	}

	// =====================================================================
	// Stok: sipariş, hazırlama, satış
	// =====================================================================

	/** Tedarikçiden gelen siparişi stoğa ekler. urunMu=false ise malzeme siparişidir. */
	public void siparisVer(boolean urunMu, int id, double miktar, String not) throws SQLException {
		islemde(c -> {
			siparisYaz(c, urunMu, id, miktar, not);
			return null;
		});
	}

	/** Birden fazla malzemeyi tek seferde sipariş eder: {malzemeId -> miktar} */
	public void malzemeSiparisleri(Map<Integer, Double> miktarlar, String not) throws SQLException {
		islemde(c -> {
			for (Map.Entry<Integer, Double> e : miktarlar.entrySet()) siparisYaz(c, false, e.getKey(), e.getValue(), not);
			return null;
		});
	}

	private void siparisYaz(Connection c, boolean urunMu, int id, double miktar, String not) throws SQLException {
		if (miktar <= 0) throw new IllegalArgumentException("Sipariş miktarı sıfırdan büyük olmalı.");
		String tablo = urunMu ? "kafe_menu_listesi" : "kafe_malzemeler";
		String ad = urunMu ? urunAdi(c, id) : malzemeAdi(c, id);
		String birim = "adet";
		if (!urunMu) {
			try (PreparedStatement ps = c.prepareStatement("SELECT birim FROM kafe_malzemeler WHERE id = ?")) {
				ps.setInt(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) birim = rs.getString(1);
				}
			}
		}
		// Tablo adı kullanıcıdan gelmiyor (yukarıda sabit iki seçenekten biri), değerler yine '?' ile gidiyor.
		try (PreparedStatement ps = c.prepareStatement("UPDATE " + tablo + " SET stok = stok + ? WHERE id = ?")) {
			if (urunMu) ps.setLong(1, Math.round(miktar));
			else ps.setDouble(1, miktar);
			ps.setInt(2, id);
			ps.executeUpdate();
		}
		String aciklama = "Sipariş teslim alındı, stoğa eklendi";
		if (not != null && !not.isBlank()) aciklama += " · " + not.trim();
		gecmiseYaz(c, "Sipariş", urunMu ? "Ürün" : "Malzeme", ad, miktar, birim, aciklama);
	}

	/**
	 * Ürünü mutfakta hazırlar: malzemeleri stoktan düşer, ürün stoğunu artırır.
	 * Malzeme yetmiyorsa HİÇBİR ŞEYİ değiştirmez ve eksik malzemelerin listesini döndürür.
	 * @return boş liste = başarıyla hazırlandı
	 */
	public List<EksikMalzeme> hazirla(int urunId, int adet) throws SQLException {
		if (adet <= 0) throw new IllegalArgumentException("Porsiyon sayısı sıfırdan büyük olmalı.");
		return islemde(c -> {
			record Dusum(int malzemeId, double miktar) {
			}
			String ad = urunAdi(c, urunId);
			List<EksikMalzeme> eksikler = new ArrayList<>();
			List<Dusum> dusumler = new ArrayList<>();
			String sql = """
					SELECT m.id, m.malzeme_adi, m.birim, m.stok, m.min_stok, um.miktar
					FROM kafe_urun_malzemeleri um
					JOIN kafe_malzemeler m ON m.id = um.malzeme_id
					WHERE um.urun_id = ?
					FOR UPDATE
					""";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, urunId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						double gereken = rs.getDouble("miktar") * adet;
						double stok = rs.getDouble("stok");
						if (stok + 1e-9 < gereken) {
							eksikler.add(new EksikMalzeme(rs.getInt("id"), rs.getString("malzeme_adi"),
									rs.getString("birim"), gereken, stok, rs.getDouble("min_stok")));
						}
						dusumler.add(new Dusum(rs.getInt("id"), gereken));
					}
				}
			}
			if (dusumler.isEmpty()) {
				throw new IllegalStateException("“" + ad + "” için ürün içeriği tanımlı değil. Önce içeriğini düzenleyin.");
			}
			if (!eksikler.isEmpty()) return eksikler;

			try (PreparedStatement ps = c.prepareStatement("UPDATE kafe_malzemeler SET stok = stok - ? WHERE id = ?")) {
				for (Dusum d : dusumler) {
					ps.setDouble(1, d.miktar());
					ps.setInt(2, d.malzemeId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
			try (PreparedStatement ps = c.prepareStatement("UPDATE kafe_menu_listesi SET stok = stok + ? WHERE id = ?")) {
				ps.setInt(1, adet);
				ps.setInt(2, urunId);
				ps.executeUpdate();
			}
			gecmiseYaz(c, "Hazırlama", "Ürün", ad, (double) adet, "porsiyon",
					dusumler.size() + " malzeme stoktan düşüldü");
			return eksikler;
		});
	}

	/** Satış yapar: ürün stoğunu azaltır. */
	public void sat(int urunId, int adet) throws SQLException {
		if (adet <= 0) throw new IllegalArgumentException("Satış adedi sıfırdan büyük olmalı.");
		islemde(c -> {
			String ad;
			BigDecimal fiyat;
			int stok;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT urun_adi, fiyat, stok FROM kafe_menu_listesi WHERE id = ? FOR UPDATE")) {
				ps.setInt(1, urunId);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) throw new IllegalStateException("Ürün bulunamadı (silinmiş olabilir).");
					ad = rs.getString(1);
					fiyat = rs.getBigDecimal(2);
					stok = rs.getInt(3);
				}
			}
			if (stok < adet) {
				throw new IllegalStateException("Stokta yalnızca " + stok + " adet “" + ad + "” var. "
						+ "Önce hazırlayın veya sipariş verin.");
			}
			try (PreparedStatement ps = c.prepareStatement("UPDATE kafe_menu_listesi SET stok = stok - ? WHERE id = ?")) {
				ps.setInt(1, adet);
				ps.setInt(2, urunId);
				ps.executeUpdate();
			}
			gecmiseYaz(c, "Satış", "Ürün", ad, (double) adet, "adet",
					"Tutar: " + Tema.para(fiyat.multiply(BigDecimal.valueOf(adet))));
			return null;
		});
	}

	// =====================================================================
	// Sistem kontrolü: ekle -> yeniden aç -> fiyat güncelle -> sil
	// =====================================================================

	/**
	 * Geçici bir test ürünüyle ekleme, fiyat güncelleme ve silme işlemlerini dener.
	 * Her adımdan sonra kayıt, YENİ bir bağlantıyla okunur. Bu, programı kapatıp yeniden açmakla aynı şeyi
	 * kanıtlar: veri programın belleğinde değil, MySQL'de kalıcı olarak duruyor.
	 */
	public List<KontrolAdimi> sistemKontrolu() {
		List<KontrolAdimi> adimlar = new ArrayList<>();
		String testAdi = "Kontrol Ürünü #" + (System.currentTimeMillis() % 100000);
		int testId = 0;
		BigDecimal ilkFiyat = new BigDecimal("50.00");
		BigDecimal yeniFiyat = new BigDecimal("65.00");
		try {
			bag();
			adimlar.add(new KontrolAdimi("MySQL bağlantısı", true, baglantiBilgisi));

			int tabloSayisi;
			String sql = """
					SELECT COUNT(*) FROM information_schema.tables
					WHERE table_schema = DATABASE() AND table_name IN
					('kafe_ayarlar','kafe_malzemeler','kafe_menu_listesi','kafe_urun_malzemeleri','kafe_islem_gecmisi')
					""";
			try (PreparedStatement ps = bag().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
				rs.next();
				tabloSayisi = rs.getInt(1);
			}
			adimlar.add(new KontrolAdimi("Tablolar", tabloSayisi == 5, tabloSayisi + " / 5 kafe tablosu bulundu"));

			List<Malzeme> malzemeler = malzemeleriGetir();
			if (malzemeler.isEmpty()) throw new IllegalStateException("Test için en az bir malzeme gerekli.");
			Malzeme ilk = malzemeler.get(0);
			testId = urunKaydet(new Urun(0, testAdi, "İçecek", ilkFiyat, "", "", false, false, 3, 1, -1),
					List.of(new ReceteKalemi(ilk.id(), ilk.ad(), ilk.birim(), 1, ilk.stok(), ilk.minStok(),
							ilk.alerjen())));
			adimlar.add(new KontrolAdimi("Ürün ekle", true, "“" + testAdi + "” eklendi (ID " + testId + ")"));

			BigDecimal okunan = yeniBaglantiylaFiyatOku(testId);
			adimlar.add(new KontrolAdimi("Kapatıp açınca kayıt duruyor mu?",
					okunan != null && okunan.compareTo(ilkFiyat) == 0,
					okunan == null ? "Kayıt bulunamadı!" : "Yeni bağlantıyla okundu: " + Tema.para(okunan)));

			fiyatGuncelle(testId, yeniFiyat);
			okunan = yeniBaglantiylaFiyatOku(testId);
			adimlar.add(new KontrolAdimi("Fiyat güncelle", okunan != null && okunan.compareTo(yeniFiyat) == 0,
					Tema.para(ilkFiyat) + " → " + (okunan == null ? "?" : Tema.para(okunan))
							+ " (yeni bağlantıyla doğrulandı)"));

			urunSil(testId);
			okunan = yeniBaglantiylaFiyatOku(testId);
			adimlar.add(new KontrolAdimi("Ürün sil", okunan == null,
					okunan == null ? "Kayıt kalıcı olarak silindi" : "Kayıt hâlâ duruyor!"));
			testId = 0;
		} catch (Exception e) {
			adimlar.add(new KontrolAdimi("Hata", false, hataMesaji(e)));
		} finally {
			try {
				if (testId != 0) urunSil(testId);
				try (PreparedStatement ps = bag().prepareStatement("DELETE FROM kafe_islem_gecmisi WHERE kalem_adi = ?")) {
					ps.setString(1, testAdi);
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				// temizlik başarısız olursa test ürünü listede görünür, elle silinebilir
			}
		}
		return adimlar;
	}

	private BigDecimal yeniBaglantiylaFiyatOku(int urunId) throws SQLException {
		try (Connection yeni = DriverManager.getConnection(url, kullanici, sifre);
				PreparedStatement ps = yeni.prepareStatement("SELECT fiyat FROM kafe_menu_listesi WHERE id = ?")) {
			ps.setInt(1, urunId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getBigDecimal(1) : null;
			}
		}
	}

	// =====================================================================
	// Yardımcılar
	// =====================================================================

	@FunctionalInterface
	private interface SqlIsi<T> {
		T calistir(Connection c) throws SQLException;
	}

	/** İşi tek bir transaction içinde çalıştırır: hata olursa yapılan her şey geri alınır (rollback). */
	private <T> T islemde(SqlIsi<T> is) throws SQLException {
		Connection c = bag();
		c.setAutoCommit(false);
		try {
			T sonuc = is.calistir(c);
			c.commit();
			return sonuc;
		} catch (SQLException | RuntimeException e) {
			c.rollback();
			throw e;
		} finally {
			c.setAutoCommit(true);
		}
	}

	private static String urunAdi(Connection c, int id) throws SQLException {
		return tekMetin(c, "SELECT urun_adi FROM kafe_menu_listesi WHERE id = ?", id, "Ürün");
	}

	private static String malzemeAdi(Connection c, int id) throws SQLException {
		return tekMetin(c, "SELECT malzeme_adi FROM kafe_malzemeler WHERE id = ?", id, "Malzeme");
	}

	private static String tekMetin(Connection c, String sql, int id, String ne) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) throw new IllegalStateException(ne + " bulunamadı (silinmiş olabilir).");
				return rs.getString(1);
			}
		}
	}

	private static BigDecimal urunFiyati(Connection c, int id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT fiyat FROM kafe_menu_listesi WHERE id = ?")) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) throw new IllegalStateException("Ürün bulunamadı (silinmiş olabilir).");
				return rs.getBigDecimal(1);
			}
		}
	}

	private static List<Integer> kullananUrunIdleri(Connection c, int malzemeId) throws SQLException {
		List<Integer> idler = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT urun_id FROM kafe_urun_malzemeleri WHERE malzeme_id = ?")) {
			ps.setInt(1, malzemeId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) idler.add(rs.getInt(1));
			}
		}
		return idler;
	}

	private static void gecmiseYaz(Connection c, String tur, String kalemTuru, String ad, Double miktar, String birim,
			String aciklama) throws SQLException {
		String sql = "INSERT INTO kafe_islem_gecmisi (islem_turu, kalem_turu, kalem_adi, miktar, birim, aciklama) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, tur);
			ps.setString(2, kalemTuru);
			ps.setString(3, ad);
			if (miktar == null) ps.setNull(4, java.sql.Types.DECIMAL);
			else ps.setDouble(4, miktar);
			ps.setString(5, birim);
			ps.setString(6, aciklama == null ? null : aciklama.length() > 255 ? aciklama.substring(0, 255) : aciklama);
			ps.executeUpdate();
		}
	}

	/** Hataları yeni başlayan birinin anlayacağı Türkçe mesaja çevirir. */
	public static String hataMesaji(Throwable e) {
		if (e instanceof SQLException s) {
			String durum = s.getSQLState() == null ? "" : s.getSQLState();
			switch (s.getErrorCode()) {
				case 1045:
					return "Kullanıcı adı veya şifre hatalı.";
				case 1049:
					return "Bu adda bir veritabanı bulunamadı. Veritabanı adını kontrol edin.";
				case 1062:
					return "Bu isimde bir kayıt zaten var. Lütfen farklı bir ad girin.";
				case 1451:
					return "Bu kayıt başka kayıtlarda kullanıldığı için silinemez.";
				case 1406:
					return "Girilen metin çok uzun.";
				case 3819:
					return "Geçersiz değer: fiyat sıfırdan büyük, stok ise negatif olmamalı.";
				default:
					if (durum.startsWith("08")) {
						return "MySQL sunucusuna ulaşılamadı. MySQL'in çalıştığından ve sunucu/port bilgisinin "
								+ "doğru olduğundan emin olun.";
					}
					return "Veritabanı hatası: " + s.getMessage();
			}
		}
		return e.getMessage() != null ? e.getMessage() : e.toString();
	}
}

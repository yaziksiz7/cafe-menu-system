package KafeMenu;

import java.time.LocalDateTime;

/** İşlem geçmişindeki bir kayıt = kafe_islem_gecmisi tablosundaki bir satır. */
public record Islem(int id, LocalDateTime tarih, String tur, String kalemTuru, String kalemAdi,
		Double miktar, String birim, String aciklama) {
}

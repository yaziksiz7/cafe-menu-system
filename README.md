# ☕ Kafe Menü ve Stok Yönetim Sistemi

Java Swing kullanılarak geliştirilmiş; kafe menüsü, ürün reçeteleri, malzeme stok durumu ve işlem geçmişini tek bir masaüstü arayüzünden yönetmeyi sağlayan uygulama.

## 🚀 Özellikler

- **Kullanıcı Girişi:** Güvenli giriş ekranı (`GirisEkrani`).
- **Menü Yönetimi:** Kafe menüsündeki ürünlerin listelenmesi ve yönetilmesi (`MenuPaneli`).
- **Reçete ve Malzeme Takibi:** Her ürünün içerdiği malzeme miktarlarının tanımlanması (`ReceteKalemi`, `MalzemePaneli`).
- **Akıllı Stok Kontrolü:** Mevcut stok durumunun izlenmesi (`StokDurumu`) ve eksik malzemelerin tespit edilmesi (`EksikMalzeme`).
- **İşlem Geçmişi:** Gerçekleştirilen işlemlerin kayıt altına alınması ve görüntülenmesi (`Islem`, `GecmisPaneli`).
- **Özelleştirilmiş Arayüz:** Özel tema, ikonlar, tablolar ve diyalog pencereleri (`Tema`, `Simge`, `Tablolar`, `Diyaloglar`).
- **Hazır Örnek Veri:** Uygulamanın hızlıca test edilebilmesi için başlangıç verileri (`OrnekVeri`).

## 🛠️ Kullanılan Teknolojiler

- **Dil:** Java
- **Arayüz (GUI):** Java Swing
- **IDE:** Eclipse

## 📂 Proje Yapısı

| Dosya | Açıklama |
| :--- | :--- |
| `KafeMenuUygulamasi.java` | Uygulamanın başlangıç noktası (`main` sınıfı) |
| `GirisEkrani.java` | Kullanıcı giriş arayüzü |
| `AnaPencere.java` | Tüm panelleri barındıran ana pencere |
| `MenuPaneli.java` | Menü ürünlerinin yönetildiği panel |
| `MalzemePaneli.java` | Malzeme ve stok işlemlerinin yapıldığı panel |
| `GecmisPaneli.java` | İşlem geçmişinin listelendiği panel |
| `Malzeme.java` / `ReceteKalemi.java` | Malzeme ve reçete veri modelleri |
| `StokDurumu.java` / `EksikMalzeme.java` | Stok ve eksik malzeme kontrol sınıfları |
| `Tema.java` / `Bilesenler.java` / `Simge.java` | Özel arayüz bileşenleri ve görsel tema |

## 💻 Kurulum ve Çalıştırma

1. Repoyu bilgisayarınıza klonlayın:
   ```bash
   git clone [https://github.com/yaziksiz7/kafemen-sistemi.git](https://github.com/yaziksiz7/kafemen-sistemi.git)

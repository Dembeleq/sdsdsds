ChicagoRP v0.3 - Paper 1.21.4 (Java 21), modsuz

1) PLUGIN GUNCELLEME: bu zip'teki src/, tools/, pom.xml ve README.txt dosyalarini GitHub deponun uzerine yukle.
   Actions > Build bitince jar'i indir, plugins/ klasorune at (eskisini sil), sunucuyu yeniden baslat.

2) KAYNAK PAKETI (3D arabalar, telefon, uygulama ikonlari, 3D tabelalar): ChicagoRP-ResourcePack.zip
   - Zip'i GitHub deponun ANA KLASORUNE yukle. Indirme linki:
       https://github.com/KULLANICI/DEPO/raw/main/ChicagoRP-ResourcePack.zip
   - plugins/ChicagoRP/config.yml icinde:  resource-pack: url: "<link>"   (sha1 hazir yazili)
   - /rpreload yaz (veya sunucuyu yeniden baslat). Oyuncular girince paketi otomatik indirir.
   - Paket olmadan da calisir ama modeller yerine duz esyalar gorunur.

3) SEHRI KURMAK (op, tek komut):  /sehir dunya     (ilerleme: /sehir durum)
   - Tabela yazisi ters gorunuyorsa: /sehir tabelacevir     Araba ters gorunuyorsa: /sehir araccevir

4) YENI KOMUTLAR: /paraver <oyuncu> <miktar>  /paraayarla <oyuncu> <miktar>  /harita  /rpreload
   /arac kilit | yolcu | izin <oyuncu> | at [oyuncu] | ayar | park | is     /meslek birak
   Meslek kontenjanlari, mesajlar, sunucu adi: config.yml

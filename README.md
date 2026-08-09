# Minecraft-Easy-Anti-Cheat
RAENCEL ANTİ - CHEAT

# RAENCEL Anti-Cheat (Server-Side)

![Java](https://shields.io)
![Minecraft](https://shields.io)
![License](https://shields.io)

RAENCEL Anti-Cheat, Minecraft (Spigot/Paper) sunucuları için sıfır kurulum (Zero-Configuration) prensibiyle tasarlanmış, tamamen sunucu taraflı (Server-Side) çalışan hafif ve agresif bir hile algılama motorudur. Oyuncuların bilgisayarına herhangi bir harici `.exe` veya `.dll` dosyası indirtmeden, sadece paket ve fizik analizleriyle en yaygın hileleri engeller.

## 🚀 Özellikler

- **Fly / Hover Modülü:** Yerçekimi yasalarını ve paket verilerini analiz ederek havada asılı kalan veya uçan hilecileri (Fly, Hover, Glide) anında tespit eder ve geri fırlatır (Rubberband).
- **SpeedHack Modülü:** Koşma ve zıplama hız limitlerini yatay düzlemde matematiksel (vektörel) olarak denetler; anormal hız artışlarını engeller.
- **Combat / KillAura Modülü:** 
  - *Açı Filtresi (Heuristics):* Oyuncunun bakış açısı (Dot Product) ile vurduğu hedefin konumunu karşılaştırır. Arkasındaki adama vuran hilecileri eler.
  - *CPS Limiter:* İnsani limitlerin üzerindeki tıklama/saldırı hızlarını (anormal milisaniye farklarını) yakalar ve hasarı iptal eder.

## 🛠️ Kurulum ve Derleme

Proje herhangi bir harici bağımlılık gerektirmez. Doğrudan kaynak kodunu kendi Spigot/Paper projenize entegre edebilir veya Maven ile derleyebilirsiniz.

### Maven Bağımlılığı (`pom.xml`)
```xml
<dependency>
    <groupId>org.spigotmc</groupId>
    <artifactId>spigot-api</artifactId>
    <version>1.20.2-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

## 📜 Lisans
Bu proje **MIT Lisansı** ile korunmaktadır. Açık kaynak kodlu olarak özgürce kullanılabilir, değiştirilebilir ve dağıtılabilir.

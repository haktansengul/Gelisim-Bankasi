CREATE DATABASE yeni_banka;
USE yeni_banka;

-- Kullanýcýlar Tablosu
CREATE TABLE kullanicilar (
    tc_no CHAR(11) PRIMARY KEY,
    ad VARCHAR(50) NOT NULL,
    soyad VARCHAR(50) NOT NULL,
    sifre VARCHAR(255) NOT NULL,
    bakiye DECIMAL(15, 2) DEFAULT 0.00,
    kayit_tarihi DATETIME DEFAULT GETDATE()
);

-- Kredi Kartlarý Tablosu
CREATE TABLE kredi_kartlari (
    kart_id CHAR(16) PRIMARY KEY,
    tc_no CHAR(11),
    limit_miktar DECIMAL(15, 2) DEFAULT 5000.00,
    guncel_borc DECIMAL(15, 2) DEFAULT 0.00,
    son_odeme_tarihi DATE,
    cvv CHAR(3),
    FOREIGN KEY (tc_no) REFERENCES kullanicilar(tc_no) ON DELETE CASCADE
);

-- Ýþlem Geçmiþi Tablosu
CREATE TABLE islem_gecmisi (
    islem_id INT IDENTITY(1,1) PRIMARY KEY,
    tc_no CHAR(11),
    islem_tipi VARCHAR(20),
    miktar DECIMAL(15, 2),
    islem_tarihi DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (tc_no) REFERENCES kullanicilar(tc_no) ON DELETE CASCADE
);

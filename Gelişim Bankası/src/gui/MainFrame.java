package gui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import database.DatabaseConfig;

public class MainFrame extends JFrame {
    private final String tcNo;
    private final String adSoyad;
    private JLabel bakiyeLabel;
    
    public MainFrame(String tcNo, String adSoyad) {
        this.tcNo = tcNo;
        this.adSoyad = adSoyad;
        
        setTitle("Ana Menü");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        
        initComponents();
        bakiyeGuncelle();
    }
    
    private void initComponents() {
        // Üst Panel
        JPanel ustPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel hosgeldinLabel = new JLabel("Hoş geldiniz, " + adSoyad);
        hosgeldinLabel.setFont(new Font("Arial", Font.BOLD, 18));
        bakiyeLabel = new JLabel();
        bakiyeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        ustPanel.add(hosgeldinLabel);
        ustPanel.add(Box.createHorizontalStrut(20));
        ustPanel.add(bakiyeLabel);
        
        add(ustPanel, BorderLayout.NORTH);
        
        // Ana Panel - İki bölümlü
        JPanel anaPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        anaPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Banka Hesabı İşlemleri Paneli
        JPanel bankaPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        bankaPanel.setBorder(BorderFactory.createTitledBorder("Banka Hesabı İşlemleri"));
        
        JButton paraYatirButton = new JButton("Para Yatır");
        JButton paraCekButton = new JButton("Para Çek");
        JButton havaleButton = new JButton("Havale/EFT");
        
        bankaPanel.add(paraYatirButton);
        bankaPanel.add(paraCekButton);
        bankaPanel.add(havaleButton);
        
        // Kredi Kartı İşlemleri Paneli
        JPanel krediPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        krediPanel.setBorder(BorderFactory.createTitledBorder("Kredi Kartı İşlemleri"));
        
        JButton kartBilgiButton = new JButton("Kart Bilgilerini Görüntüle");
        JButton krediCekButton = new JButton("Kredi Çek");
        JButton borcOdeButton = new JButton("Borç Öde");
        
        krediPanel.add(kartBilgiButton);
        krediPanel.add(krediCekButton);
        krediPanel.add(borcOdeButton);
        
        anaPanel.add(bankaPanel);
        anaPanel.add(krediPanel);
        
        add(anaPanel, BorderLayout.CENTER);
        
        // Çıkış Butonu
        JButton cikisButton = new JButton("Çıkış");
        cikisButton.setBackground(Color.RED);
        cikisButton.setForeground(Color.WHITE);
        JPanel altPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        altPanel.add(cikisButton);
        add(altPanel, BorderLayout.SOUTH);
        
        // Action Listeners
        paraYatirButton.addActionListener(e -> paraIslemi("yatir"));
        paraCekButton.addActionListener(e -> paraIslemi("cek"));
        havaleButton.addActionListener(e -> havaleYap());
        kartBilgiButton.addActionListener(e -> kartBilgileriGoster());
        krediCekButton.addActionListener(e -> krediCek());
        borcOdeButton.addActionListener(e -> borcOde());
        cikisButton.addActionListener(e -> cikisYap());
    }
    
    private void bakiyeGuncelle() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String query = "SELECT bakiye FROM kullanicilar WHERE tc_no = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, tcNo);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double bakiye = rs.getDouble("bakiye");
                bakiyeLabel.setText(String.format("Bakiye: %.2f TL", bakiye));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Bakiye güncelleme hatası: " + ex.getMessage());
        }
    }
    
    private void paraIslemi(String islemTipi) {
        String miktar = JOptionPane.showInputDialog(this, 
            islemTipi.equals("yatir") ? "Yatırılacak miktar:" : "Çekilecek miktar:");
            
        if (miktar == null || miktar.isEmpty()) return;
        
        try {
            double tutar = Double.parseDouble(miktar);
            if (tutar <= 0) {
                JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
                return;
            }
            
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                
                // Bakiye kontrolü (para çekme işlemi için)
                if (islemTipi.equals("cek")) {
                    String kontrolQuery = "SELECT bakiye FROM kullanicilar WHERE tc_no = ?";
                    PreparedStatement kontrolStmt = conn.prepareStatement(kontrolQuery);
                    kontrolStmt.setString(1, tcNo);
                    ResultSet rs = kontrolStmt.executeQuery();
                    
                    if (rs.next() && rs.getDouble("bakiye") < tutar) {
                        JOptionPane.showMessageDialog(this, "Yetersiz bakiye!");
                        return;
                    }
                }
                
                // Bakiye güncelleme
                String updateQuery = "UPDATE kullanicilar SET bakiye = bakiye " + 
                                   (islemTipi.equals("yatir") ? "+" : "-") + 
                                   " ? WHERE tc_no = ?";
                PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                pstmt.setDouble(1, tutar);
                pstmt.setString(2, tcNo);
                pstmt.executeUpdate();
                
                // İşlem kaydı
                String islemQuery = "INSERT INTO islem_gecmisi (tc_no, islem_tipi, miktar) VALUES (?, ?, ?)";
                PreparedStatement islemStmt = conn.prepareStatement(islemQuery);
                islemStmt.setString(1, tcNo);
                islemStmt.setString(2, islemTipi.equals("yatir") ? "PARA_YATIRMA" : "PARA_CEKME");
                islemStmt.setDouble(3, tutar);
                islemStmt.executeUpdate();
                
                conn.commit();
                JOptionPane.showMessageDialog(this, "İşlem başarılı!");
                bakiyeGuncelle();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "İşlem hatası: " + ex.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
        }
    }
    
    private void havaleYap() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField aliciTcField = new JTextField();
        JTextField miktarField = new JTextField();
        
        panel.add(new JLabel("Alıcı TC:"));
        panel.add(aliciTcField);
        panel.add(new JLabel("Miktar:"));
        panel.add(miktarField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Havale/EFT", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    
        if (result != JOptionPane.OK_OPTION) return;
        
        String aliciTc = aliciTcField.getText();
        String miktarStr = miktarField.getText();
        
        try {
            double miktar = Double.parseDouble(miktarStr);
            if (miktar <= 0) {
                JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
                return;
            }
            
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                
                // Gönderen bakiye kontrolü
                String kontrolQuery = "SELECT bakiye FROM kullanicilar WHERE tc_no = ?";
                PreparedStatement kontrolStmt = conn.prepareStatement(kontrolQuery);
                kontrolStmt.setString(1, tcNo);
                ResultSet rs = kontrolStmt.executeQuery();
                
                if (!rs.next() || rs.getDouble("bakiye") < miktar) {
                    JOptionPane.showMessageDialog(this, "Yetersiz bakiye!");
                    return;
                }
                
                // Alıcı kontrolü
                kontrolStmt.setString(1, aliciTc);
                if (!kontrolStmt.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Alıcı hesap bulunamadı!");
                    return;
                }
                
                // Gönderen hesaptan düş
                String updateQuery = "UPDATE kullanicilar SET bakiye = bakiye - ? WHERE tc_no = ?";
                PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                pstmt.setDouble(1, miktar);
                pstmt.setString(2, tcNo);
                pstmt.executeUpdate();
                
                // Alıcı hesaba ekle
                updateQuery = "UPDATE kullanicilar SET bakiye = bakiye + ? WHERE tc_no = ?";
                pstmt = conn.prepareStatement(updateQuery);
                pstmt.setDouble(1, miktar);
                pstmt.setString(2, aliciTc);
                pstmt.executeUpdate();
                
                // İşlem kayıtları
                String islemQuery = "INSERT INTO islem_gecmisi (tc_no, islem_tipi, miktar) VALUES (?, ?, ?)";
                PreparedStatement islemStmt = conn.prepareStatement(islemQuery);
                
                // Gönderen işlem kaydı
                islemStmt.setString(1, tcNo);
                islemStmt.setString(2, "HAVALE_GONDERIM");
                islemStmt.setDouble(3, miktar);
                islemStmt.executeUpdate();
                
                // Alıcı işlem kaydı
                islemStmt.setString(1, aliciTc);
                islemStmt.setString(2, "HAVALE_ALIM");
                islemStmt.setDouble(3, miktar);
                islemStmt.executeUpdate();
                
                conn.commit();
                JOptionPane.showMessageDialog(this, "Havale başarıyla gerçekleşti!");
                bakiyeGuncelle();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Havale hatası: " + ex.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
        }
    }
    
    private void kartBilgileriGoster() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String query = "SELECT * FROM kredi_kartlari WHERE tc_no = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, tcNo);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String bilgi = String.format("""
                    Kart Numarası: %s
                    Limit: %.2f TL
                    Güncel Borç: %.2f TL
                    Son Ödeme Tarihi: %s
                    CVV: %s""",
                    rs.getString("kart_id"),
                    rs.getDouble("limit_miktar"),
                    rs.getDouble("guncel_borc"),
                    rs.getDate("son_odeme_tarihi"),
                    rs.getString("cvv"));
                    
                JOptionPane.showMessageDialog(this, bilgi, "Kart Bilgileri", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Kredi kartı bilgisi bulunamadı!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Bilgi görüntüleme hatası: " + ex.getMessage());
        }
    }
    
    private void krediCek() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Mevcut limit ve borç kontrolü
            String kontrolQuery = "SELECT limit_miktar, guncel_borc FROM kredi_kartlari WHERE tc_no = ?";
            PreparedStatement kontrolStmt = conn.prepareStatement(kontrolQuery);
            kontrolStmt.setString(1, tcNo);
            ResultSet rs = kontrolStmt.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Kredi kartı bulunamadı!");
                return;
            }
            
            double limit = rs.getDouble("limit_miktar");
            double mevcutBorc = rs.getDouble("guncel_borc");
            double kullanilabilirLimit = limit - mevcutBorc;
            
            String miktar = JOptionPane.showInputDialog(this, 
                String.format("Kullanılabilir limit: %.2f TL\nÇekmek istediğiniz miktar:", kullanilabilirLimit));
                
            if (miktar == null || miktar.isEmpty()) return;
            
            double cekilenMiktar = Double.parseDouble(miktar);
            if (cekilenMiktar <= 0 || cekilenMiktar > kullanilabilirLimit) {
                JOptionPane.showMessageDialog(this, "Geçersiz miktar veya limit yetersiz!");
                return;
            }
            
            double borcMiktari = cekilenMiktar * 1.30; // %30 faiz
            
            conn.setAutoCommit(false);
            
            // Borç güncelleme
            String updateQuery = "UPDATE kredi_kartlari SET guncel_borc = guncel_borc + ? WHERE tc_no = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, borcMiktari);
            pstmt.setString(2, tcNo);
            pstmt.executeUpdate();
            
            // Bakiye güncelleme
            updateQuery = "UPDATE kullanicilar SET bakiye = bakiye + ? WHERE tc_no = ?";
            pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, cekilenMiktar);
            pstmt.setString(2, tcNo);
            pstmt.executeUpdate();
            
            conn.commit();
            
            JOptionPane.showMessageDialog(this, 
                String.format("Kredi çekme işlemi başarılı!\nÇekilen: %.2f TL\nToplam Borç: %.2f TL", 
                    cekilenMiktar, borcMiktari));
            bakiyeGuncelle();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Kredi çekme hatası: " + ex.getMessage());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
        }
    }
    
    private void borcOde() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Mevcut borç kontrolü
            String kontrolQuery = "SELECT guncel_borc FROM kredi_kartlari WHERE tc_no = ?";
            PreparedStatement kontrolStmt = conn.prepareStatement(kontrolQuery);
            kontrolStmt.setString(1, tcNo);
            ResultSet rs = kontrolStmt.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Kredi kartı bulunamadı!");
                return;
            }
            
            double mevcutBorc = rs.getDouble("guncel_borc");
            if (mevcutBorc <= 0) {
                JOptionPane.showMessageDialog(this, "Ödenecek borcunuz bulunmamaktadır.");
                return;
            }
            
            String miktar = JOptionPane.showInputDialog(this, 
                String.format("Mevcut borç: %.2f TL\nÖdemek istediğiniz miktar:", mevcutBorc));
                
            if (miktar == null || miktar.isEmpty()) return;
            
            double odenenMiktar = Double.parseDouble(miktar);
            if (odenenMiktar <= 0 || odenenMiktar > mevcutBorc) {
                JOptionPane.showMessageDialog(this, "Geçersiz ödeme miktarı!");
                return;
            }
            
            // Bakiye kontrolü
            kontrolQuery = "SELECT bakiye FROM kullanicilar WHERE tc_no = ?";
            kontrolStmt = conn.prepareStatement(kontrolQuery);
            kontrolStmt.setString(1, tcNo);
            rs = kontrolStmt.executeQuery();
            
            if (!rs.next() || rs.getDouble("bakiye") < odenenMiktar) {
                JOptionPane.showMessageDialog(this, "Yetersiz bakiye!");
                return;
            }
            
            conn.setAutoCommit(false);
            
            // Borç güncelleme
            String updateQuery = "UPDATE kredi_kartlari SET guncel_borc = guncel_borc - ? WHERE tc_no = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, odenenMiktar);
            pstmt.setString(2, tcNo);
            pstmt.executeUpdate();
            
            // Bakiye güncelleme
            updateQuery = "UPDATE kullanicilar SET bakiye = bakiye - ? WHERE tc_no = ?";
            pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, odenenMiktar);
            pstmt.setString(2, tcNo);
            pstmt.executeUpdate();
            
            conn.commit();
            
            JOptionPane.showMessageDialog(this, 
                String.format("Borç ödeme işlemi başarılı!\nÖdenen: %.2f TL\nKalan Borç: %.2f TL", 
                    odenenMiktar, mevcutBorc - odenenMiktar));
            bakiyeGuncelle();
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Borç ödeme hatası: " + ex.getMessage());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz miktar!");
        }
    }
    
    private void cikisYap() {
        this.dispose();
        new LoginFrame().setVisible(true);
    }
}
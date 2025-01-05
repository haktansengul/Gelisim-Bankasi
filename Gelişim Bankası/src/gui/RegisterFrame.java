package gui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Random;
import java.time.LocalDate;
import database.DatabaseConfig;

public class RegisterFrame extends JFrame {
    private JTextField tcField, adField, soyadField;
    private JPasswordField sifreField;
    
    public RegisterFrame() {
        setTitle("Yeni Kayıt");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        
        initComponents();
    }
    
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Başlık
        JLabel baslikLabel = new JLabel("YENİ KAYIT");
        baslikLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(baslikLabel, gbc);
        
        // Form alanları
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        add(new JLabel("TC No:"), gbc);
        
        tcField = new JTextField(15);
        gbc.gridx = 1;
        add(tcField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Ad:"), gbc);
        
        adField = new JTextField(15);
        gbc.gridx = 1;
        add(adField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Soyad:"), gbc);
        
        soyadField = new JTextField(15);
        gbc.gridx = 1;
        add(soyadField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("Şifre:"), gbc);
        
        sifreField = new JPasswordField(15);
        gbc.gridx = 1;
        add(sifreField, gbc);
        
        // Butonlar
        JPanel buttonPanel = new JPanel();
        JButton kaydetButton = new JButton("Kaydet");
        JButton iptalButton = new JButton("İptal");
        
        kaydetButton.addActionListener(e -> kaydet());
        iptalButton.addActionListener(e -> giriseDon());
        
        buttonPanel.add(kaydetButton);
        buttonPanel.add(iptalButton);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);
    }
    
    private String generateKartNo() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(rand.nextInt(10));
        }
        return sb.toString();
    }
    
    private String generateCVV() {
        Random rand = new Random();
        return String.format("%03d", rand.nextInt(1000));
    }
    
    private void kaydet() {
        String tcNo = tcField.getText();
        String ad = adField.getText();
        String soyad = soyadField.getText();
        String sifre = new String(sifreField.getPassword());
        
        if (tcNo.isEmpty() || ad.isEmpty() || soyad.isEmpty() || sifre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tüm alanları doldurunuz!");
            return;
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Kullanıcı kaydı
                String userQuery = "INSERT INTO kullanicilar (tc_no, ad, soyad, sifre) VALUES (?, ?, ?, ?)";
                PreparedStatement userStmt = conn.prepareStatement(userQuery);
                userStmt.setString(1, tcNo);
                userStmt.setString(2, ad);
                userStmt.setString(3, soyad);
                userStmt.setString(4, sifre);
                userStmt.executeUpdate();
                
                // Kredi kartı oluşturma
                String kartNo = generateKartNo();
                String cvv = generateCVV();
                LocalDate sonOdemeTarihi = LocalDate.now().plusMonths(1);
                
                String kartQuery = "INSERT INTO kredi_kartlari (kart_id, tc_no, cvv, son_odeme_tarihi) " +
                                 "VALUES (?, ?, ?, ?)";
                PreparedStatement kartStmt = conn.prepareStatement(kartQuery);
                kartStmt.setString(1, kartNo);
                kartStmt.setString(2, tcNo);
                kartStmt.setString(3, cvv);
                kartStmt.setDate(4, java.sql.Date.valueOf(sonOdemeTarihi));
                kartStmt.executeUpdate();
                
                conn.commit();
                JOptionPane.showMessageDialog(this, "Kayıt başarıyla tamamlandı!");
                giriseDon();
                
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Kayıt hatası: " + ex.getMessage());
        }
    }
    
    private void giriseDon() {
        this.dispose();
        new LoginFrame().setVisible(true);
    }
}
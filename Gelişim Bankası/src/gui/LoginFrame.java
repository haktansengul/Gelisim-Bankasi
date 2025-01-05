package gui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import database.DatabaseConfig;

public class LoginFrame extends JFrame {
    private JTextField tcField;
    private JPasswordField sifreField;
    
    public LoginFrame() {
        setTitle("Banka Giriş");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        
        initComponents();
    }
    
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Başlık
        JLabel baslikLabel = new JLabel("BANKA GİRİŞ");
        baslikLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(baslikLabel, gbc);
        
        // TC No
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        add(new JLabel("TC No:"), gbc);
        
        tcField = new JTextField(15);
        gbc.gridx = 1;
        add(tcField, gbc);
        
        // Şifre
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Şifre:"), gbc);
        
        sifreField = new JPasswordField(15);
        gbc.gridx = 1;
        add(sifreField, gbc);
        
        // Butonlar
        JPanel buttonPanel = new JPanel();
        JButton girisButton = new JButton("Giriş");
        JButton kayitButton = new JButton("Kayıt Ol");
        
        girisButton.addActionListener(e -> girisYap());
        kayitButton.addActionListener(e -> kayitEkraniAc());
        
        buttonPanel.add(girisButton);
        buttonPanel.add(kayitButton);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);
    }
    
    private void girisYap() {
        String tcNo = tcField.getText();
        String sifre = new String(sifreField.getPassword());
        
        if (tcNo.isEmpty() || sifre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tüm alanları doldurunuz!");
            return;
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String query = "SELECT * FROM kullanicilar WHERE tc_no = ? AND sifre = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, tcNo);
            pstmt.setString(2, sifre);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                this.dispose();
                new MainFrame(tcNo, rs.getString("ad") + " " + rs.getString("soyad")).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Hatalı TC No veya şifre!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Veritabanı hatası: " + ex.getMessage());
        }
    }
    
    private void kayitEkraniAc() {
        this.dispose();
        new RegisterFrame().setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
package com.custom.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RightPanel extends JPanel {
	public RightPanel(){
		super();
		setOpaque(false);//����ɫ��Ϊ͸������
		JLabel  label = new JLabel ();
		label.setText("PAD�Ѿ����ӵ���");
		
		label.setOpaque(false);//����ɫ��Ϊ͸������
        
		label.setLayout(null); 
		label.setPreferredSize(new   Dimension(420,100)); 
		label.setBounds(10, 50, 110, 50);
		label.setBorder(BorderFactory.createLineBorder(Color.black)); 
		
		add(label);
		final ImageIcon img = new ImageIcon(Main.imgPath+"update_left.png");
		JPanel panel = new JPanel() {
			public void paintComponent(Graphics g) {
				g.drawImage(img.getImage(), 0, 0, null);
				super.paintComponent(g);
			}
		};
        panel.setOpaque(false);//����ɫ��Ϊ͸������
        
        panel.setLayout(null); 
        panel.setPreferredSize(new   Dimension(420,100)); 
        panel.setBounds(10, 110, 110, 120);
        panel.setBorder(BorderFactory.createLineBorder(Color.black)); 
        
        add(panel);
	}

}
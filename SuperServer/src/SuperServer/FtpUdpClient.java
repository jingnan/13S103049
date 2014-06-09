package SuperServer;

import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class FtpUdpClient {
	public FtpUdpClient(){
		ClientFrame myClient = new ClientFrame();
	}
	
	
	
	class ClientFrame extends JFrame{
		public ClientFrame(){
			setTitle("UDP-FTP-Client");
			setSize(400,600);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			
			JPanel myPanel = new JPanel();
			myPanel.setSize(400, 600);
			myPanel.setLayout(null);
			
			ImageIcon img = new ImageIcon("ºóÍË.png"); 
			JButton backButton = new JButton(img);
			backButton.setBounds(20, 20, 30, 30);
			myPanel.add(backButton);
			
			JTextField URLField = new JTextField();
			URLField.setBounds(60, 20, 300, 30);
			myPanel.add(URLField);
			JTable table = new JTable();
			table.setBounds(20, 70, 340, 480);
			this.add(table);
			
			this.add(myPanel);
			setVisible(true);
		}
	}
	
	public static void main(String[] args){
		new FtpUdpClient();
	}
}

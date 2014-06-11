package SuperServer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class FtpUdpClient extends JFrame implements ActionListener{
	public FtpUdpClient(){
		initFrame();
		initServerInfo();
	}
	
	public void initFrame(){
		setTitle("UDP-FTP-Client");
		setSize(500,600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		myPanel = new JPanel();
		myPanel.setSize(500, 600);
		myPanel.setLayout(null);
		
		ImageIcon img = new ImageIcon("back.png"); 
		backButton = new JButton(img);
		backButton.setBounds(20, 20, 30, 30);
		myPanel.add(backButton);
		backButton.addActionListener(this);
		
		img = new ImageIcon("enter.png"); 
		enterButton = new JButton(img);
		enterButton.setBounds(55, 20, 30, 30);
		myPanel.add(enterButton);
		enterButton.addActionListener(this);
		
		urlField = new JTextField();
		urlField.setBounds(90, 20, 370, 30);
		myPanel.add(urlField);
		
		listModel = new DefaultListModel();
		fileList = new JList(listModel);
		fileList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				 if(fileList.getSelectedIndex() != -1) {
						 listClickAction();
				 }
			}
		});
		
		fileList.setBounds(20, 60, 440, 480);
		fileList.setVisible(false);
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		content = new JTextArea();
		content.setBounds(20, 60, 440, 480);
		content.setVisible(true);
		content.setLineWrap(true);
		content.setWrapStyleWord(true);
		content.setVisible(true);
		
		scrollPane = new JScrollPane(content);
		
		myPanel.add(fileList);
		myPanel.add(content);
		
		this.add(myPanel);
		setVisible(true);
		setResizable(false);
	}
	
	public void initServerInfo(){
		try {
			serverPort = 8021;
			dir = "/";
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void udpSend(String sendStr){
		try {
	        InetAddress addr = InetAddress.getByName(serverIP);
	        byte[] sendBuf;
	        sendBuf = sendStr.getBytes();
	        DatagramPacket sendPacket 
	            = new DatagramPacket(sendBuf , sendBuf.length , addr , serverPort);   
	        soc.send(sendPacket);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// �������ݰ�
	String udpReceive() {
		String recvStr = null;
		try {
			byte[] recvBuf = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvBuf,recvBuf.length);
			soc.receive(recvPacket);
			recvStr = new String(recvPacket.getData(), 0,recvPacket.getLength());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return recvStr;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == enterButton)
			enterAction();
		else if(e.getSource() == backButton)
			backAction();
	}
	
	public void enterAction(){
		//�����ʾ����
		listModel.clear();
		content.setText("");
		//��ȡ�û�����URL��Ϣ
		String url = urlField.getText();
		//��ȡ������IP
		serverIP = url.split("/")[2];
		if(first){
			//�����������ݰ��������¶˿ں�,��ʹ���¶˿ںŴ�������
			udpSend("new port");
			serverPort = Integer.valueOf(udpReceive());	
			first = false;
		}
		//������������
		int flag = parseInput(url);
		if(flag == 0){//���ʵ���Ŀ¼
			udpSend("CWD "+param);
			recvString = udpReceive();//������Ϣ250
			udpSend("TYPE A");
			recvString = udpReceive();//������Ϣ200
			udpSend("LIST");
			recvString = udpReceive();//������Ϣ150
			String filetemp = udpReceive();//�����ļ��б�
			//�����ļ��б�
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//������Ϣ226
		}else if(flag == 1){//���ʵ����ļ�
			udpSend("TYPE A");
			recvString = udpReceive();//������Ϣ200
			udpSend("PASV");
			recvString = udpReceive();//������Ϣ227
			udpSend("RETR "+param);
			recvString = udpReceive();//������Ϣ150
			String filecontent = udpReceive();//�����ļ�����
			content.setText(filecontent);
			recvString = udpReceive();//������Ϣ226
			content.setVisible(true);
			fileList.setVisible(false);	
		}	
	}
	
	public void backAction(){
		//��ȡ�û���ǰURL��Ϣ
		String url = urlField.getText();
		String[] urltemp = url.split("/");
		if(urltemp.length == 3){
			return;
		}else{
			//�����ʾ����
			listModel.clear();
			udpSend("CDUP");
			recvString = udpReceive();//������Ϣ200
			udpSend("LIST");
			recvString = udpReceive();//������Ϣ150
			String filetemp = udpReceive();//�����ļ��б�
			//�����ļ��б�
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//������Ϣ226
		}
		//����URL��Ϣ
		url = url.substring(0,url.length()-urltemp[urltemp.length-1].length()-1);
		urlField.setText(url);
	}
	
	//�б����¼������ʵ����תĿ¼����
	public void listClickAction(){
		//����������ݣ�������ļ���������ʾ���ݣ�������ǣ���ת
		param = "/"+fileList.getSelectedValue();
		int flag = param.indexOf(".");
		if(flag == -1){//���ʵ���Ŀ¼
			//�����ʾ����
			listModel.clear();
			udpSend("CWD "+param);
			recvString = udpReceive();//������Ϣ250
			udpSend("TYPE A");
			recvString = udpReceive();//������Ϣ200
			udpSend("LIST");
			recvString = udpReceive();//������Ϣ150
			String filetemp = udpReceive();//�����ļ��б�
			//�����ļ��б�
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//������Ϣ226
			//�ı�url��Ϣ
			String url = urlField.getText()+param;
			urlField.setText(url);
			
		}else{//���ʵ����ļ�
			//�����ʾ����
			content.setText("");
			udpSend("TYPE A");
			recvString = udpReceive();//������Ϣ200
			udpSend("PASV");
			recvString = udpReceive();//������Ϣ227
			udpSend("RETR "+param);
			recvString = udpReceive();//������Ϣ150
			String filecontent = udpReceive();//�����ļ�����
			content.setText(filecontent);
			recvString = udpReceive();//������Ϣ226
			content.setVisible(true);
			fileList.setVisible(false);	
			//�ı�url��Ϣ
			String url = urlField.getText()+param;
			urlField.setText(url);
		}	
	}
	
	//��URL����ȡ��������,������ʵ����ļ��У�����0��������ʵ����ļ�������1
	public int parseInput(String input){
		int flag = -1;
		param = "";
		String[] temp = input.split("/");
		if(temp.length == 3){//�û�ֻ��д��ip����������Ϊ"/"
			param = "/";
			flag = 0;
		}else{
			String last = temp[temp.length-1];
			if(last.contains(".")){//�û����ʵ�Ϊ�ļ�
				flag = 1;
			}else{
				flag = 0;
			}
			for(int i = 3;i<temp.length;i++){
				param = "/"+temp[i];
			}
		}
		return flag;
	}
	
	private JPanel myPanel;
	private JButton backButton;
	private JButton enterButton;
	private JTextField urlField;
	private JList fileList;
	private DefaultListModel listModel;
	private JTextArea content;
	private JScrollPane scrollPane;
	
	private String dir;  //�洢Ŀ¼
	private DatagramSocket soc;
	private String serverIP = "";//������IP
	private int serverPort;//�������˿�
	private String param = "";//�������
	private boolean first = true;
	private String recvString = "";
	
	public static void main(String[] args){
		new FtpUdpClient().setVisible(true);;
	}
}

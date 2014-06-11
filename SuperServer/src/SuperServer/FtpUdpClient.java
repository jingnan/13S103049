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
	
	// 接受数据包
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
		//清空显示区域
		listModel.clear();
		content.setText("");
		//获取用户输入URL信息
		String url = urlField.getText();
		//提取服务器IP
		serverIP = url.split("/")[2];
		if(first){
			//发送任意数据包，接受新端口号,并使用新端口号传输数据
			udpSend("new port");
			serverPort = Integer.valueOf(udpReceive());	
			first = false;
		}
		//解析访问内容
		int flag = parseInput(url);
		if(flag == 0){//访问的是目录
			udpSend("CWD "+param);
			recvString = udpReceive();//接受消息250
			udpSend("TYPE A");
			recvString = udpReceive();//接受消息200
			udpSend("LIST");
			recvString = udpReceive();//接受消息150
			String filetemp = udpReceive();//接受文件列表
			//绘制文件列表
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//接受消息226
		}else if(flag == 1){//访问的是文件
			udpSend("TYPE A");
			recvString = udpReceive();//接受消息200
			udpSend("PASV");
			recvString = udpReceive();//接受消息227
			udpSend("RETR "+param);
			recvString = udpReceive();//接受消息150
			String filecontent = udpReceive();//接受文件内容
			content.setText(filecontent);
			recvString = udpReceive();//接受消息226
			content.setVisible(true);
			fileList.setVisible(false);	
		}	
	}
	
	public void backAction(){
		//获取用户当前URL信息
		String url = urlField.getText();
		String[] urltemp = url.split("/");
		if(urltemp.length == 3){
			return;
		}else{
			//清空显示区域
			listModel.clear();
			udpSend("CDUP");
			recvString = udpReceive();//接受消息200
			udpSend("LIST");
			recvString = udpReceive();//接受消息150
			String filetemp = udpReceive();//接受文件列表
			//绘制文件列表
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//接受消息226
		}
		//更新URL信息
		url = url.substring(0,url.length()-urltemp[urltemp.length-1].length()-1);
		urlField.setText(url);
	}
	
	//列表单击事件，点击实现跳转目录功能
	public void listClickAction(){
		//解析点击内容，如果是文件名，则显示内容，如果不是，跳转
		param = "/"+fileList.getSelectedValue();
		int flag = param.indexOf(".");
		if(flag == -1){//访问的是目录
			//清空显示区域
			listModel.clear();
			udpSend("CWD "+param);
			recvString = udpReceive();//接受消息250
			udpSend("TYPE A");
			recvString = udpReceive();//接受消息200
			udpSend("LIST");
			recvString = udpReceive();//接受消息150
			String filetemp = udpReceive();//接受文件列表
			//绘制文件列表
			String[] files = filetemp.split(" ");
			for(int i = 0;i<files.length;i++){
				listModel.addElement(files[i]);	
			}
			fileList.setVisible(true);	
			content.setVisible(false);
			recvString = udpReceive();//接受消息226
			//改变url信息
			String url = urlField.getText()+param;
			urlField.setText(url);
			
		}else{//访问的是文件
			//清空显示区域
			content.setText("");
			udpSend("TYPE A");
			recvString = udpReceive();//接受消息200
			udpSend("PASV");
			recvString = udpReceive();//接受消息227
			udpSend("RETR "+param);
			recvString = udpReceive();//接受消息150
			String filecontent = udpReceive();//接受文件内容
			content.setText(filecontent);
			recvString = udpReceive();//接受消息226
			content.setVisible(true);
			fileList.setVisible(false);	
			//改变url信息
			String url = urlField.getText()+param;
			urlField.setText(url);
		}	
	}
	
	//从URL中提取访问内容,如果访问的是文件夹，返回0，如果访问的是文件，返回1
	public int parseInput(String input){
		int flag = -1;
		param = "";
		String[] temp = input.split("/");
		if(temp.length == 3){//用户只填写了ip，参数设置为"/"
			param = "/";
			flag = 0;
		}else{
			String last = temp[temp.length-1];
			if(last.contains(".")){//用户访问的为文件
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
	
	private String dir;  //存储目录
	private DatagramSocket soc;
	private String serverIP = "";//服务器IP
	private int serverPort;//服务器端口
	private String param = "";//传输参数
	private boolean first = true;
	private String recvString = "";
	
	public static void main(String[] args){
		new FtpUdpClient().setVisible(true);;
	}
}

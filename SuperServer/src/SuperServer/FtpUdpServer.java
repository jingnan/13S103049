package SuperServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import SuperServer.FtpTcpServer.FtpHandler;
import SuperServer.FtpTcpServer.UserInfo;

public class FtpUdpServer {
	public FtpUdpServer(){
		usersInfo.add(new UserInfo("admin", "admin", System.getProperty("user.dir").replace('\\', '/')+"/ftp"));
		
		// 监听8021号端口,8021口用于控制
		DatagramSocket  s;
		try {
			s = new DatagramSocket (8021);
			DatagramPacket packet = new DatagramPacket(new byte[1024], 1024); 
			while(true){	
				s.receive(packet);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	class FtpHandler extends Thread {

		// 构造方法
		public FtpHandler(Socket s, int i) {

		}
		
		public void run() {
			
		}
	}
	
	
	class UserInfo {
		String user;
		String password;
		String workDir;
		
		public UserInfo(String a, String b, String c)
		{
			user = a;
			password = b;
			workDir = c;
		}
	}
	
	public ArrayList<FtpHandler> users = new ArrayList<FtpHandler>();
	public static int counter = 0;
	public static String initDir = System.getProperty("user.dir").replace('\\', '/')+"/ftp";
	public static ArrayList<UserInfo> usersInfo = new ArrayList<UserInfo>();
	
	public static void main(String[] args){
		new FtpUdpServer();
	}
}

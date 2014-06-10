package SuperServer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class FtpUdpServer {
	public FtpUdpServer() {
		// ����8021�Ŷ˿�,8021�����ڿ���
		DatagramSocket s;
		try {
			s = new DatagramSocket(8021);
			DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
			while (true) {
				s.receive(packet);
				FtpHandler h = new FtpHandler(packet);
				h.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class FtpHandler extends Thread {
		DatagramSocket server;
		DatagramPacket initPacket;
		int port; // �ͻ��˵�port
		InetAddress clientAddr = null;
		int id;
		String cmd = ""; // ���ָ��(�ո�ǰ)
		String param = ""; // �ŵ�ǰָ��֮��Ĳ���(�ո��)
		String user;
		String remoteHost = " "; // �ͻ�IP
		int remotePort = 0; // �ͻ�TCP �˿ں�
		String dir = FtpTcpServer.initDir;// ��ǰĿ¼
		String rootdir = System.getProperty("user.dir").replace('\\', '/')+"/ftp"; // Ĭ�ϸ�Ŀ¼,��checkPASS������
//		int state = 0; // �û�״̬��ʶ��,��checkPASS������
		String reply; // ���ر���
		int type = 0; // �ļ�����(ascII �� bin)
		String requestfile = "";
		boolean isrest = false;
		InetAddress addr;
		int clientPort;

		// ���췽��
		public FtpHandler(DatagramPacket packet) {
			dir = FtpTcpServer.initDir;
			initPacket = packet;
			port = getPort();
			try {
				server = new DatagramSocket(port);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			addr = initPacket.getAddress();
			clientPort = initPacket.getPort();
			// �ȸ��߿ͻ����һ��˿���
			udpSend("" + port);
		}

		// �������ݰ�
		void udpSend(String sendStr) {
			try {

				byte[] sendBuf;
				sendBuf = sendStr.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendBuf,sendBuf.length, addr,clientPort);
				server.send(sendPacket);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
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
				server.receive(recvPacket);
				recvStr = new String(recvPacket.getData(), 0,recvPacket.getLength());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return recvStr;
		}

		public int getPort() {
			DatagramSocket s = null;// ΪUDP����е�Socket��,ֻ�����ж�UDPռ�õĶ˿�
			// ��������ֵ֮��Ķ˿ں�
			int MINPORT = 10000;
			int MAXPORT = 65000;
			for (; MINPORT < MAXPORT; MINPORT++) {
				try {
					s = new DatagramSocket(MINPORT);
					s.close();
					return MINPORT;
				} catch (IOException e) {
					continue;
				}
			}
			return -1;
		}

		public void run() {
			String str = "";
			int parseResult; // ��cmd һһ��Ӧ�ĺ�
			try {
				boolean finished = false;
				while (!finished) {
					str = udpReceive(); //
					if (str == null)
						finished = true; // ����while
					else {
						parseResult = parseInput(str); // ָ��ת��Ϊָ���
						System.out.println("ָ��:" + cmd + " ����:" + param);
						System.out.print("->");
							switch (parseResult)// ָ��ſ���,���������Ƿ�������еĹؼ�
							{
							case -1:
								errCMD(); // �﷨��
								break;
							case 4:
								finished = commandCDUP(); // ����һ��Ŀ¼
								break;
							case 5:
								finished = commandCWD(); // ��ָ����Ŀ¼
								break;
							case 6:
								finished = commandQUIT(); // �˳�
								break;
							case 7:
								finished = commandPORT(); // �ͻ���IP:��ַ+TCP �˿ں�
								break;
							case 8:
								finished = commandTYPE(); // �ļ���������(ascII �� bin)
								break;
							case 9:
								finished = commandRETR(); // �ӷ������л���ļ�
								break;
							case 11:
								finished = commandABOR(); // �رմ���������dataSocket
								break;
							case 14:
								finished = commandLIST(); // �ļ���Ŀ¼���б�
								break;
							case 15:
							case 16:
								finished = commandPWD(); // "��ǰĿ¼" ��Ϣ
								break;
							case 17:
								finished = commandNOOP(); // "������ȷ" ��Ϣ
								break;
							case 18:
								finished = commandPASV(); // "������ȷ" ��Ϣ
								break;
							}
					}
					System.out.println(reply);
					udpSend(reply);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// parseInput����
		int parseInput(String s) {
			int p = 0;
			int i = -1;
			p = s.indexOf(" ");
			if (p == -1) // ������޲�������(�޿ո�)
				cmd = s;
			else
				cmd = s.substring(0, p); // �в�������,���˲���

			if (p >= s.length() || p == -1)// ����޿ո�,��ո��ڶ����s������֮��
				param = "";
			else
				param = s.substring(p + 1, s.length());
			cmd = cmd.toUpperCase(); // ת���� String Ϊ��д

			if (cmd.equals("CDUP"))
				i = 4;
			if (cmd.equals("CWD"))
				i = 5;
			if (cmd.equals("QUIT"))
				i = 6;
			if (cmd.equals("PORT"))
				i = 7;
			if (cmd.equals("TYPE"))
				i = 8;
			if (cmd.equals("RETR"))
				i = 9;
			if (cmd.equals("ABOR"))
				i = 11;
			if (cmd.equals("LIST"))
				i = 14;
			if (cmd.equals("PWD"))
				i = 15;
			if (cmd.equals("NOOP"))
				i = 16;
			if (cmd.equals("XPWD"))
				i = 17;
			if (cmd.equals("PASV"))
				i = 18;
			return i;
		}

		// validatePath����
		// �ж�·��������,���� int
		int validatePath(String s) {
			File f = new File(s); // ���·��
			if (f.exists() && !f.isDirectory()) {
				String s1 = s.toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 1; // �ļ������Ҳ���·��,����rootdir ��ʼ
				else
					return 0; // �ļ������Ҳ���·��,����rootdir ��ʼ
			}
			f = new File(deleTail(dir) + s);// ����·��
			if (f.exists() && !f.isDirectory()) {
				String s1 = (deleTail(dir) + s).toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 2; // �ļ������Ҳ���·��,����rootdir ��ʼ
				else
					return 0; // �ļ������Ҳ���·��,����rootdir ��ʼ
			}
			return 0; // �������
		}

		void errCMD() {
			reply = "500 �﷨����";
		}

		// ������������ȴ���������
		boolean commandPASV() {
			try {
				InetAddress addr = InetAddress.getLocalHost();
				String ip = addr.getHostAddress().toString();// ��ñ���IP,��������ip
				String highport = "" + port / 256;
				String lowport = "" + port % 256;
				reply = "227 ������ȷ  (" + ip + "," + highport + "," + lowport+ ")";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}// commandPASV() end

		boolean commandCDUP()// ����һ��Ŀ¼
		{
			File f = new File(dir);
			if (f.getParent() != null && (!dir.equals(rootdir)))// �и�·�� && ���Ǹ�·��
			{
				dir = f.getParent().replace('\\', '/');
				reply = "200 ������ȷ";
			} else {
				reply = "550 ��ǰĿ¼�޸�·��";
			}

			return false;
		}// commandCDUP() end

		boolean commandCWD()// CWD (CHANGE WORKING DIRECTORY)
		{
			// ������ı乤��Ŀ¼���û�ָ����Ŀ¼
			File f = new File(param);
			String s = "";// ��ǰĿ¼
			String s1 = "";
			if (dir.endsWith("/"))
				s = dir.substring(0,dir.length()-1);
			else
				s = dir;
			File f1 = new File(s + param);// ��תĿ¼

			if (f.isDirectory() && f.exists()) {
				if (param.equals("..") || param.equals("../")) {
					if (dir.compareToIgnoreCase(rootdir) == 0) {
						reply = "550 ��·��������";
						// return false;
					} else {
						s1 = new File(dir).getParent();
						if (s1 != null) {
							dir = s1;
							reply = "250 ������ļ��������, ��ǰĿ¼��Ϊ: " + dir;
						} else
							reply = "550 ��·��������";
					}
				} else if (param.equals(".") || param.equals("./")) {

				} else {
					dir = rootdir + param;
					// dir = param;
					reply = "250 ������ļ��������, ����·����Ϊ " + dir;
				}
			} else if (f1.isDirectory() && f1.exists()) {
				dir = s + param;
				reply = "250 ������ļ��������, ����·����Ϊ " + dir;
			} else
				reply = "501 �����﷨����";
			return false;
		} // commandCDW() end

		boolean commandQUIT() {
			reply = "221 ����ر�����";
			return true;
		}// commandQuit() end

		/*
		 * ʹ�ø�����ʱ���ͻ��˱��뷢�Ϳͻ������ڽ������ݵ�32λIP ��ַ��16λ ��TCP �˿ںš�
		 * ��Щ��Ϣ��8λΪһ�飬ʹ��ʮ���ƴ��䣬�м��ö��Ÿ�����
		 */
		boolean commandPORT() {
			int p1 = 0;
			int p2 = 0;
			int[] a = new int[6];// ���ip+tcp
			int i = 0; //
			try {
				while ((p2 = param.indexOf(",", p1)) != -1)// ǰ5λ
				{
					a[i] = Integer.parseInt(param.substring(p1, p2));
					p2 = p2 + 1;
					p1 = p2;
					i++;
				}
				a[i] = Integer.parseInt(param.substring(p1, param.length()));// ���һλ
			} catch (NumberFormatException e) {
				reply = "501 �����﷨����";
				return false;
			}

			remoteHost = a[0] + "." + a[1] + "." + a[2] + "." + a[3];
			remotePort = a[4] * 256 + a[5];
			reply = "200 ������ȷ";
			return false;
		}// commandPort() end

		// LIST ����������ͻ��˷��ط������й���Ŀ¼�µ�Ŀ¼�ṹ�������ļ���Ŀ¼���б�
		boolean commandLIST()// �ļ���Ŀ¼���б�
		{
			try {
				udpSend("150 �ļ�״̬����,ls�� ASCII ��ʽ����");
				File f = new File(dir);
				String[] dirStructure = f.list();// ָ��·���е��ļ�������,��������ǰ·����·��
				String flieList = "";//�ļ��б��ÿո����
				for (int i = 0; i < dirStructure.length; i++) {
					flieList = flieList + dirStructure[i]+" ";
				}
				udpSend(flieList);
				reply = "226 �����������ӽ���";
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 Requested action aborted: local error in processing";
				return false;
			}

			return false;
		}// commandLIST() end

		boolean commandTYPE() // TYPE �������������������
		{
			if (param.equals("A")) {
				type = FtpState.FTYPE_ASCII;// 0
				reply = "200 ������ȷ ,ת ASCII ģʽ";
			} else if (param.equals("I")) {
				type = FtpState.FTYPE_IMAGE;// 1
				reply = "200 ������ȷ ת BINARY ģʽ";
			} else
				reply = "504 �����ִ�����ֲ���";

			return false;
		}

		// connamdRETR ����
		// �ӷ������л���ļ�
		boolean commandRETR() {
			requestfile = param;
			dir = deleTail(dir) + param;
			File f = new File(requestfile);
			if (!f.exists()) {
				f = new File(dir);
				if (!f.exists()) {
					reply = "550 �ļ�������";
					return false;
				}
				requestfile = dir;
			}

			if (!param.contains(".")) {
				isrest = true;
			}
			if (isrest) {
				
			} else {
				if (type == FtpState.FTYPE_IMAGE) // bin
				{
					try {
						udpSend("150 �ļ�״̬����,�Զ����η�ʽ���ļ�:  "
								+ requestfile);
						BufferedInputStream fin = new BufferedInputStream(new FileInputStream(requestfile));
						String send="";
						byte[] buf = new byte[1024]; // Ŀ�껺����
						int l = 0;
						while ((l = fin.read(buf, 0, 1024)) != -1) // ������δ����
						{
							send = send + new String(buf,0,1);
						}
						udpSend(send);
						fin.close();
						reply = "226 �����������ӽ���";

					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 ����ʧ��: ���������";
						return false;
					}

				}
				if (type == FtpState.FTYPE_ASCII)// ascII
				{
					try {
						udpSend("150 Opening ASCII mode data connection for "+ requestfile);
						BufferedReader fin = new BufferedReader(new FileReader(requestfile));
						String s = "";
						String send = "";
						while ((s = fin.readLine()) != null) {
							send = send + s;
						}
						udpSend(send);
						fin.close();
						reply = "226 �����������ӽ���";
					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 ����ʧ��: ���������";
						return false;
					}
				}
			}
			return false;

		}

		boolean commandPWD() {
			reply = "257 " + dir + " �ǵ�ǰĿ¼.";
			return false;
		}

		boolean commandNOOP() {
			reply = "200 ������ȷ.";
			return false;
		}

		// ǿ��dataSocket ��
		boolean commandABOR() {
			try {
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 ����ʧ��: ���������";
				return false;
			}
			reply = "421 ���񲻿���, �ر����ݴ�������";
			return false;
		}

		String addTail(String s) {
			if (!s.endsWith("/"))
				s = s + "/";
			return s;
		}

		String deleTail(String s) {
			if (s.endsWith("/")) {
				s = s.substring(0, s.length() - 1);
			}
			return s;
		}
	}

	class FtpState {
		final static int FTYPE_ASCII = 0;
		final static int FTYPE_IMAGE = 1;
		final static int FMODE_STREAM = 0;
		final static int FMODE_COMPRESSED = 1;
		final static int FSTRU_FILE = 0;
		final static int FSTRU_PAGE = 1;
	}

	public ArrayList<FtpHandler> users = new ArrayList<FtpHandler>();
	public static int counter = 0;
	public static String initDir = System.getProperty("user.dir").replace('\\','/')+ "/ftp";

	public static void main(String[] args) {
		new FtpUdpServer();
	}
}

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
		// 监听8021号端口,8021口用于控制
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
	
	public FtpUdpServer(boolean test){
		testHandler = new FtpHandler();
	}
	
	public FtpHandler testHandler;

	class FtpHandler extends Thread {
		DatagramSocket server;
		DatagramPacket initPacket;
		int port; // 客户端的port
		InetAddress clientAddr = null;
		int id;
		String cmd = ""; // 存放指令(空格前)
		String param = ""; // 放当前指令之后的参数(空格后)
		String user;
		String remoteHost = " "; // 客户IP
		int remotePort = 0; // 客户TCP 端口号
		String dir = FtpTcpServer.initDir;// 当前目录
		String rootdir = System.getProperty("user.dir").replace('\\', '/')+"/ftp"; // 默认根目录,在checkPASS中设置
//		int state = 0; // 用户状态标识符,在checkPASS中设置
		String reply; // 返回报告
		int type = 0; // 文件类型(ascII 或 bin)
		String requestfile = "";
		boolean isrest = false;
		InetAddress addr;
		int clientPort;

		// 构造方法
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
			// 先告诉客户端我换端口了
			udpSend("" + port);
		}
		
		public FtpHandler(){
			
		}

		// 发送数据包
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

		// 接受数据包
		public String udpReceive() {
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
			DatagramSocket s = null;// 为UDP编程中的Socket类,只可以判断UDP占用的端口
			// 测试两个值之间的端口号
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
			int parseResult; // 与cmd 一一对应的号
			try {
				boolean finished = false;
				while (!finished) {
					str = udpReceive(); //
					if (str == null)
						finished = true; // 跳出while
					else {
						parseResult = parseInput(str); // 指令转化为指令号
						System.out.println("指令:" + cmd + " 参数:" + param);
						System.out.print("->");
							switch (parseResult)// 指令号开关,决定程序是否继续运行的关键
							{
							case -1:
								errCMD(); // 语法错
								break;
							case 4:
								finished = commandCDUP(); // 到上一层目录
								break;
							case 5:
								finished = commandCWD(); // 到指定的目录
								break;
							case 6:
								finished = commandQUIT(); // 退出
								break;
							case 7:
								finished = commandPORT(); // 客户端IP:地址+TCP 端口号
								break;
							case 8:
								finished = commandTYPE(); // 文件类型设置(ascII 或 bin)
								break;
							case 9:
								finished = commandRETR(); // 从服务器中获得文件
								break;
							case 11:
								finished = commandABOR(); // 关闭传输用连接dataSocket
								break;
							case 14:
								finished = commandLIST(); // 文件和目录的列表
								break;
							case 15:
							case 16:
								finished = commandPWD(); // "当前目录" 信息
								break;
							case 17:
								finished = commandNOOP(); // "命令正确" 信息
								break;
							case 18:
								finished = commandPASV(); // "命令正确" 信息
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

		// parseInput方法
		public int parseInput(String s) {
			int p = 0;
			int i = -1;
			p = s.indexOf(" ");
			if (p == -1) // 如果是无参数命令(无空格)
				cmd = s;
			else
				cmd = s.substring(0, p); // 有参数命令,过滤参数

			if (p >= s.length() || p == -1)// 如果无空格,或空格在读入的s串最后或之外
				param = "";
			else
				param = s.substring(p + 1, s.length());
			cmd = cmd.toUpperCase(); // 转换该 String 为大写

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

		// validatePath方法
		// 判断路径的属性,返回 int
		public int validatePath(String s) {
			File f = new File(s); // 相对路径
			if (f.exists() && !f.isDirectory()) {
				String s1 = s.toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 1; // 文件存在且不是路径,且以rootdir 开始
				else
					return 0; // 文件存在且不是路径,不以rootdir 开始
			}
			f = new File(deleTail(dir) + s);// 绝对路径
			if (f.exists() && !f.isDirectory()) {
				String s1 = (deleTail(dir) + s).toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 2; // 文件存在且不是路径,且以rootdir 开始
				else
					return 0; // 文件存在且不是路径,不以rootdir 开始
			}
			return 0; // 其他情况
		}

		void errCMD() {
			reply = "500 语法错误";
		}

		// 请求服务器，等待数据连接
		boolean commandPASV() {
			try {
				InetAddress addr = InetAddress.getLocalHost();
				String ip = addr.getHostAddress().toString();// 获得本机IP,即服务器ip
				String highport = "" + port / 256;
				String lowport = "" + port % 256;
				reply = "227 命令正确  (" + ip + "," + highport + "," + lowport+ ")";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}// commandPASV() end

		boolean commandCDUP()// 到上一层目录
		{
			File f = new File(dir);
			if (f.getParent() != null && (!dir.equals(rootdir)))// 有父路径 && 不是根路径
			{
				dir = f.getParent().replace('\\', '/');
				reply = "200 命令正确";
			} else {
				reply = "550 当前目录无父路径";
			}

			return false;
		}// commandCDUP() end

		boolean commandCWD()// CWD (CHANGE WORKING DIRECTORY)
		{
			// 该命令改变工作目录到用户指定的目录
			File f = new File(param);
			String s = "";// 当前目录
			String s1 = "";
			if (dir.endsWith("/"))
				s = dir.substring(0,dir.length()-1);
			else
				s = dir;
			File f1 = new File(s + param);// 跳转目录

			if (f.isDirectory() && f.exists()) {
				if (param.equals("..") || param.equals("../")) {
					if (dir.compareToIgnoreCase(rootdir) == 0) {
						reply = "550 此路径不存在";
						// return false;
					} else {
						s1 = new File(dir).getParent();
						if (s1 != null) {
							dir = s1;
							reply = "250 请求的文件处理结束, 当前目录变为: " + dir;
						} else
							reply = "550 此路径不存在";
					}
				} else if (param.equals(".") || param.equals("./")) {

				} else {
					dir = rootdir + param;
					// dir = param;
					reply = "250 请求的文件处理结束, 工作路径变为 " + dir;
				}
			} else if (f1.isDirectory() && f1.exists()) {
				dir = s + param;
				reply = "250 请求的文件处理结束, 工作路径变为 " + dir;
			} else
				reply = "501 参数语法错误";
			return false;
		} // commandCDW() end

		boolean commandQUIT() {
			reply = "221 服务关闭连接";
			return true;
		}// commandQuit() end

		//使用该命令时，客户端必须发送客户端用于接收数据的32位IP地址和16位 的TCP端口号。这些信息以8位为一组，使用十进制传输，中间用逗号隔开。
		boolean commandPORT() {
			int p1 = 0;
			int p2 = 0;
			int[] a = new int[6];// 存放ip+tcp
			int i = 0; //
			try {
				while ((p2 = param.indexOf(",", p1)) != -1)// 前5位
				{
					a[i] = Integer.parseInt(param.substring(p1, p2));
					p2 = p2 + 1;
					p1 = p2;
					i++;
				}
				a[i] = Integer.parseInt(param.substring(p1, param.length()));// 最后一位
			} catch (NumberFormatException e) {
				reply = "501 参数语法错误";
				return false;
			}

			remoteHost = a[0] + "." + a[1] + "." + a[2] + "." + a[3];
			remotePort = a[4] * 256 + a[5];
			reply = "200 命令正确";
			return false;
		}// commandPort() end

		// LIST 命令用于向客户端返回服务器中工作目录下的目录结构，包括文件和目录的列表。
		boolean commandLIST()// 文件和目录的列表
		{
			try {
				udpSend("150 文件状态正常,ls以 ASCII 方式操作");
				File f = new File(dir);
				String[] dirStructure = f.list();// 指定路径中的文件名数组,不包括当前路径或父路径
				String flieList = "";//文件列表，用空格隔开
				for (int i = 0; i < dirStructure.length; i++) {
					flieList = flieList + dirStructure[i]+" ";
				}
				udpSend(flieList);
				reply = "226 传输数据连接结束";
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 Requested action aborted: local error in processing";
				return false;
			}

			return false;
		}// commandLIST() end

		boolean commandTYPE() // TYPE 命令用来完成类型设置
		{
			if (param.equals("A")) {
				type = FtpState.FTYPE_ASCII;// 0
				reply = "200 命令正确 ,转 ASCII 模式";
			} else if (param.equals("I")) {
				type = FtpState.FTYPE_IMAGE;// 1
				reply = "200 命令正确 转 BINARY 模式";
			} else
				reply = "504 命令不能执行这种参数";

			return false;
		}

		// connamdRETR 方法
		// 从服务器中获得文件
		boolean commandRETR() {
			requestfile = param;
			dir = deleTail(dir) + param;
			File f = new File(requestfile);
			if (!f.exists()) {
				f = new File(dir);
				if (!f.exists()) {
					reply = "550 文件不存在";
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
						udpSend("150 文件状态正常,以二进治方式打开文件:  "
								+ requestfile);
						BufferedInputStream fin = new BufferedInputStream(new FileInputStream(requestfile));
						String send="";
						byte[] buf = new byte[1024]; // 目标缓冲区
						int l = 0;
						while ((l = fin.read(buf, 0, 1024)) != -1) // 缓冲区未读满
						{
							send = send + new String(buf,0,1)+"	";
						}
						udpSend(send);
						fin.close();
						reply = "226 传输数据连接结束";

					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 请求失败: 传输出故障";
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
						reply = "226 传输数据连接结束";
					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 请求失败: 传输出故障";
						return false;
					}
				}
			}
			return false;
		}

		boolean commandPWD() {
			reply = "257 " + dir + " 是当前目录.";
			return false;
		}

		boolean commandNOOP() {
			reply = "200 命令正确.";
			return false;
		}

		// 强关dataSocket 流
		boolean commandABOR() {
			try {
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 请求失败: 传输出故障";
				return false;
			}
			reply = "421 服务不可用, 关闭数据传送连接";
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

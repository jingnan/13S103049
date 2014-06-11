package SuperServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FtpTcpServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new FtpTcpServer();
	}

	public FtpTcpServer() {
		
		usersInfo.add(new UserInfo("jingnan", "jingnan", System.getProperty("user.dir").replace('\\', '/')+"/ftp"));
		
		// 监听21号端口,21口用于控制
		ServerSocket s;
		try {
			s = new ServerSocket(21);

			int i = 0;
			while (true) {
				// 接受客户端请求
				Socket incoming = s.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						incoming.getInputStream()));
				PrintWriter out = new PrintWriter(incoming.getOutputStream(),
						true);// 文本文本输出流
				out.println("220 准备为您服务" + ",你是当前第  " + counter + " 个登陆者!");// 命令正确的提示

				// 创建服务线程
				FtpHandler h = new FtpHandler(incoming, i);
				h.start();
				users.add(h); // 将此用户线程加入到这个 ArrayList 中
				counter++;
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<FtpHandler> users = new ArrayList<FtpHandler>();
	public static int counter = 0;
	public static String initDir = System.getProperty("user.dir").replace('\\', '/')+"/ftp";
	public static ArrayList<UserInfo> usersInfo = new ArrayList<UserInfo>();

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

	class FtpHandler extends Thread {
		Socket ctrlSocket; // 用于控制的套接字
		Socket dataSocket; // 用于传输的套接字
		ServerSocket randDataSocket;//随机分配的用于数据传输的套接字
		int id;
		String cmd = ""; // 存放指令(空格前)
		String param = ""; // 放当前指令之后的参数(空格后)
		String user;
		String remoteHost = " "; // 客户IP
		int remotePort = 0; // 客户TCP 端口号
		String dir = FtpTcpServer.initDir;// 当前目录
		String rootdir = System.getProperty("user.dir").replace('\\', '/')+"/ftp"; // 默认根目录,在checkPASS中设置
		int state = 0; // 用户状态标识符,在checkPASS中设置
		String reply; // 返回报告
		PrintWriter ctrlOutput;
		int type = 0; // 文件类型(ascII 或 bin)
		String requestfile = "";
		boolean isrest = false;
		

		// FtpHandler方法
		// 构造方法
		public FtpHandler(Socket s, int i) {
			ctrlSocket = s;
			id = i;
			dir = FtpTcpServer.initDir;
		}

		// run 方法
		public void run() {
			String str = "";
			int parseResult; // 与cmd 一一对应的号

			try {
				BufferedReader ctrlInput = new BufferedReader(
						new InputStreamReader(ctrlSocket.getInputStream()));
				ctrlOutput = new PrintWriter(ctrlSocket.getOutputStream(), true);
				state = FtpState.FS_WAIT_LOGIN; // 0
				boolean finished = false;
				while (!finished) {
					str = ctrlInput.readLine(); //
					if (str == null)
						finished = true; // 跳出while
					else {
						parseResult = parseInput(str); // 指令转化为指令号
						System.out.println("指令:" + cmd + " 参数:" + param);
						System.out.print("->");
						switch (state) // 用户状态开关
						{
						case FtpState.FS_WAIT_LOGIN:
							finished = commandUSER();
							break;
						case FtpState.FS_WAIT_PASS:
							finished = commandPASS();
							break;
						case FtpState.FS_LOGIN: {
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
							break;

						}
					}
					System.out.println(reply);
					ctrlOutput.println(reply);
					ctrlOutput.flush();//

				}
				ctrlSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// parseInput方法
		int parseInput(String s) {
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
		int validatePath(String s) {
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

		boolean checkPASS(String s) // 检查密码是否正确,从文件中找
		{
			for (int i = 0; i < FtpTcpServer.usersInfo.size(); i++) {
				if (((UserInfo) FtpTcpServer.usersInfo.get(i)).user.equals(user)
						&& ((UserInfo) FtpTcpServer.usersInfo.get(i)).password
								.equals(s)) {
					rootdir = ((UserInfo) FtpTcpServer.usersInfo.get(i)).workDir;
					dir = ((UserInfo) FtpTcpServer.usersInfo.get(i)).workDir;
					return true;
				}
			}
			return false;
		}

		// commandUSER方法
		// 用户名是否正确
		boolean commandUSER() {
			if (cmd.equals("USER")) {
				reply = "331 用户名正确,需要口令";
				user = param;
				state = FtpState.FS_WAIT_PASS;
				return false;
			} else {
				reply = "501 参数语法错误,用户名不匹配";
				return true;
			}

		}

		// commandPASS 方法
		// 密码是否正确
		boolean commandPASS() {
			if (cmd.equals("PASS")) {
				if (checkPASS(param)) {
					reply = "230 用户登录了";
					state = FtpState.FS_LOGIN;
					System.out.println("新消息: 用户: " + user + " 来自于: "
							+ remoteHost + "登录了");
					System.out.print("->");
					return false;
				} else {
					reply = "530 没有登录";
					return true;
				}
			} else {
				reply = "501 参数语法错误,密码不匹配";
				return true;
			}

		}

		void errCMD() {
			reply = "500 语法错误";
		}
		
		//请求服务器，等待数据连接
		boolean commandPASV(){
			try {
				randDataSocket = new ServerSocket(0);
				InetAddress addr = InetAddress.getLocalHost();
				String ip=addr.getHostAddress().toString();//获得本机IP,即服务器ip
				ip = ip.replace('.', ',');
				String highport = ""+randDataSocket.getLocalPort()/256;
				String lowport = ""+randDataSocket.getLocalPort()%256;
				reply = "227 命令正确  ("+ip+","+highport+","+lowport+")";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}//commandPASV() end

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
			String s = "";//当前目录
			String s1 = "";
			if (dir.endsWith("/"))
				s = dir.substring(0,dir.length()-1);
			else
				s = dir;
			File f1 = new File(s + param);//跳转目录

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
					dir = rootdir+param;
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

		/*
		 * 使用该命令时，客户端必须发送客户端用于接收数据的32位IP 地址和16位 的TCP 端口号。
		 * 这些信息以8位为一组，使用十进制传输，中间用逗号隔开。
		 */
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

		//LIST 命令用于向客户端返回服务器中工作目录下的目录结构，包括文件和目录的列表。
		boolean commandLIST()// 文件和目录的列表
		{
			try {
//				dataSocket = new Socket(remoteHost, remotePort,InetAddress.getLocalHost(), 20);
				dataSocket = randDataSocket.accept();
				PrintWriter dout = new PrintWriter(
						dataSocket.getOutputStream(), true);
				if (param.equals("") || param.equals("LIST")) {
					ctrlOutput.println("150 文件状态正常,ls以 ASCII 方式操作");
					File f = new File(dir);
					dout.println("目录");
					String[] dirStructure = f.list();// 指定路径中的文件名数组,不包括当前路径或父路径
					String fileType;
					for (int i = 0; i < dirStructure.length; i++) {
						if (dirStructure[i].indexOf(".") != -1) {
							fileType = "- "; //
						} else {
							fileType = "d "; //
						}
						dout.println(dirStructure[i]);// (fileType+dirStructure[i]);
					}
				}
				dout.close();
				dataSocket.close();
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
			File f = new File(requestfile);
			if (!f.exists()) {
				f = new File(deleTail(dir) + param);
				if (!f.exists()) {
					reply = "550 文件不存在";
					return false;
				}
				requestfile = deleTail(dir) + param;
			}
			
			if(!param.contains(".")){
				isrest = true;
			}
			if (isrest) {
			} else {
				if (type == FtpState.FTYPE_IMAGE) // bin
				{
					try {
						ctrlOutput.println("150 文件状态正常,以二进治方式打开文件:  "
								+ requestfile);
						dataSocket = randDataSocket.accept();
						BufferedInputStream fin = new BufferedInputStream(
								new FileInputStream(requestfile));
						PrintStream dataOutput = new PrintStream(
								dataSocket.getOutputStream(), true);
						byte[] buf = new byte[1024]; // 目标缓冲区
						int l = 0;
						while ((l = fin.read(buf, 0, 1024)) != -1) // 缓冲区未读满
						{
							dataOutput.write(buf, 0, l); // 写入套接字
						}
						fin.close();
						dataOutput.close();
						dataSocket.close();
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
						ctrlOutput.println("150 Opening ASCII mode data connection for "+ requestfile);
						dataSocket = randDataSocket.accept();
						BufferedReader fin = new BufferedReader(new FileReader(
								requestfile));
						PrintWriter dataOutput = new PrintWriter(
								dataSocket.getOutputStream(), true);
						String s;
						while ((s = fin.readLine()) != null) {
							dataOutput.println(s); 
						}
						fin.close();
						dataOutput.close();
						dataSocket.close();
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
				dataSocket.close();
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
		
		String deleTail(String s){
			if(s.endsWith("/")){
				s=s.substring(0,s.length()-1);
			}
			return s;
		}

	}

	class FtpState {
		final static int FS_WAIT_LOGIN = 0; // 等待输入用户名状态
		final static int FS_WAIT_PASS = 1; // 等待输入密码状态
		final static int FS_LOGIN = 2; // 已经登陆状态

		final static int FTYPE_ASCII = 0;
		final static int FTYPE_IMAGE = 1;
		final static int FMODE_STREAM = 0;
		final static int FMODE_COMPRESSED = 1;
		final static int FSTRU_FILE = 0;
		final static int FSTRU_PAGE = 1;
	}

}

package SuperServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
	//指定80端口启动http服务器，每收到一个请求就创建一个服务器响应线程
	public void start() throws IOException {
		ServerSocket server = new ServerSocket(80);
		System.out.println("server start at 80 port...........");
		while (true) {
			Socket soc = server.accept();
			ServerThread s = new ServerThread(soc);
			s.start();
		}
	}

	//服务器响应线程
	class ServerThread extends Thread {
		Socket soc;
		public ServerThread(Socket soc) {
			this.soc = soc;
		}
		
		//读取文件内容，转化为byte数组
		public  byte[] getFileByte(String filename) throws IOException
		{
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			File file=new File(filename);
			FileInputStream fis=new FileInputStream(file);
			byte[] b=new byte[1024];
			int read = 0;
			while((read=fis.read(b))!=-1)
			{
				baos.write(b,0,read);
			}
			fis.close();
			baos.close();
			return baos.toByteArray();
		}

		//分析并规范化url，如请求 "/"要规范成"/test.html",如后缀为".htm"，转化成".html"
		private String getQueryResource(String queryurl)
		{
			String queryresource=null;
			int index=queryurl.indexOf('?');
			if(index!=-1){
				queryresource=queryurl.substring(0,queryurl.indexOf('?'));
			}else{
				queryresource=queryurl;
			}		
			if(queryresource.endsWith("/")){
				queryresource=queryresource+"test.html";
			}else if(queryresource.endsWith(".htm")){
				queryresource=queryresource+"l";
			}
			return queryresource;
		}	
	
		//根据用户请求的资源类型，设定http响应头的信息，判断用户请求文件类型
		private String getHead(String queryresource)
		{
			String filename="";
			int index=queryresource.lastIndexOf("/");
			filename=queryresource.substring(index+1);
			String filetype=filename.substring(filename.indexOf(".")+1);
			if(filetype.equals("html"))
			{
				return "HTTP/1.0200OK\n"+"Content-Type:text/html\n" + "Server:myserver\n" + "\n";
			}
			else if(filetype.equals("jpg")||filetype.equals("gif")||filetype.equals("png"))
			{
				return "HTTP/1.0200OK\n"+"Content-Type:image/jpeg\n" + "Server:myserver\n" + "\n";
			}
			else return null;
		}

		public void run() {
			try {
				InputStream is = soc.getInputStream();
				OutputStream os = soc.getOutputStream();
				soc.setSoTimeout(1000); //设定超时时间
				int readint;
				char c;
				byte[] buf = new byte[1024];
				byte[] data = null;
				int state = 0;
				String method = "";//传输方式，GET或POST
				String queryurl = ""; //请求URL信息
				String queryresource = "";
				String head = "";
				boolean start = false;
				while (true) {
					readint = is.read();
					c = (char) readint;
					boolean space=Character.isWhitespace(readint);
					if(!space){
						start = true;
						method+=c;
					}
					if(start && space){
						break;
					}
				}
				start = false;
				while(true){
					readint = is.read();
					c = (char) readint;
					boolean space=Character.isWhitespace(readint);
					if(!space){
						start = true;
						queryurl+=c;
					}
					if(start && space){
						break;
					}
				}

				queryresource=getQueryResource(queryurl);
				head=getHead(queryresource);

				while (true) {
					data = getFileByte("http"+queryresource);
					if (data != null) {
						os.write(data);
						os.close();
						break;
					}
				}
			} catch (java.net.SocketTimeoutException e){
				System.out.println("time out");
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	public static void main(String[] args) throws IOException {
		new HttpServer().start();
	}
}

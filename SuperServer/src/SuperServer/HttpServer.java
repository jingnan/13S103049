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
	//ָ��80�˿�����http��������ÿ�յ�һ������ʹ���һ����������Ӧ�߳�
	public void start() throws IOException {
		ServerSocket server = new ServerSocket(80);
		System.out.println("server start at 80 port...........");
		while (true) {
			Socket soc = server.accept();
			ServerThread s = new ServerThread(soc);
			s.start();
		}
	}

	//��������Ӧ�߳�
	class ServerThread extends Thread {
		Socket soc;
		public ServerThread(Socket soc) {
			this.soc = soc;
		}
		
		//��ȡ�ļ����ݣ�ת��Ϊbyte����
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

		//�������淶��url�������� "/"Ҫ�淶��"/test.html",���׺Ϊ".htm"��ת����".html"
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
	
		//�����û��������Դ���ͣ��趨http��Ӧͷ����Ϣ���ж��û������ļ�����
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
				soc.setSoTimeout(1000); //�趨��ʱʱ��
				int readint;
				char c;
				byte[] buf = new byte[1024];
				byte[] data = null;
				int state = 0;
				String method = "";//���䷽ʽ��GET��POST
				String queryurl = ""; //����URL��Ϣ
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

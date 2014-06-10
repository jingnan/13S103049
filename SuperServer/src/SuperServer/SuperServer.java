package SuperServer;

import java.io.IOException;

public class SuperServer {
	public static void main(String[] args)
	{
		new Thread(){
			public void run()
			{
				new FtpTcpServer();
			}
		}.start();
		new Thread(){
			public void run()
			{
				new FtpUdpServer();
			}
		}.start();
		new Thread(){
			public void run()
			{
				new HttpServer();
			}
		}.start();
	}
}

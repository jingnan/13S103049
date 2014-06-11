package SuperServer;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.Test;

public class FtpUdpServerTest {

	private FtpUdpServer.FtpHandler test = new FtpUdpServer(true).testHandler;

	@Test
	public void testGetPort() {
		for (int i = 0; i <= 20; i++) {
			int port = test.getPort();
			if (port != -1) {
				try {
					DatagramSocket soc = new DatagramSocket(port);
				} catch (IOException e) {
					fail("i = " + i + "port: " + port + " open error!");
				}
			} else {
				break;
			}
		}
	}

	@Test
	public void testParseInput() {
		assertEquals(test.parseInput("CWD"), 5);
		assertEquals(test.parseInput("cwd"), 5);
		assertEquals(test.parseInput("CWD aabb"), 5);
		assertEquals(test.parseInput("CWDD"), -1);
	}

	@Test
	public void testValidatePath() {
		String hostip = System.getProperty("user.dir").replace('\\', '/');
		test.dir = hostip + "/ftp";

		assertEquals(test.validatePath("/ftp"), 0);
		assertEquals(test.validatePath("/测试FTP服务器.txt"), 2);
		assertEquals(test.validatePath(hostip + "/ftp"), 0);
		assertEquals(test.validatePath(hostip + "/ftp/测试FTP服务器.txt"), 1);

	}
}

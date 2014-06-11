package SuperServer;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpServerTest {

	private HttpServer.ServerThread test = new HttpServer(true).testThread;

	@Test
	public void testGetQueryResource() {
		assertEquals(test.getQueryResource("http://192.168.1.120/?balabala"),
				"http://192.168.1.120/test.html");
		assertEquals(test.getQueryResource("http://192.168.1.120/"),
				"http://192.168.1.120/test.html");
		assertEquals(test.getQueryResource("http://192.168.1.120/test"),
				"http://192.168.1.120/test.html");
		assertEquals(test.getQueryResource("http://192.168.1.120/test.htm"),
				"http://192.168.1.120/test.html");
		assertEquals(test.getQueryResource("http://192.168.1.120/test.html"),
				"http://192.168.1.120/test.html");

	}
	
	@Test
	public void testGetHead() {
		assertEquals(test.getHead("http://192.168.1.120/test.html"),
				"HTTP/1.0200OK\n" + "Content-Type:text/html\n"
						+ "Server:myserver\n" + "\n");
		assertEquals(test.getHead("http://192.168.1.120/yeah.jpg"),
				"HTTP/1.0200OK\n" + "Content-Type:image/jpeg\n"
						+ "Server:myserver\n" + "\n");
		assertEquals(test.getHead("http://192.168.1.120/yeah.pdf"), null);
	}

}

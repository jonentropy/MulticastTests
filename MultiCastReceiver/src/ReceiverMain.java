import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ReceiverMain {
	public static void main(String[] a){
		MulticastSocket socket = null;
		try {
			socket = new MulticastSocket(1235);
			socket.joinGroup(InetAddress.getByName("239.255.10.10"));
			socket.setSoTimeout(1 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(socket == null) return;

		while(true) {
			final byte[] buf = new byte[1024];
			final DatagramPacket reply = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(reply);

				System.out.println();
				System.out.println("Received: " + new String(reply.getData()));
		
			} catch (IOException e) {
				System.out.print(".");
			}
		}
	}
}

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;

public class SenderMain {

	private static MulticastSocket socket;

	public static void main(String[] s){

		try {
			socket = new MulticastSocket(1235);
			socket.joinGroup(InetAddress.getByName("239.255.10.10"));
			socket.setSoTimeout(10 * 1000);
		} catch (IOException e) {}

		if(socket == null) return;
		int inttoSend = 0;

		while(true) {
			String toSendString = "" + inttoSend;
			byte[] toSend = toSendString.getBytes();
			
			System.out.println("sending " + toSendString + " at " + new Date());
			try {
				socket.send(new DatagramPacket(toSend, toSend.length, InetAddress.getByName("239.255.10.10"), 1235));
			} catch (Exception e) {}
			
			inttoSend++;
			
			try {Thread.sleep(2000);} catch (InterruptedException e) {}
		}
	}
}
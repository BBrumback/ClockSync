import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
	
	public static boolean isRunning = true;
	
	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(4000);
		byte[] buffer = new byte[256];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		ByteBuffer bb;
		LongBuffer lb;
		long id, t0, t1;
		
		Timer timer = new Timer();
		timer.schedule(new RunUntilServer(), hoursToMilli(1));
		socket.setSoTimeout(30000);
		
		while(isRunning) {
			
			//This try block is to catch time out exceptions
			try {
				socket.receive(packet);
				t1 = System.currentTimeMillis();
				bb = ByteBuffer.wrap(packet.getData());
				lb = bb.asLongBuffer();
				id = lb.get(0);
				t0 = lb.get(1);
				
				bb = ByteBuffer.allocate((Long.SIZE/Byte.SIZE)*4);
				bb.putLong(id);
				bb.putLong(t0);
				bb.putLong(t1);
				bb.putLong(System.currentTimeMillis());
				
				try {
					socket.send(new DatagramPacket(bb.array(), bb.array().length, packet.getAddress(), 5000));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Couldnt send packet for some reason");
				}
			} catch(SocketTimeoutException e) {
				//Just catch the exception and keep running
			}
		}
		System.out.println("I finished");
		socket.close();
		System.exit(0);
	}
	
	//input number of hours returns that in milliseconds
	private static int hoursToMilli(int hours) {
		return hours * 60 * 60 * 1000;
	}
}

class RunUntilServer extends TimerTask {
	
	@Override
	public void run() {
		Server.isRunning = false;
	}
}
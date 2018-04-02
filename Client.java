import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	
	//Static variables to count packets being sent and to allow the program to stop after some time
	public static long packetCount = 0;
	public static boolean isRunning = true;
	
	public static void main(String[] args) throws IOException {
		
		//Set up the List for the last 8 RTT
		final int normListSize = 8;
		List<Long> RTTList = new ArrayList<Long>(normListSize);
		
		//Set up socket to receive packets
		DatagramSocket socket = new DatagramSocket(5000);
		socket.setSoTimeout(10000);
		
		//Set up the initial packet frame work to read incoming packets
		byte[] buffer = new byte[256];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		//Buffers to read packets
		ByteBuffer bb;
		LongBuffer lb;
		
		//Set up my variables to track things
		long id, t0, t1, t2, t3, RTT;
		double theta, smoothTheta;
		int expectedPacket = 0, missedPackets = 0, receivedPackets = 0;
		
		//Set up printing
		PrintStream output = new PrintStream(new FileOutputStream("log.txt"));
		
		//Seting up timers to trigger packets to be sent and when to end
		Timer timer = new Timer();
		//This requests the time by sending a udp packet
		RequestTime t = new RequestTime();
		timer.schedule(t, 0, 10000);
		//This stops the program after n hours
		timer.schedule(new RunUntilClient(), hoursToMilli(1));
		
		//This initializes the RTTList
		for(int i = 0; i < normListSize; i++) {
			RTTList.add((long) 0);
		}
		
		//Prints out a header
		output.printf("PacketNumber, RTT, Theta, ClientTime, CorrectedClientTime, SmoothedTheta\n");
		
		//Starts watching for incomming packets
		while(isRunning) {
			//try to catch time-out errors
			try {
				socket.receive(packet);
				t3 = System.currentTimeMillis();
				receivedPackets++;
			
				//extract packet data
				bb = ByteBuffer.wrap(packet.getData());
				lb = bb.asLongBuffer();
				id = lb.get(0);
				t0 = lb.get(1);
				t1 = lb.get(2);
				t2 = lb.get(3);
				
				//Count the number of missing packets by their ID
				if(id == expectedPacket) {
					expectedPacket++;
				}else if(expectedPacket < id) {
					//I dont expect a large jump in the number of 
					long deltaId = id - expectedPacket;
					expectedPacket += deltaId;
				}
				
				//Calculate the RTT and theta and then print that data
				RTT = (t2-t3) + (t0-t1);
				theta = RTT / 2.0;
				RTTList.set(receivedPackets % normListSize, RTT);
				smoothTheta = RTTList.stream().min(Long::compare).get().longValue()/2.0;
				output.printf("%d,%d,%f,%d,%f,%f\n", id, RTT, theta, t3, t3 + smoothTheta, smoothTheta);
				output.flush();
			} catch(SocketTimeoutException e) {
				missedPackets++;
			}
		}
		System.out.println("I finished");
		output.printf("Number of packets sent, number of packets returned, number of packets droped\n");
		output.printf("%d,%d,%d", packetCount, receivedPackets, missedPackets);
		output.close();
		socket.close();
		System.exit(0);
	}
	
	//input number of hours returns that in milliseconds
	private static int hoursToMilli(int hours) {
		return hours * 60 * 60 * 1000;
	}
	
}

class RequestTime extends TimerTask {
	private DatagramSocket socket;
	private InetAddress ip;
	
	public RequestTime() throws SocketException, UnknownHostException {
		socket = new DatagramSocket();
		
		//*********************************
		//This is the ip of where the server is
		ip = InetAddress.getByName("localhost");
	}
	
	@Override
	public void run() {
		//send packets
		ByteBuffer bb = ByteBuffer.allocate((Long.SIZE/Byte.SIZE)*2);
		bb.putLong(Client.packetCount);
		bb.putLong(System.currentTimeMillis());
		try {
			socket.send(new DatagramPacket(bb.array(), bb.array().length, ip, 4000));
			Client.packetCount++;
		} catch (IOException e) {
			System.err.println("Couldnt send packet for some reason");
		}
	}
}

class RunUntilClient extends TimerTask {
	
	@Override
	public void run() {
		//stop the while loop
		Client.isRunning = false;
	}
}

package dlmsServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import ConstantValue.Constants;
import Library.LibraryInterface;
import Library.LibraryInterfaceHelper;
import dlmsServerInterfaceImpl.McgLibraryImplementation;

public class McgillServer {

	public static void receive(McgLibraryImplementation implementation) {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(Constants.MCGILL_SERVER_PORT);
			byte[] buffer = new byte[1000];
			System.out.println("Mcgill Server Started");
			while (true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String message = new String(request.getData());
				String [] data = message.split("-");
				String userID = data[0].trim();
				String itemID = data[1].trim();
				String itemName = data[3].trim();
				String newItemID = data[4].trim();
				String responseString = "";
				switch(data[2].trim()) {
				case "1": responseString = implementation.borrowItem(userID, itemID);
				break;
				case "2": responseString = implementation.findItem(userID, itemName, true);
				break;
				case "3": responseString = implementation.returnItem(userID, itemID);
				break;
				case "4": responseString = implementation.waitingList(userID, itemID);
				break;
				case "5": responseString = implementation.exchangeItem(userID, itemID, newItemID,true);
				break;
				case "6": responseString = implementation.bookPresent(userID, itemID,newItemID);
				break;
				case "7": responseString = implementation.bookBorrowed(userID, itemID);
				break;
				}
				DatagramPacket reply = new DatagramPacket(responseString.getBytes(), responseString.length(), request.getAddress(),
						request.getPort());
				aSocket.send(reply);
			}
		}catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}
	public static void main (String[] args ) {
		try {
			McgLibraryImplementation mcgStub = new McgLibraryImplementation();
			Runnable task = () -> {
				receive(mcgStub);
			};
			Thread thread = new Thread(task);
			thread.start();
			// create and initialize the ORB //// get reference to rootpoa &amp; activate
			// the POAManager
			ORB orb = ORB.init(args, null);
			// -ORBInitialPort 1050 -ORBInitialHost localhost
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			// create servant and register it with the ORB
			McgLibraryImplementation addobj = new McgLibraryImplementation();
			addobj.setORB(orb);

			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(addobj);
			LibraryInterface href = LibraryInterfaceHelper.narrow(ref);

			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			NameComponent path[] = ncRef.to_name(Constants.MCGILL_SERVER_NAME);
			ncRef.rebind(path, href);

			System.out.println("Mcgill Server ready and waiting ...");

			// wait for invocations from clients
			for (;;) {
				orb.run();
			}
		}

		catch (Exception e) {
			System.err.println("ERROR: " + e);
			e.printStackTrace(System.out);
		}

		System.out.println("Mcgill Exiting ...");

	}
}
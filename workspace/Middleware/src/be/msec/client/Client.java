package be.msec.client;

import be.msec.client.connection.Connection;


import be.msec.client.connection.IConnection;
import be.msec.client.connection.SimulatedConnection;

import java.util.Arrays;

import javax.smartcardio.*;

public class Client {

	private final static byte IDENTITY_CARD_CLA =(byte)0x80;
	private static final byte VALIDATE_PIN_INS = 0x22;
	private final static short SW_VERIFICATION_FAILED = 0x6300;
	private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
	
	private final static byte GET_SERIAL_INS= 0x24;
	
	//INS codes for different SPs
	private final static byte GET_eGov_DATA=(byte)0x05;
	private final static byte GET_Health_DATA=(byte)0x06;	
	private final static byte GET_SN_DATA=(byte)0x07;
	private final static byte GET_def_DATA=(byte)0x08;
	//	timestamp implementation to be discussed
	//private final static byte GET_TS_DATA=(byte)0x09;
	
	//individuals identified by a service-specific pseudonym
	private  byte[] nym_Gov = new byte[]{0x11}; // to have something to test data saving on javacard
	private byte[] nym_Health = new byte[]{0x12}; // to have something to test data saving on javacard
	private byte[] nym_SN = new byte[]{0x13}; // to have something to test data saving on javacard
	private byte[] nym_def = new byte[]{0x14}; // to have something to test data saving on javacard
	
	private byte[] name;
	private byte[] address;
	private byte[] country;
	private byte[] birthdate;
	private byte[] age;
	private byte[] gender;
	private byte[] picture;
	private byte[] bloodType;
	
	//Certificates and Keys
	private final static byte CertC0=(byte)0x20;	//common cert
	private final static byte SKC0=(byte)0x21;
	private final static byte CertCA=(byte)0x22;	//CA
	private final static byte CertG=(byte)0x23;	//cert for gov timestam
	private final static byte SKG=(byte)0x24;
	private final static byte CertSP=(byte)0x25;	//cert in each domain
	private final static byte SKsp=(byte)0x26;
	private final static byte Ku=(byte)0x27;
	private final static byte PKG=(byte)0x28;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		IConnection c;
		boolean simulation = true;		// Choose simulation vs real card here

		if (simulation) {
			//Simulation:	
			c = new SimulatedConnection();
		} else {
			//Real Card:
			c = new Connection();
			((Connection)c).setTerminal(0); //depending on which cardreader you use
		}
		
		c.connect(); 
		
		try {

			/*
			 * For more info on the use of CommandAPDU and ResponseAPDU:
			 * See http://java.sun.com/javase/6/docs/jre/api/security/smartcardio/spec/index.html
			 */
			
			CommandAPDU a;
			ResponseAPDU r;
			
			if (simulation) {
				//0. create applet (only for simulator!!!)
				//Constructs a CommandAPDU from the four header bytes, command data, and expected response data length. (see link above)
				// 0x7f = 127 in decimal
				a = new CommandAPDU(0x00, 0xa4, 0x04, 0x00,new byte[]{(byte) 0xa0, 0x00, 0x00, 0x00, 0x62, 0x03, 0x01, 0x08, 0x01}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("select installer applet failed");
				
				a = new CommandAPDU(0x80, 0xB8, 0x00, 0x00,new byte[]{0xb, 0x01,0x02,0x03,0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x00, 0x00, 0x00}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("Applet creation failed");
				
				//1. Select applet  (not required on a real card, applet is selected by default)
				a = new CommandAPDU(0x00, 0xa4, 0x04, 0x00,new byte[]{0x01,0x02,0x03,0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x00, 0x00}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("Applet selection failed");
			}
			
			//2. Send PIN
			a = new CommandAPDU(IDENTITY_CARD_CLA, VALIDATE_PIN_INS, 0x00, 0x00,new byte[]{0x01,0x02,0x03,0x04});
			r = c.transmit(a);

			System.out.println(r);
			if (r.getSW()==SW_VERIFICATION_FAILED) throw new Exception("PIN INVALID");
			else if(r.getSW()!=0x9000) throw new Exception("Exception on the card: " + r.getSW());
			System.out.println("PIN Verified");
			
			
			// get Serial#, example to get data from card
			// this prints a single entry of the byte array
			a = new CommandAPDU(IDENTITY_CARD_CLA, GET_SERIAL_INS, 0x00, 0x00);
			r = c.transmit(a);

			System.out.println(r);
			if (r.getSW()==SW_VERIFICATION_FAILED) throw new Exception("PIN INVALID");
			else if(r.getSW()!=0x9000) throw new Exception("Exception on the card: " + r.getSW());

			//print response data array
			byte[] b =r.getData();
			String s = Arrays.toString(b);
			System.out.println(s);

			//decimal encoding, choice of '8' is only to extract an example from the byte array b
			System.out.println((byte)r.getData()[8]);
			//ASCII
			System.out.println(new String(new byte[]{ (byte)r.getData() [8]}, "US-ASCII"));

			//eGov data
//			System.out.println("FooBar:");
//			a = new CommandAPDU(IDENTITY_CARD_CLA, GET_eGov_DATA, 0x00, 0x00);
//			r = c.transmit(a);
			
		} catch (Exception e) {
			throw e;
		}

		finally {
			System.out.println("------ end connection ------");
			c.close();  // close the connection with the card
		}


	}

}

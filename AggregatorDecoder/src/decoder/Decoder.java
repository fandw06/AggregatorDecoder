package decoder;


import java.util.Arrays;

/**
 * <p>The class {@code Decoder} is a stateless class for data decoding and parsing. These two steps are completed by {@link #decode(byte[])} and {@link #parse(Packet)}. In the first step, the raw 
 * bit-stream input is decoded as {@link Packet}, and in the second step the packet is further parsed to return calibrated data like acceleration, ecg, etc.
 * 
 * <p>Raw input is the received data from FPGA through SPI (vSPI in FPGA) as a byte array with a fixed length 32. (The maximum 
 * length of an array received through SPI is 64 defined by IOIO.) To be more precise, the input data is a 256-bit bit-stream including incomplete preamble 
 * (0xAAAAAAAA), header, payload and trailer. In decoding step, the {@link #decode} method searches headers (matchBitPattern) in the bit-stream then decode the payload by inverting every 16 bits.
 * The output of {@link #decode} method is {@link Packet} type which includes {@link PacketType} and payload.</p>
 * 
 * <p>In parsing step, the payload is parsed according to the {@link PacketType} and the data is calibrated by related methods.
 * 
 * 
 * @author Dawei Fan
 * 
 * @version 1.0 12/22/2015
 * 
 * @version 1.1 01/20/2015
 * 		<p>1, Add a parameter for decode() to process a user-defined type, avoiding for-loop to compare all headers.</p>
 * 
 * @since 1.0
 *
 */
public class Decoder {
	
	private static final int RAW_PACKET_SIZE = 32;
	
	/**
	 * Threshold is used for searching headers in {@link #decode}. If the threshold is 0, then strictly search the header by compare strings. Since there maybe error
	 * in received headers due to the noise, a threshold could be set to tolerate errors to some extent. The threshold is better to be less than 2.
	 * 
	 */
	private static final int THRESHOLD = 2;
	
	/**
	 * In {@link #decode} step, the data is inverted every 16 bits.
	 */
	private static final int WORD_LENGTH = 16;
	
	/**
	 * The number of first several bits in the input bit-stream for searching headers.
	 */
	private static final int SEARCH_RANGE = 16*Byte.SIZE;
	
	/**
	 * The range of ADC. This is for ECG and voltage of super capacitor.
	 */
	private static final double ADC_RANGE = 1.8;
	
//	private static final double G = 9.8067;
		
	/**
     * Decoder should not be instantiated.
     */
	private Decoder() {}
	
	/** 
	 * <p> The method {@code decode} decodes a 256-bit bit stream and return a {@link Packet}.</p>
	 * <p> If headers are not found, then return null.
	 * @param input 256-bit input bit-stream.
	 * @param p User specified header, pass "null" if search for all possible headers.
	 * 
	 * @return A decoded packet. Return null if headers are not found. (Consider using exceptions)
	 */
	public static Packet decode(byte[] input, PacketType p) {
		/** 
		 * If the length of raw packet is not 32, it is not a valid packet.
		 */
		if (input.length != RAW_PACKET_SIZE) 
			return null;	
		
		String sData = byte2string(input);		
		for (int i = 0; i <= SEARCH_RANGE; i++) {
			if (p == null) {
				for (PacketType pt : PacketType.values()) {
					String sHeader = pt.getBinaryStringLE();
					if (matchBitPattern(sHeader, sData.substring(i, i+WORD_LENGTH), THRESHOLD)) {
						/**
						 * When a specified header is matched, update validPacket, packetType and feed payload.
						 */
						int payload[] = new int[pt.getPayloadLength()];
						/**
						 * Offset for 2-byte header.
						 */
						int offset = 2 * Byte.SIZE;
						for (int j = 0; j < pt.getPayloadLength()/2; j++) {
							int data = string2int((String)sData.subSequence(i+offset+j*WORD_LENGTH, i+offset+j*WORD_LENGTH+WORD_LENGTH), true);
							payload[2*j] = (data>>Byte.SIZE);
							payload[2*j+1] = data&0xff;
						}
						int trailer = string2int((String)sData.subSequence(i+offset+pt.getPayloadLength()/2*WORD_LENGTH, i+offset+pt.getPayloadLength()/2*WORD_LENGTH+WORD_LENGTH), true);
						if (trailer !=  pt.getTrailer()) {
			//				System.out.println("The trailer of the packet is "+ Integer.toHexString(trailer)+" , but "+ Integer.toHexString(pt.getTrailer()) + " is expected.");
						}
						return new Packet(pt, payload);
					}
				}
			}
			/* Just search for a user-specified header. */
			else {
				String sHeader = p.getBinaryStringLE();
				if (matchBitPattern(sHeader, sData.substring(i, i+WORD_LENGTH), THRESHOLD)) {
					/**
					 * When a specified header is matched, update validPacket, packetType and feed payload.
					 */
					int payload[] = new int[p.getPayloadLength()];
					/**
					 * Offset for 2-byte header.
					 */
					int offset = 2 * Byte.SIZE;
					for (int j = 0; j < p.getPayloadLength()/2; j++) {
						int data = string2int((String)sData.subSequence(i+offset+j*WORD_LENGTH, i+offset+j*WORD_LENGTH+WORD_LENGTH), true);
						payload[2*j] = (data>>Byte.SIZE);
						payload[2*j+1] = data&0xff;
					}
					int trailer = string2int((String)sData.subSequence(i+offset+p.getPayloadLength()/2*WORD_LENGTH, i+offset+p.getPayloadLength()/2*WORD_LENGTH+WORD_LENGTH), true);
					if (trailer !=  p.getTrailer()) {
		//				System.out.println("The trailer of the packet is "+ Integer.toHexString(trailer)+" , but "+ Integer.toHexString(pt.getTrailer()) + " is expected.");
					}
					return new Packet(p, payload);
				}
			}
		}
		return null;	
	}
	
	
	/** 
	 * <p> The method {@code parse} parse the payload of a packet according to {@link PacketType} and return calibrated data.</p>
	 * 
	 * @param packet the decoded packet.
	 * @return Parsed data.
	 * 
	 * @version 1.1
	 * 
	 */
	public static double[] parse(Packet packet) {
		if (packet == null) return null;
		switch (packet.packetType) {
		
			case SAP_ACC: {
				/**
				 * ax, ay, az.
				 */
				double acceleration[] = new double[3];
				for (int i = 0; i<3; i++) 
					acceleration[i] = calibrateAcceleration(packet.payload[i*2+1]);
				return acceleration;
			}
		
			case SAP_ALL: {
				/**
				 * ax, ay, az, vol, ecg.
				 */
				double all[] = new double[5];
				for (int i = 0; i<3; i++)
					all[i] = calibrateAcceleration(packet.payload[i*2+1]);
				all[3] = calibrateVoltage(packet.payload[3*2 + 1]);
				all[4] = calibrateECG(packet.payload[4*2+1]);
				return all;
			}
			
			case SAP_ACC_VOL: {
				/**
				 * ax, ay, az, vol.
				 */
				double all[] = new double[4];
				for (int i = 0; i<3; i++)
					all[i] = calibrateAcceleration(packet.payload[i*2+1]);
				
				int vol = ((packet.payload[7] & 0b00111111)<<2)+((packet.payload[9])>>>6);
				all[3] = calibrateVoltage(vol);
				return all;
			}
			
			case SAP_ACC_ECG: {
				/**
				 * ax, ay, az, vol.
				 */
				double all[] = new double[4];
				for (int i = 0; i<3; i++)
					all[i] = calibrateAcceleration(packet.payload[i*2+1]);
				
				int vol = ((packet.payload[7])<<2)+((packet.payload[9])>>>6);
				all[3] = calibrateVoltage(vol);
				return all;
			}
			
			case SAP_DOUBLE: {
				/**
				 * ax, ay, az, vol, ecg, ax, ay, az, vol, ecg.
				 */
				double all[] = new double[5];
				/**
				 * Correct errors using 'or' operation of first and second part of the packets.
				 */
				int correct[] = new int[5];
				for (int i = 0; i<5; i++) 
					correct[i] = packet.payload[i] | packet.payload[i+5];
				
				for (int i = 0; i<3; i++)
					all[i] = calibrateAcceleration(correct[i*2+1]);
				all[3] = calibrateVoltage(correct[3*2 + 1]);
				all[4] = calibrateECG(correct[4*2+1]);
				return all;
			}
			
			default:
				System.err.println("Unknow packet!");
				return null;
		}
	}
	
	/**
	 * The acceleration data coming from the accelerometer is 12 bits but only 8 MSBs are read.
	 * The range of accelerator is -2g~2g and the scale factor is g/1000.
	 * 
	 * @param data 8 MSBs value
	 * @return Calibrated acceleration 
	 */
	private static double calibrateAcceleration(int data) {
		if (data > 128)
			data = data - 256;
		return (double)(data<<4)/(double)1000;
	}
	
	/**
	 * The voltage data is 8 bits.
	 * The range of voltage is 0V~3V and the scale factor is 1/256.
	 *
	 * @param data 8 MSBs value
	 * @return Calibrated acceleration
	 */
	private static double calibrateVoltage(int data) {
		return (double)(data)/(double)255*ADC_RANGE;
	}

	/**
	 * The voltage data is 8 bits.
	 * The range of voltage is 0V~2.7V and the scale factor is 1/256.
	 *
	 * @param data 8 MSBs value
	 * @return Calibrated acceleration
	 */
	private static double calibrateECG(int data) {
		return (double)(data)/(double)255*ADC_RANGE;
	}
	
	
	private static boolean matchBitPattern(String s1, String s2, int threshold) {
		if (threshold == 0)
			return s1.equals(s2);		
		int diff = Integer.bitCount(string2int(s1, false) ^ string2int(s2, false));
		return (diff <= threshold);
	}
	
	public static void printAsHexString(int d[]) {
		String res[] = new String[d.length];
		for (int i = 0; i<d.length; i++)
			res[i] = String.format("%02x", d[i]);
		System.out.println(Arrays.toString(res));
	}
	
	public static void printAsHexString(int d[], String name) {
		String res[] = new String[d.length];
		for (int i = 0; i<d.length; i++)
			res[i] = String.format("%02x", d[i]);
		System.out.print(name+": ");
		System.out.println(Arrays.toString(res));
	}
	
	private static String byte2string(byte input[]) {
		StringBuilder data = new StringBuilder(input.length * Byte.SIZE);
		for (int i = 0; i < Byte.SIZE * input.length; i++ ) 
			data.append((input[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
		return data.toString();
	}
	
	public static int diffBits(int a, int b) {
		return Integer.bitCount(a ^ b);
	}
	
	/**
	 * Convert a binary string to a integer.
	 * Big-endian: MSB is s[0]
	 * Little-endian: MSB is s[end]
	 * @param s
	 * @return
	 */
	private static int string2int(String s, boolean littleEndian) {
		int data = 0;
		int base = 1;
		if (littleEndian) {
			for (int i = 0; i < s.length(); i++) {
				if (s.charAt(i) == '1')
					data += base;
				base = base<<1;
			}
		}
		else {
			for (int i = s.length()-1; i >=0 ; i--) {
				if (s.charAt(i) == '1')
					data += base;
				base = base<<1;
			}
		}
		return data;
	}
}

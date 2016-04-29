package decoder;


/**
 * <p>The class {@code PacketType} contains all defined packet types. Each packet has a 16-bit header, 8-bit trailer (default value is 
 * 0xff) and a specified packet length. A header indicates the packet type and a trailer is set to be 0xff for all types of packets, 
 * thus trailer is defined as static field.
 * 
 * <p>Methods include getters for fields and generating a binary string of a header for searching in raw input bit stream.
 * 
 * @author Dawei Fan
 * 
 * @version 1.0 12/22/2015
 * 
 * @version 1.1 01/20/2016
 * 		<p>1, Add SAP_DOUBLE type for error correcting test.</p>
 * 
 * @version 1.2 04/29/2016
 * 		<p>1, Modify 2 headers for 2016 ASSIST site visit.</p>	
 * @since 1.0
 */
public enum PacketType {

	/**
	 * <p>Transmit acceleration data only:
	 * (Test, 2015)</p>
	 * Format:
	 * 5322-00AX-00AY-00AZ-0000-0000-0000-FFFF (16bytes).<p>
	 * 
	 */
	SAP_ACC         (0x5322, 16),
	
	/**
	 * <p>Transmit acceleration and voltage of super-capacitor data:
	 * (For site visit 2016)</p>
	 * Format:
	 * 5C22-01AX-01AY-01AZ-01VA-01VB-FFFF (14bytes).<p>
	 * UU = 00 + the 6 most significant bits of the vol sample <p>
	 * VV = 2 least significant bits of the vol sample + 000000
	 * 
	 * 
	 */
	SAP_ACC_VOL     (0x5C22, 14),
	
	/**
	 * <p>Transmit acceleration and ecg data:
	 * (For site visit 2016)</p>
	 * Format:
	 * 5C22-01AX-01AY-01AZ-01VA-01VB-FFFF (14bytes).<p>
	 * UU = 00 + the 6 most significant bits of the ECG sample <p>
	 * VV = 2 least significant bits of the ECG sample + 000000
	 * 
	 */
	SAP_ACC_ECG     (0x532D, 14),

	SAP_DOUBLE      (0x5C2D, 24),
	
	/**
	 * <p>Transmit acceleration voltage of super-capacitor, and ECG data:
	 * (For site visit 2015)</p>
	 * Format:
	 * 5C2D-00AX-00AY-00AZ-00Vo-01EG-FFFF (14bytes).<p>
	 * 
	 */
	SAP_ALL         (0x5C2D, 14);

	private final int header;
	/**
	 * Trailer 0xFFFF is the same for all types of packets.
	 */
	private static final int trailer = 0xffff;
	private final int packetLength;
	private final int payloadLength;
	
	PacketType(int header, int length) {
		this.header = header;
		this.packetLength = length;
		this.payloadLength = length - 4;
	}
	
	/**
	 * Generate a binary string for a header using its type for searching. As the input data stream is inverted, 
	 * here the string is in a sense of "little-endian" which lower byte appears at the beginning of the string.
	 * <p>For example, SAP_ACC = 0x5322, the output string is:
	 * 
	 * <br>Byte order:    0    1    2    3    (In string)
	 * <br>Binary:        0100 0100 1100 1010
	 * <br>Hex:           2    2    3    5
	 * <br>String offset: 0       ---      31
	 * </p>
	 * @return Generated string
	 */
	public String getBinaryStringLE() {
		StringBuilder data = new StringBuilder(2 * Byte.SIZE);
		for (int i = 0; i < 2 * Byte.SIZE; i++ ) 
			data.append(((header >> i)  & 1) == 0 ? '0' : '1');
		return data.toString();
	}
	
	/**
	 * Return the header value of a packet type.
	 * @return 16-bit integer value of a header.
	 */
	public int getHeader() {
		return this.header;
	}
	
	/**
	 * Return the trailer value of a packet type. Now it is the same for all types.
	 * @return 8-bit integer value of a trailer.
	 */
	public int getTrailer() {
		return PacketType.trailer;
	}
	
	/**
	 * Return the packet length of a packet type.
	 * @return length.
	 */
	public int getPacketLength() {
		return this.packetLength;
	}

	/**
	 * Return the payload length of a packet type.
	 * @return length.
	 */
	public int getPayloadLength() {
		return this.payloadLength;
	}
}
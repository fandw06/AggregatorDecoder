package decoder;


import java.util.Arrays;

/**
 * <p>The class {@code Packet} is a data structure to represent a decoded packet, which includes a PacketType and a payload.
 * This is used as a return type for {@link Decoder#decode(byte[])} method. Payload is used for further data parsing as an input for
 * {@link Decoder#parse(Packet)} method.
 *  
 * @author Dawei Fan
 * 
 * @version 1.0 12/22/2015
 * 
 * @version 1.1 01/20/2016
 * 		<p>1, Add getters of PacketType and Payload.</p>
 * 
 * @since 1.0
 */
public class Packet {
	PacketType packetType;
	int[] payload;
	
	public Packet(PacketType pt, int[] p) {
		this.packetType = pt;
		this.payload = Arrays.copyOf(p, p.length);
	}
	
	public PacketType getPacketType() {
		return packetType;
	}
	
	public int[] getPayload() {
		return this.payload;
	}
}

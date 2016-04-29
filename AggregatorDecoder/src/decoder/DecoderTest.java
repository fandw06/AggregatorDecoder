package decoder;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class DecoderTest {

	@Test
	public void testDecode() {
		
	}

	@Test
	public void testParse() {
		/**
		 * Data = 0x68;
		 */
		int data = 0x68;
		int pl[] = {01, 0x89, 01, 0x23, 01, 0x34, 01, (data>>>2), 01, (data<<6), 0xff, 0xff};
		Packet pAccEcg = new Packet(PacketType.SAP_ACC_ECG, pl);
		double res[] = Decoder.parse(pAccEcg);
		System.out.println(Arrays.toString(res));
	}

}

package net.sf.cram.encoding;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import net.sf.block.ByteBufferUtils;
import net.sf.block.ExposedByteArrayOutputStream;
import net.sf.cram.EncodingID;
import net.sf.cram.EncodingParams;

public class GammaIntegerEncoding implements Encoding<Integer> {
	public static final EncodingID ENCODING_ID = EncodingID.GAMMA;
	private int offset ;

	public GammaIntegerEncoding() {
	}
	
	@Override
	public EncodingID id() {
		return ENCODING_ID;
	}

	public static EncodingParams toParam(int offset) {
		GammaIntegerEncoding e = new GammaIntegerEncoding();
		e.offset = offset ;
		return new EncodingParams(ENCODING_ID, e.toByteArray());
	}

	@Override
	public byte[] toByteArray() {
		ByteBuffer buf = ByteBuffer.allocate(10);
		ByteBufferUtils.writeUnsignedITF8(offset, buf);
		buf.flip();
		byte[] array = new byte[buf.limit()];
		buf.get(array);
		return array;
	}

	@Override
	public void fromByteArray(byte[] data) {
		offset = ByteBufferUtils.readUnsignedITF8(data) ;
	}

	@Override
	public BitCodec<Integer> buildCodec(Map<Integer, InputStream> inputMap,
			Map<Integer, ExposedByteArrayOutputStream> outputMap) {
		return new GammaIntegerCodec(offset);
	}

}
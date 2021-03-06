/*******************************************************************************
 * Copyright 2013 EMBL-EBI
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sf.cram.encoding.writer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.sf.cram.encoding.BitCodec;
import net.sf.cram.encoding.DataSeries;
import net.sf.cram.encoding.DataSeriesMap;
import net.sf.cram.encoding.DataSeriesType;
import net.sf.cram.encoding.Encoding;
import net.sf.cram.encoding.EncodingFactory;
import net.sf.cram.io.BitOutputStream;
import net.sf.cram.io.ExposedByteArrayOutputStream;
import net.sf.cram.structure.CompressionHeader;
import net.sf.cram.structure.EncodingID;
import net.sf.cram.structure.EncodingKey;
import net.sf.cram.structure.EncodingParams;

public class DataWriterFactory {

	public Writer buildWriter(BitOutputStream bos,
			Map<Integer, ExposedByteArrayOutputStream> outputMap,
			CompressionHeader h, int refId) throws IllegalArgumentException,
			IllegalAccessException {
		Writer writer = new Writer();
		writer.captureReadNames = h.readNamesIncluded;
		writer.refId = refId ;
		writer.substitutionMatrix = h.substitutionMatrix ;
		writer.AP_delta = h.AP_seriesDelta ;

		for (Field f : writer.getClass().getFields()) {
			if (f.isAnnotationPresent(DataSeries.class)) {
				DataSeries ds = f.getAnnotation(DataSeries.class);
				EncodingKey key = ds.key();
				DataSeriesType type = ds.type();
				
				f.set(writer,
						createWriter(type, h.eMap.get(key), bos, outputMap));
			}

			if (f.isAnnotationPresent(DataSeriesMap.class)) {
				DataSeriesMap dsm = f.getAnnotation(DataSeriesMap.class);
				String name = dsm.name();
				if ("TAG".equals(name)) {
					Map<Integer, DataWriter<byte[]>> map = new HashMap<Integer, DataWriter<byte[]>>();
					for (Integer key : h.tMap.keySet()) {
						EncodingParams params = h.tMap.get(key);
						DataWriter<byte[]> tagWtiter = createWriter(
								DataSeriesType.BYTE_ARRAY, params, bos,
								outputMap);
						map.put(key, tagWtiter);
					}
					f.set(writer, map);
				}
			}
		}
		
		return writer;
	}

	private <T> DataWriter<T> createWriter(DataSeriesType valueType,
			EncodingParams params, BitOutputStream bos,
			Map<Integer, ExposedByteArrayOutputStream> outputMap) {
		EncodingFactory f = new EncodingFactory();
		Encoding<T> encoding = f.createEncoding(valueType, params.id);
		if (encoding == null)
			throw new RuntimeException("Encoding not found: value type="
					+ valueType.name() + ", encoding id=" + params.id.name());

		encoding.fromByteArray(params.params);

		return new DefaultDataWriter<T>(encoding.buildCodec(null, outputMap),
				bos);
	}

	private static class DefaultDataWriter<T> implements DataWriter<T> {
		private BitCodec<T> codec;
		private BitOutputStream bos;

		public DefaultDataWriter(BitCodec<T> codec, BitOutputStream bos) {
			this.codec = codec;
			this.bos = bos;
		}

		@Override
		public long writeData(T value) throws IOException {
			return codec.write(bos, value);
		}

	}
}

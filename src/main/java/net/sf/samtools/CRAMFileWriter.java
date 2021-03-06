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
package net.sf.samtools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.cram.build.ContainerFactory;
import net.sf.cram.build.CramIO;
import net.sf.cram.build.Sam2CramRecordFactory;
import net.sf.cram.structure.Container;
import net.sf.cram.structure.CramHeader;
import net.sf.cram.structure.CramRecord;

public class CRAMFileWriter extends SAMFileWriterImpl {
	private String fileName;
	private List<CramRecord> records = new ArrayList<CramRecord>();
	private ContainerFactory containerFactory;
	private int recordsPerSlice = 10000;
	private int containerSize = recordsPerSlice * 10;

	private Sam2CramRecordFactory sam2CramRecordFactory;
	private OutputStream os;

	private boolean preserveReadNames = false;

	protected boolean shouldFlushContainer(SAMRecord nextRecord) {
		if (records.size() >= containerSize)
			return true;
		return false;
	}

	protected void flushContainer() throws IllegalArgumentException,
			IllegalAccessException, IOException {
		Container container = containerFactory.buildContainer(records);
		records.clear();
		CramIO.writeContainer(container, os);
	}

	@Override
	protected void writeAlignment(SAMRecord alignment) {
		if (shouldFlushContainer(alignment))
			try {
				flushContainer();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		CramRecord cramRecord = sam2CramRecordFactory
				.createCramRecord(alignment);
		records.add(cramRecord);
	}

	@Override
	protected void writeHeader(String textHeader) {
		SAMFileHeader header = new SAMFileHeader();
		containerFactory = new ContainerFactory(header, recordsPerSlice,
				preserveReadNames);

		header.setTextHeader(textHeader);
		CramHeader cramHeader = new CramHeader(2, 0, fileName, header);
		try {
			CramIO.writeCramHeader(cramHeader, os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void finish() {
		if (!records.isEmpty())
			try {
				flushContainer();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	@Override
	protected String getFilename() {
		return fileName;
	}
}

package com.gartham.tools.files.dupechecker;

import java.util.Arrays;

import org.alixia.javalibrary.strings.StringTools;

public class FileHash {
	private final byte[] bytes;

	public FileHash(byte... bytes) {
		this.bytes = bytes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileHash other = (FileHash) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return StringTools.toHexString(bytes);
	}

}
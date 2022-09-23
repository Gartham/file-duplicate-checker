package com.gartham.tools.files.dupechecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FileDupeChecker {

	private static class Hash {
		private final byte[] bytes;

		public Hash(byte... bytes) {
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
			Hash other = (Hash) obj;
			if (!Arrays.equals(bytes, other.bytes))
				return false;
			return true;
		}

	}

	private static final int BUFFER_SIZE = 65535;

	public static void main(String[] args) throws NoSuchAlgorithmException {

		File file = new File(args[0]);
		if (!file.exists())
			System.err.println("File not found: " + file);
		else if (!file.isDirectory())
			System.err.println("You need to specify a directory to search through!");
		else {
			Map<Hash, File> hashtable = new HashMap<>();
			MessageDigest hasher = MessageDigest.getInstance("SHA-256");

			Stack<File> dirchildren = new Stack<>();
			dirchildren.push(file);
			while (!dirchildren.isEmpty()) {
				File tf = dirchildren.pop();
				for (File f : tf.listFiles(File::isDirectory))
					dirchildren.push(f);
				addToHashtable(hashtable, hasher, tf.listFiles());
			}

		}
	}

	public static void addToHashtable(Map<Hash, File> hashtable, MessageDigest hasher, File... files) {
		byte[] bytes = new byte[BUFFER_SIZE];
		for (File f : files) {
			// Only reads in non-folders.
			if (f.isFile())// NOT RECURSIVE
				try (FileInputStream fis = new FileInputStream(f)) {
					int count;
					try {
						while ((count = fis.read(bytes)) != -1)
							hasher.update(bytes, 0, count);
					} catch (IOException e) {
						System.err.println("An error occurred while READING the file: " + f + ".\n\tError message: "
								+ e.getMessage());
						hasher.reset();
						continue;
					}
					byte[] hash = hasher.digest();
					Hash h = new Hash(hash);
					if (hashtable.containsKey(h))
						System.out.println("Duplicate between files: \n\t" + hashtable.get(h) + "\n\t" + f);
					else
						hashtable.put(h, f);
				} catch (IOException e) {
					System.err.println(
							"An error occurred with OPENING the file: " + f + ".\n\tError message: " + e.getMessage());
				}
		}
	}

}

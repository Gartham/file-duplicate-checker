package com.gartham.tools.files.dupechecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.alixia.javalibrary.strings.StringTools;

public class FileDupeChecker {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		File file = new File(args[0]);
		if (!file.exists())
			System.err.println("File not found: " + file);
		else if (!file.isDirectory())
			System.err.println("You need to specify a directory to search through!");
		else {

			FileDatabase db = new FileDatabase();

			Stack<File> dirchildren = new Stack<>();
			dirchildren.push(file);

			while (!dirchildren.isEmpty()) {
				File tf = dirchildren.pop();
				for (File f : tf.listFiles(File::isDirectory))
					dirchildren.push(f);
				for (File f : tf.listFiles()) {
					// Only reads in non-folders.
					if (f.isFile())
						try {
							db.addFile(f);
						} catch (IOException e1) {
							System.err.println("Failed to open a file: " + e1
									+ ". There is a small chance that this will cause OTHER files to not be tracked appropriately.");
						}
				}
			}

			for (Entry<FileHash, List<File>> e : db.collectDuplicates().entrySet())
				if (e.getValue().size() != 1) {
					System.out.println("Files with hash " + e.getKey() + ':');
					for (File f : e.getValue())
						System.out.println("\t" + f);
				}

		}
	}

	private static final int BUFFER_SIZE = 65535, STATUS_DELAY_GAP = 10000;
	private static final boolean PRINT_STATUS = true;

	/**
	 * Runs the old checking algorithm from before file lengths were considered by
	 * this program. (This function is just a copy of the main function from that
	 * point in time.)
	 * 
	 * @param args The program arguments. (This should contain at least one value
	 *             which is the folder to scan. Subsequent values are ignored.)
	 * @throws NoSuchAlgorithmException If SHA-256 is not supported by this Java
	 *                                  installation.
	 */
	public static void checkOld(String... args) throws NoSuchAlgorithmException {
		File file = new File(args[0]);
		if (!file.exists())
			System.err.println("File not found: " + file);
		else if (!file.isDirectory())
			System.err.println("You need to specify a directory to search through!");
		else {
			Map<FileHash, List<File>> hashtable = new HashMap<>();
			MessageDigest hasher = MessageDigest.getInstance("SHA-256");

			Stack<File> dirchildren = new Stack<>();
			dirchildren.push(file);

			long bytesHashed = 0, lastPrint = System.currentTimeMillis();

			while (!dirchildren.isEmpty()) {
				File tf = dirchildren.pop();
				for (File f : tf.listFiles(File::isDirectory))
					dirchildren.push(f);
				byte[] bytes = new byte[BUFFER_SIZE];
				for (File f : tf.listFiles()) {
					// Only reads in non-folders.
					if (f.isFile())// NOT RECURSIVE
						try (FileInputStream fis = new FileInputStream(f)) {
							int count;
							try {
								while ((count = fis.read(bytes)) != -1) {
									hasher.update(bytes, 0, count);
									if (PRINT_STATUS) {
										bytesHashed += count;
										long curtime = System.currentTimeMillis();
										if (curtime - STATUS_DELAY_GAP > lastPrint) {
											System.out
													.println(
															"Hashed " + StringTools.formatBytes(bytesHashed)
																	+ " in the last " + (curtime - lastPrint)
																	+ "ms.\n\tRate: "
																	+ StringTools.formatBytes(
																			bytesHashed * 1000 / (curtime - lastPrint))
																	+ "/s");
											lastPrint = curtime;
											bytesHashed = 0;
										}
									}
								}
							} catch (IOException e) {
								System.err.println("An error occurred while READING the file: " + f
										+ ".\n\tError message: " + e.getMessage());
								hasher.reset();
								continue;
							}
							byte[] hash = hasher.digest();
							FileHash h = new FileHash(hash);
							if (hashtable.containsKey(h))
								hashtable.get(h).add(f);
							else {
								List<File> files = new ArrayList<>(1);
								hashtable.put(h, files);
								files.add(f);
							}
						} catch (IOException e) {
							System.err.println("An error occurred with OPENING the file: " + f + ".\n\tError message: "
									+ e.getMessage());
						}
				}
			}

			for (Entry<FileHash, List<File>> e : hashtable.entrySet())
				if (e.getValue().size() != 1) {
					System.out.println("Files with hash " + e.getKey() + ':');
					for (File f : e.getValue())
						System.out.println("\t" + f);
				}

		}
	}

}

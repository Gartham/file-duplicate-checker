package com.gartham.tools.files.dupechecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * Contains and tracks a set of {@link File}s. A {@link FileDatabase} stores a
 * map of maps of lists of files. The Root Map maps is keyed by file size, the
 * Length Maps each map file hashes to respective files, and finally, the lists
 * contained by the inner maps each store {@link File} objects with the same
 * exact hash (these {@link File}s are exactly equal).
 * </p>
 * 
 * <pre>
 * <code>Map[file-size : Map[file-hash : List[file]]]</code>
 * </pre>
 * 
 * <h2>Root Map</h2>
 * <p>
 * The Root Map stores an Length Map for each file-size encountered. If a 2KB
 * (2048 Bytes) file has been indexed in this {@link FileDatabase}, then the
 * Root Map will have an entry with <code>2048</code> as the key and a
 * {@link Map} of hashes of {@link File}s with that size to the {@link File}s
 * themselves.
 * </p>
 * <h2>Length Maps</h2>
 * <p>
 * There is one Length Map for each file size. If two files are encountered that
 * are both 512B but possess different hashes, then there will be an Length Map
 * containing both of them under different {@link FileHash} keys.
 * </p>
 * <h2>Lists</h2>
 * <p>
 * The lists store each store a set of {@link File}s that have the same hash
 * (and the same size).
 * </p>
 * <p>
 * Each list is indexed by a file-size <i>and</i> a {@link FileHash}.
 * </p>
 * <h1>Behavior</h1>
 * <p>
 * This class is used to index files as they're added, so it obtains
 * {@link File#length() file lengths} and hashes the files on the fly, <i>as is
 * needed</i>.
 * </p>
 * <ul>
 * <li>If two files are added with different lengths, then there will be two
 * separate Length Maps.</li>
 * <li>If two files are added with the same length, there will be a single Inner
 * Map that contains both of them.
 * <ul>
 * <li>If the files have different hashes, then the Length Map will contain two
 * separate entries.</li>
 * <li>If the files have the same hash, the Length Map will contain one entry
 * with the two {@link File}s' hash as the key and a {@link List} containing
 * both of the {@link File}s as the entry.</li>
 * </ul>
 * <p>
 * This class is designed to hash files <i>lazily</i>. If a {@link FileDatabase}
 * is created and only one {@link File} is added to it, there's no need to hash
 * that file because there's nothing else to compare it with. This same
 * principle holds for files of different lengths. If two files with different
 * lengths are added, neither file will be hashed because it is already known
 * that they are not the same file.
 * </p>
 * <p>
 * If this {@link FileDatabase} contains a {@link File} with a certain length,
 * and <i>no other {@link File} in this {@link FileDatabase}</i> has the same
 * length, then the unique-length file is stored <b>under the <code>null</code>
 * key</b> in its Length Map. Whenever another {@link File} is
 * {@link #addFile(File) added} to this {@link FileDatabase} with the same
 * length, the Length Map for that length is retrieved and then the
 * <code>null</code> key is checked. If the <code>null</code> key contains an
 * element, it is removed and the sole {@link File} it stored is hashed (and
 * that hash is compared to the {@link File} being added). Then, one of the
 * following occurs:
 * </p>
 * <ul>
 * <li>If the hashes collide, then both {@link File}s are stored under the same
 * Length Map entry inside the {@link List} under the files' hash.</li>
 * <li>If the hashes are not equal, then the {@link File} being added is stored
 * under its own hash, and the other {@link File} is stored under <i>its</i> own
 * hash.
 * </p>
 * <p>
 * This object should never have a Length Map that contains more than one
 * {@link File} and has an entry with the <code>null</code> key.
 * </p>
 * 
 * @author Gartham
 *
 */
public class FileDatabase {
	private final Map<Long, Map<FileHash, List<File>>> rootMap = new HashMap<>();

	private int bufferSize;
	private final MessageDigest hasher;
	{
		try {
			hasher = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public FileDatabase() {
		this(65536);
	}

	public FileDatabase(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * Adds the specified {@link File} to this database and indexes it.
	 * 
	 * @param f The {@link File} to add.
	 * @throws IOException If an {@link IOException} occurs while hashing one of the
	 *                     files. If this occurs, it might be that an already added
	 *                     file is preventing new files from being added (if the new
	 *                     files have the same length as the already existing file,
	 *                     and attempts to hash the existing file are causing the
	 *                     {@link IOException}).
	 */
	public void addFile(File f) throws IOException {
		long len = f.length();
		Map<FileHash, List<File>> lenmap = rootMap.get(len);

		if (lenmap == null) {
			// No other file with the same length has been encountered yet.

			rootMap.put(len, lenmap = new HashMap<>());

			ArrayList<File> files = new ArrayList<>(1);
			files.add(f);

			// Don't hash this file yet; but store it in the length map under the null key.
			lenmap.put(null, files);
		} else
		// First, check if there's only, exactly one File object inside this length map.
		// If this is the case, then there will be an entry at the null key.
		if (lenmap.containsKey(null)) {
			// Get file object that's already in the map.
			File old = lenmap.get(null).get(0);

			// We need to do two things. First we need to hash both of the files. If this
			// fails, we want to throw an error.
			FileHash oldHash = hash(old), newHash = hash(f);

			// Hashing both files worked. Remove the null entry.
			lenmap.remove(null);

			// Add the old file to the correct place.
			ArrayList<File> files = new ArrayList<>();
			files.add(old);
			lenmap.put(oldHash, files);

			// Check if the old and new file hashes are the same
			if (oldHash.equals(newHash)) {
				// If so, add the new file to the same list as the old one.
				files.add(f);
			} else {
				// Otherwise, add the new file to its correct place.
				files = new ArrayList<>();// Make a new list
				files.add(f);// Add it to the list
				lenmap.put(newHash, files);// Put the list in the map.
			}
		} else {
			// If there is no null key, then we don't have to handle any special cases.
			// First, hash this file:
			FileHash hash = hash(f);

			// Next, check if the hash is already contained somewhere:
			if (lenmap.containsKey(hash)) {
				// It is! This file is a collision. Just add it to the list of files with the
				// same hash:
				lenmap.get(hash).add(f);
			} else {
				// It's not; no collision. Create a new list of files for this hash and add it:
				List<File> files = new ArrayList<>();
				files.add(f);
				lenmap.put(hash, files);
			}
		}
	}

	/**
	 * <p>
	 * Scans through this {@link FileDatabase} and returns a {@link Map} of all the
	 * {@link FileHash hashes} that have duplicate {@link File}s registered. The
	 * {@link Map} contains all of the duplicate {@link File}s as values.
	 * </p>
	 * <p>
	 * The returned {@link Map} is modifiable and is <b>not</b> backed by this
	 * {@link FileDatabase}; changes to this object do not affect the returned
	 * {@link Map}, nor vice versa.
	 * </p>
	 * 
	 * @return A {@link Map} of the {@link File}s that are duplicates, keyed by
	 *         their {@link FileHash}. Every {@link List} of {@link File}s in this
	 *         map is contains {@link File}s all with the same hash, and is stored
	 *         under that hash as the key.
	 */
	public Map<FileHash, List<File>> collectDuplicates() {
		Map<FileHash, List<File>> dupes = new HashMap<>();
		for (Map<FileHash, List<File>> lenmap : rootMap.values())
			for (Entry<FileHash, List<File>> e : lenmap.entrySet())
				if (e.getValue().size() > 1)
					dupes.put(e.getKey(), e.getValue());
		return dupes;
	}

	public FileHash hash(File file) throws IOException {
		hasher.reset();
		byte[] bytes = new byte[bufferSize];
		try (FileInputStream fis = new FileInputStream(file)) {
			while ((fis.read(bytes)) != -1)
				hasher.update(bytes);
		}
		return new FileHash(hasher.digest());
	}

}

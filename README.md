# File Duplication Checker
This is a command line application that quickly checks to see if files are duplicates of each other.

![image](https://user-images.githubusercontent.com/67575219/192043613-35e968d9-976c-4019-a3a3-5039f1b68397.png)

It takes a directory as an argument and scans over that entire directory tree; (it searches files in all subfolders as well as the parent folder). The program detects duplicate files even if they have different names or are in different (sub-)directories.

### Calling
To run the program, just pass a directory as the only argument:
```batch
java -jar dupecheck.jar /some/folder
```

If the directory has spaces in the pathanme:
```batch
java -jar dupecheck.jar "C:/My Documents"
```

## Implementation
The implementation uses hashing. It generates and stores a hash of each file it encounters. (Hashes are small, ~32B, so they are easy to store in memory.) Whenever it encounters another file that has the same contents as one it's already scanned, it compares the hashes and finds a collision.
![image](https://user-images.githubusercontent.com/67575219/192056453-47a7b07d-4dfb-4796-b19f-e4181d781bd8.png)
Hashing is done using `SHA-256`. On a CPU-bound consumer computer, this program can easily hash and compare about `200MB/s` of file data, in some cases reaching as fast as `300MB/s`.

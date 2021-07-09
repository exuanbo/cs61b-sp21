package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.MyUtils.mkdir;
import static gitlet.MyUtils.rmObjectFile;
import static gitlet.Utils.*;

/**
 * Represent the file object.
 */
@SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
public class Blob implements Serializable {

    /**
     * The source file from constructor.
     */
    private final File source;

    /**
     * The content of the source file.
     */
    private final byte[] content;

    /**
     * The SHA1 id generated from the source file content.
     */
    private final String id;

    /**
     * The file of this instance with the path generated from SHA1 id.
     */
    private final File file;

    public Blob(File sourceFile) {
        source = sourceFile;
        content = readContents(source);
        id = sha1(content);
        file = getFileFromId(id);
    }

    /**
     * Get a Blob instance from the file with the SHA1 id.
     *
     * @param id SHA1 id
     * @return Blob instance
     */
    public static Blob fromFile(String id) {
        return readObject(getFileFromId(id), Blob.class);
    }

    /**
     * Get a File instance with the path generated from SHA1 id.
     *
     * @param id SHA1 id
     * @return File instance
     */
    private static File getFileFromId(String id) {
        String dirName = getDirNameFromId(id);
        String fileName = getFileNameFromId(id);
        return join(Repository.OBJECTS_DIR, dirName, fileName);
    }

    /**
     * Get directory name from SHA1 id in the objects folder.
     *
     * @param id SHA1 id
     * @return Directory name
     */
    private static String getDirNameFromId(String id) {
        return id.substring(0, 2);
    }

    /**
     * Get file name from SHA1 id.
     *
     * @param id SHA1 id
     * @return File name
     */
    private static String getFileNameFromId(String id) {
        return id.substring(2);
    }

    /**
     * Save this Blob instance to file in objects folder.
     */
    public void save() {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            mkdir(dir);
        }
        writeObject(file, this);
    }

    /**
     * Delete the actual file in the objects folder.
     */
    public void delete() {
        rmObjectFile(file);
    }

    /**
     * Get the SHA1 id generated from the source file content.
     *
     * @return SHA1 id
     */
    public String getId() {
        return id;
    }

    /**
     * Write the file content back to the source file.
     */
    public void writeContentToSource() {
        writeContents(source, content);
    }
}

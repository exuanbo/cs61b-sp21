package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.MyUtils.getObjectFile;
import static gitlet.MyUtils.saveObjectFile;
import static gitlet.Utils.*;

/**
 * Represent the file object.
 *
 * @author Exuanbo
 */
@SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
public class Blob implements Serializable, Dumpable {

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
        content = readContents(sourceFile);
        id = sha1(sourceFile.getPath(), content);
        file = getObjectFile(id);
    }

    /**
     * Get a Blob instance from the file with the SHA1 id.
     *
     * @param id SHA1 id
     * @return Blob instance
     */
    public static Blob fromFile(String id) {
        return readObject(getObjectFile(id), Blob.class);
    }

    /**
     * Save this Blob instance to file in objects folder.
     */
    public void save() {
        saveObjectFile(file, this);
    }

    /**
     * Write the file content back to the source file.
     */
    public void writeContentToSource() {
        writeContents(source, content);
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
     * Get the Blob file.
     *
     * @return File instance
     */
    public File getFile() {
        return file;
    }

    public void dump() {
        System.out.printf("path: %s\ncontent: %s\n", source.getPath(), new String(content, StandardCharsets.UTF_8));
    }
}

package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;

/**
 * Represent the file object.
 *
 * @author Exuanbo
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
     * Delete the actual file in the objects folder.
     */
    public void delete() {
        rmWithDir(file);
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

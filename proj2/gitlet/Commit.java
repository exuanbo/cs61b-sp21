package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import static gitlet.MyUtils.*;
import static gitlet.Utils.readObject;
import static gitlet.Utils.sha1;

/**
 * The commit object.
 *
 * @author Exuanbo
 */
public class Commit implements Serializable {

    /**
     * The created time.
     */
    private final String time;

    /**
     * The message of this Commit.
     */
    private final String message;

    /**
     * The parent commit SHA1 id.
     */
    private final String parentCommitId;

    /**
     * The tracked files Map with file path as key and SHA1 id as value..
     */
    private final Map<String, String> tracked;

    /**
     * The SHA1 id.
     */
    private final String id;

    /**
     * The file of this instance with the path generated from SHA1 id.
     */
    private final File file;

    public Commit(String msg, String parent, Map<String, String> filesMap) {
        time = new Date().toString();
        message = msg;
        parentCommitId = parent;
        tracked = filesMap;
        id = sha1(time, message, parentCommitId, tracked.toString());
        file = getObjectFile(id);
    }

    /**
     * Get a Commit instance from the file with the SHA1 id.
     *
     * @param id SHA1 id
     * @return Commit instance
     */
    public static Commit fromFile(String id) {
        return readObject(getObjectFile(id), Commit.class);
    }

    /**
     * Save this Commit instance to file in objects folder.
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
     * Get the time when the commit is created.
     *
     * @return Date and time
     */
    public String getTime() {
        return time;
    }

    /**
     * Get the commit message.
     *
     * @return Commit message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the parent commit id.
     *
     * @return Parent commit id.
     */
    public String getParentCommitId() {
        return parentCommitId;
    }

    /**
     * Get the tracked files Map with file path as key and SHA1 id as value.
     *
     * @return Files Map
     */
    public Map<String, String> getTracked() {
        return tracked;
    }

    /**
     * Get the SHA1 id.
     *
     * @return SHA1 id
     */
    public String getId() {
        return id;
    }
}

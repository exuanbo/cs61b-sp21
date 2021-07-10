package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static gitlet.MyUtils.*;
import static gitlet.Utils.readObject;
import static gitlet.Utils.sha1;

/**
 * The commit object.
 *
 * @author Exuanbo
 */
public class Commit implements Serializable, Dumpable {

    /**
     * The created time.
     */
    private final Date time;

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

    public Commit(String msg, String parent, Map<String, String> trackedFilesMap) {
        time = new Date();
        message = msg;
        parentCommitId = parent;
        tracked = trackedFilesMap;
        id = generateId();
        file = getObjectFile(id);
    }

    /**
     * Initial commit.
     */
    public Commit() {
        time = new Date(0);
        message = "initial commit";
        parentCommitId = "0".repeat(40);
        tracked = new HashMap<>();
        id = generateId();
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
     * Generate SHA1 id from timestamp, message, parentCommitId and tracked files Map.
     *
     * @return SHA1 id
     */
    private String generateId() {
        return sha1(getTimestamp(), message, parentCommitId, tracked.toString());
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
     * Get the Date instance when the commit is created.
     *
     * @return Date instance
     */
    public Date getTime() {
        return time;
    }

    /**
     * Get the timestamp.
     *
     * @return Date and time
     */
    public String getTimestamp() {
        // Thu Jan 1 00:00:00 1970 +0000
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return dateFormat.format(time);
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

    /**
     * Get commit log.
     *
     * @return Log content
     */
    public String getLog() {
        return "===\n" +
            "commit " + id + "\n" +
            "Date: " + getTimestamp() + "\n" +
            message + "\n\n";
    }

    public void dump() {
        System.out.println(getLog() + tracked);
    }
}

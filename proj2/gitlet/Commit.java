package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
     * The created date.
     */
    private final Date date;

    /**
     * The message of this Commit.
     */
    private final String message;

    /**
     * The parent commits SHA1 id.
     */
    private final String[] parents;

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

    public Commit(String msg, String[] parentsArray, Map<String, String> trackedFilesMap) {
        date = new Date();
        message = msg;
        parents = parentsArray;
        tracked = trackedFilesMap;
        id = generateId();
        file = getObjectFile(id);
    }

    /**
     * Initial commit.
     */
    public Commit() {
        date = new Date(0);
        message = "initial commit";
        parents = new String[0];
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
     * Generate a SHA1 id from timestamp, message, parents Array and tracked files Map.
     *
     * @return SHA1 id
     */
    private String generateId() {
        return sha1(getTimestamp(), message, Arrays.toString(parents), tracked.toString());
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
    public Date getDate() {
        return date;
    }

    /**
     * Get the timestamp.
     *
     * @return Date and time
     */
    public String getTimestamp() {
        // Thu Jan 1 00:00:00 1970 +0000
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return dateFormat.format(date);
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
     * Get the parent commits id.
     *
     * @return Array of parent commits id.
     */
    public String[] getParents() {
        return parents;
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
     * Get the commit log.
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

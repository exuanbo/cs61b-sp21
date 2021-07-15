package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.MyUtils.rm;
import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;

/**
 * The staging area representation.
 *
 * @author Exuanbo
 */
public class StagingArea implements Serializable {

    /**
     * The added files Map with file path as key and SHA1 id as value.
     */
    private final Map<String, String> added = new HashMap<>();

    /**
     * The removed files Set with file path as key.
     */
    private final Set<String> removed = new HashSet<>();

    /**
     * The tracked files Map with file path as key and SHA1 id as value.
     */
    private transient Map<String, String> tracked;

    /**
     * Get a StagingArea instance from the file INDEX.
     *
     * @return StagingArea instance
     */
    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    /**
     * Save this instance to the file INDEX.
     */
    public void save() {
        writeObject(Repository.INDEX, this);
    }

    /**
     * Get added files Map.
     *
     * @return Map with file path as key and SHA1 id as value.
     */
    public Map<String, String> getAdded() {
        return added;
    }

    /**
     * Get removed files Set.
     *
     * @return Set of files paths.
     */
    public Set<String> getRemoved() {
        return removed;
    }

    /**
     * Set tracked files.
     *
     * @param filesMap Map with file path as key and SHA1 id as value.
     */
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

    /**
     * Tells whether the staging area is clean,
     * which means no file is added, modified, or removed.
     *
     * @return true if is clean
     */
    public boolean isClean() {
        return added.isEmpty() && removed.isEmpty();
    }

    /**
     * Clear the staging area.
     */
    public void clear() {
        added.clear();
        removed.clear();
    }

    /**
     * Perform a commit. Return tracked files Map after commit.
     *
     * @return Map with file path as key and SHA1 id as value.
     */
    public Map<String, String> commit() {
        tracked.putAll(added);
        for (String filePath : removed) {
            tracked.remove(filePath);
        }
        clear();
        return tracked;
    }

    /**
     * Add file to the staging area.
     *
     * @param file File instance
     * @return true if the staging area is changed
     */
    public boolean add(File file) {
        String filePath = file.getPath();

        Blob blob = new Blob(file);
        String blobId = blob.getId();

        String trackedBlobId = tracked.get(filePath);
        if (trackedBlobId != null) {
            if (trackedBlobId.equals(blobId)) {
                if (added.remove(filePath) != null) {
                    return true;
                }
                return removed.remove(filePath);
            }
        }

        String prevBlobId = added.put(filePath, blobId);
        if (prevBlobId != null && prevBlobId.equals(blobId)) {
            return false;
        }

        if (!blob.getFile().exists()) {
            blob.save();
        }
        return true;
    }

    /**
     * Remove file.
     *
     * @param file File instance
     * @return true if the staging area is changed
     */
    public boolean remove(File file) {
        String filePath = file.getPath();

        String addedBlobId = added.remove(filePath);
        if (addedBlobId != null) {
            return true;
        }

        if (tracked.get(filePath) != null) {
            if (file.exists()) {
                rm(file);
            }
            return removed.add(filePath);
        }
        return false;
    }
}

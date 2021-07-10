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
public class StagingArea implements Serializable, Dumpable {

    /**
     * The added files Map with file path as key and SHA1 id as value.
     */
    private final Map<String, String> added = new HashMap<>();

    /**
     * The modified files Map with file path as key and SHA1 id as value.
     */
    private final Map<String, String> modified = new HashMap<>();

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
     * Set tracked files.
     *
     * @param filesMap Files Map from commit instance.
     */
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

    /**
     * Save this instance to the file INDEX.
     */
    public void save() {
        writeObject(Repository.INDEX, this);
    }

    /**
     * Tells whether the staging area is clean,
     * which means no file is added, modified, or removed.
     *
     * @return true if is clean
     */
    public boolean isClean() {
        return added.isEmpty() && modified.isEmpty() && removed.isEmpty();
    }

    /**
     * Perform a commit.
     *
     * @return Tracked files Map
     */
    public Map<String, String> commit() {
        tracked.putAll(added);
        tracked.putAll(modified);
        for (String filePath : removed) {
            tracked.remove(filePath);
        }
        clear();
        return tracked;
    }

    /**
     * Clear the staging area.
     */
    private void clear() {
        added.clear();
        modified.clear();
        removed.clear();
    }

    /**
     * Add file to the staging area.
     *
     * @param file File instance
     * @return true if the staging area is changed
     */
    public boolean addFile(File file) {
        String filePath = file.getPath();

        Blob blob = new Blob(file);
        String blobId = blob.getId();

        boolean isModified = false;

        String trackedBlobId = tracked.get(filePath);
        if (trackedBlobId != null) {
            if (trackedBlobId.equals(blobId)) {
                String modifiedBlobId = modified.remove(filePath);
                if (modifiedBlobId != null) {
                    Blob.fromFile(modifiedBlobId).delete();
                    return true;
                }
                return removed.remove(filePath);
            }
            isModified = true;
        }

        String prevBlobId = (isModified ? modified : added).put(filePath, blobId);
        if (prevBlobId != null) {
            if (prevBlobId.equals(blobId)) {
                return false;
            }
            Blob.fromFile(prevBlobId).delete();
        }
        blob.save();
        return true;
    }

    /**
     * Remove file.
     *
     * @param file File instance
     * @return true if the staging area is changed
     */
    public boolean removeFile(File file) {
        String filePath = file.getPath();

        String addedBlobId = added.remove(filePath);
        if (addedBlobId != null) {
            Blob.fromFile(addedBlobId).delete();
            return true;
        }

        String trackedBlobId = tracked.remove(filePath);
        if (trackedBlobId != null) {
            removed.add(filePath);
            String modifiedBlobId = modified.remove(filePath);
            if (modifiedBlobId != null) {
                Blob.fromFile(modifiedBlobId).delete();
            }
            if (file.exists()) {
                rm(file);
            }
            return true;
        }

        return false;
    }

    public void dump() {
        System.out.printf("added: %s\nmodified: %s\nremoved: %s", added, modified, removed);
    }
}

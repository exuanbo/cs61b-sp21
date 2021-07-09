package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

/**
 * Utility functions.
 *
 * @author Exuanbo
 */
public class MyUtils {

    /**
     * Create a directory from the File object.
     *
     * @param dir File object
     */
    public static void mkdir(File dir) {
        if (!dir.mkdir()) {
            throw new IllegalArgumentException(String.format("mkdir: %s: Failed to create.", dir.getPath()));
        }
    }

    /**
     * Delete the file if exists.
     *
     * @param file File instance
     */
    public static void rm(File file) {
        if (!file.delete()) {
            throw new IllegalArgumentException(String.format("rm: %s: Failed to delete.", file.getPath()));
        }
    }

    /**
     * Delete the file and its directory if no other files exist.
     *
     * @param file File instance
     */
    @SuppressWarnings("ConstantConditions")
    public static void rmWithDir(File file) {
        rm(file);
        File dir = file.getParentFile();
        if (dir.list().length == 0) {
            rm(dir);
        }
    }

    /**
     * Print a message and exit with status code 0.
     *
     * @param msg  String to print
     * @param args Arguments referenced by the format specifiers in the format string
     */
    public static void exit(String msg, Object... args) {
        message(msg, args);
        System.exit(0);
    }

    /**
     * Get a File instance with the path generated from SHA1 id in the objects folder.
     *
     * @param id SHA1 id
     * @return File instance
     */
    public static File getObjectFile(String id) {
        String dirName = getObjectDirName(id);
        String fileName = getObjectFileName(id);
        return join(Repository.OBJECTS_DIR, dirName, fileName);
    }

    /**
     * Get directory name from SHA1 id in the objects folder.
     *
     * @param id SHA1 id
     * @return Directory name
     */
    private static String getObjectDirName(String id) {
        return id.substring(0, 2);
    }

    /**
     * Get file name from SHA1 id.
     *
     * @param id SHA1 id
     * @return File name
     */
    private static String getObjectFileName(String id) {
        return id.substring(2);
    }

    /**
     * Save the serializable object to the file path.
     * Create a parent directory if not exists.
     *
     * @param file File instance
     * @param obj  Serializable object
     */
    public static void saveObjectFile(File file, Serializable obj) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            mkdir(dir);
        }
        writeObject(file, obj);
    }
}

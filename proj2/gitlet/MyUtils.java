package gitlet;

import java.io.File;

import static gitlet.Utils.message;

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
     * Print a message and exit with status code 0.
     *
     * @param msg  String to print
     * @param args Arguments referenced by the format specifiers in the format string
     */
    public static void exit(String msg, Object... args) {
        message(msg, args);
        System.exit(0);
    }
}

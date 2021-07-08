package gitlet;

import java.io.File;

import static gitlet.Utils.message;

public class MyUtils {

    /**
     * Create a directory from the File object.
     *
     * @param dir File object
     */
    public static void createDir(File dir) {
        if (!dir.mkdir()) {
            exit("Directory %s can not be created.", dir.getPath());
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

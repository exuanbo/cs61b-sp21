package gitlet;

import java.io.File;

import static gitlet.MyUtils.createDir;
import static gitlet.MyUtils.exit;
import static gitlet.Utils.join;
import static gitlet.Utils.writeContents;

/**
 * Represents a gitlet repository.
 *
 * @author Exuanbo
 */
public class Repository {

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The index file.
     */
    public static final File INDEX = join(GITLET_DIR, "index");

    /**
     * The HEAD file.
     */
    public static final File HEAD = join(GITLET_DIR, "HEAD");

    /**
     * The refs directory.
     */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");

    /**
     * The heads directory.
     */
    public static final File HEADS_REFS_DIR = join(REFS_DIR, "heads");

    /**
     * The objects directory.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /**
     * Default branch name.
     */
    private static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * HEAD ref prefix.
     */
    private static final String BRANCH_REF_PREFIX = "ref: refs/heads/";

    /**
     * Constructor.
     */
    public Repository() {
    }

    /**
     * Initialize a repository at the current working directory.
     *
     * <pre>
     * .gitlet
     * ├── HEAD
     * ├── objects
     * └── refs
     *     └── heads
     * </pre>
     */
    public static void init() {
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }

        createDir(GITLET_DIR);
        createDir(REFS_DIR);
        createDir(HEADS_REFS_DIR);
        createDir(OBJECTS_DIR);

        changeHEAD(DEFAULT_BRANCH_NAME);
    }

    /**
     * Change HEAD to branch.
     *
     * @param branchName Branch name
     */
    private static void changeHEAD(String branchName) {
        writeContents(HEAD, BRANCH_REF_PREFIX + branchName);
    }

    /**
     * Exit if the repository at the current working directory is not initialized.
     */
    public static void checkWorkingDir() {
        if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * Add file to stage area.
     *
     * @param fileName File name
     */
    public void add(String fileName) {
    }
}

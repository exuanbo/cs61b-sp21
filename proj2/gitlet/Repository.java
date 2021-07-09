package gitlet;

import java.io.File;
import java.nio.file.Paths;

import static gitlet.MyUtils.exit;
import static gitlet.MyUtils.mkdir;
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
     * The staging area instance. Initialized in the constructor.
     */
    private final StagingArea stagingArea;

    public Repository() {
        if (INDEX.exists()) {
            stagingArea = StagingArea.fromFile();
        } else {
            stagingArea = new StagingArea();
        }
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

        mkdir(GITLET_DIR);
        mkdir(REFS_DIR);
        mkdir(HEADS_REFS_DIR);
        mkdir(OBJECTS_DIR);

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
     * Add file to the staging area.
     *
     * @param fileName File name
     */
    public void add(String fileName) {
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        if (!file.exists()) {
            exit("File does not exist.");
        }
        if (stagingArea.addFile(file)) {
            stagingArea.save();
        }
    }
}

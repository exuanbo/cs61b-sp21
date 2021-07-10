package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import static gitlet.MyUtils.exit;
import static gitlet.MyUtils.mkdir;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *
 * @author Exuanbo
 */
public class Repository {

    /**
     * The current working directory.
     */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    private static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The index file.
     */
    public static final File INDEX = join(GITLET_DIR, "index");

    /**
     * The objects directory.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /**
     * The HEAD file.
     */
    private static final File HEAD = join(GITLET_DIR, "HEAD");

    /**
     * The refs directory.
     */
    private static final File REFS_DIR = join(GITLET_DIR, "refs");

    /**
     * The heads directory.
     */
    private static final File BRANCH_HEADS_DIR = join(REFS_DIR, "heads");

    /**
     * Default branch name.
     */
    private static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * HEAD ref prefix.
     */
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";

    /**
     * The staging area instance. Initialized in the constructor.
     */
    private final StagingArea stagingArea;

    /**
     * The current branch name.
     */
    private final String currentBranchName;

    /**
     * The commit that HEAD points to.
     */
    private final Commit HEADCommit;

    public Repository() {
        if (INDEX.exists()) {
            stagingArea = StagingArea.fromFile();
        } else {
            stagingArea = new StagingArea();
        }
        currentBranchName = getCurrentBranchName();
        HEADCommit = getHeadCommit(currentBranchName);
        stagingArea.setTracked(HEADCommit.getTracked());
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
        mkdir(BRANCH_HEADS_DIR);
        mkdir(OBJECTS_DIR);
        changeCurrentBranch(DEFAULT_BRANCH_NAME);
        createInitialCommit();
    }

    /**
     * Get current branch name from HEAD file.
     *
     * @return Name of the branch
     */
    private static String getCurrentBranchName() {
        String HEADContent = readContentsAsString(HEAD);
        return HEADContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * Get branch head ref file in refs/heads folder.
     *
     * @param branchName Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branchName) {
        return join(BRANCH_HEADS_DIR, branchName);
    }

    /**
     * Change current branch.
     *
     * @param branchName Name of the branch
     */
    private static void changeCurrentBranch(String branchName) {
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    /**
     * Create an initial commit.
     */
    private static void createInitialCommit() {
        Commit initialCommit = new Commit();
        initialCommit.save();
        changeBranchHead(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    /**
     * Change branch head.
     *
     * @param branchName Name of the branch
     * @param id         Commit SHA1 id
     */
    private static void changeBranchHead(String branchName, String id) {
        File branchHeadFile = getBranchHeadFile(branchName);
        writeContents(branchHeadFile, id);
    }

    /**
     * Get a File instance from CWD by the name.
     *
     * @param fileName Name of the file
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        return file;
    }

    /**
     * Add file to the staging area.
     *
     * @param fileName Name of the file
     */
    public void add(String fileName) {
        File file = getFileFromCWD(fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }
        if (stagingArea.addFile(file)) {
            stagingArea.save();
        }
    }

    /**
     * Perform a commit with message.
     *
     * @param message Commit message
     */
    public void commit(String message) {
        if (stagingArea.isClean()) {
            exit("No changes added to the commit.");
        }
        Map<String, String> newTracked = stagingArea.commit();
        stagingArea.save();
        Commit newCommit = new Commit(message, new String[]{HEADCommit.getId()}, newTracked);
        newCommit.save();
        changeBranchHead(currentBranchName, newCommit.getId());
    }

    /**
     * Remove file.
     *
     * @param fileName Name of the file
     */
    public void remove(String fileName) {
        File file = getFileFromCWD(fileName);
        if (stagingArea.removeFile(file)) {
            stagingArea.save();
        } else {
            exit("No reason to remove the file.");
        }
    }
}

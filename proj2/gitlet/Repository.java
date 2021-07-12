package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

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
    private final String currentBranch;

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
        currentBranch = getCurrentBranch();
        HEADCommit = getHeadCommit(currentBranch);
        stagingArea.setTracked(HEADCommit.getTrackedFiles());
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
     * Print all commit logs ever made.
     */
    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        forEachCommitInOrder(commit -> logBuilder.append(commit.getLog()).append("\n"));
        System.out.print(logBuilder);
    }

    /**
     * Print all commits that have the exact message.
     *
     * @param message Content of the message
     */
    public static void find(String message) {
        StringBuilder resultBuilder = new StringBuilder();
        forEachCommitInOrder(commit -> {
            if (commit.getMessage().equals(message)) {
                resultBuilder.append(commit.getId()).append("\n");
            }
        });
        if (resultBuilder.length() == 0) {
            exit("Found no commit with that message.");
        }
        System.out.print(resultBuilder);
    }

    /**
     * Get current branch name from HEAD file.
     *
     * @return Name of the branch
     */
    private static String getCurrentBranch() {
        String HEADContent = readContentsAsString(HEAD);
        return HEADContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    }

    /**
     * Get head commit of the branch.
     *
     * @param branch Name of the branch
     * @return Commit instance
     */
    private static Commit getHeadCommit(String branch) {
        File branchHeadFile = getBranchHeadFile(branch);
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * Get branch head ref file in refs/heads folder.
     *
     * @param branch Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branch) {
        return join(BRANCH_HEADS_DIR, branch);
    }

    /**
     * Change current branch.
     *
     * @param branch Name of the branch
     */
    private static void changeCurrentBranch(String branch) {
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branch);
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
     * @param branch   Name of the branch
     * @param commitId Commit SHA1 id
     */
    private static void changeBranchHead(String branch, String commitId) {
        File branchHeadFile = getBranchHeadFile(branch);
        writeContents(branchHeadFile, commitId);
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
     * Iterate all commits in the order of created date
     * and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        Queue<Commit> commitsToLog = new PriorityQueue<>((a, b) -> b.getDate().compareTo(a.getDate()));
        Set<String> checkedCommitIds = new HashSet<>();

        File[] branchHeadFiles = BRANCH_HEADS_DIR.listFiles();
        Arrays.sort(branchHeadFiles, Comparator.comparing(File::getName));

        for (File branchHeadFile : branchHeadFiles) {
            String branchHeadCommitId = readContentsAsString(branchHeadFile);
            if (checkedCommitIds.contains(branchHeadCommitId)) {
                continue;
            }
            checkedCommitIds.add(branchHeadCommitId);
            Commit branchHeadCommit = Commit.fromFile(branchHeadCommitId);
            commitsToLog.add(branchHeadCommit);
        }

        while (true) {
            Commit latestCommit = commitsToLog.poll();
            cb.accept(latestCommit);
            String[] parentCommitIds = latestCommit.getParents();
            if (parentCommitIds.length == 0) {
                break;
            }
            for (String parentCommitId : parentCommitIds) {
                if (checkedCommitIds.contains(parentCommitId)) {
                    continue;
                }
                checkedCommitIds.add(parentCommitId);
                Commit parentCommit = Commit.fromFile(parentCommitId);
                commitsToLog.add(parentCommit);
            }
        }
    }

    /**
     * Append lines of file name in order from files paths Set to StringBuilder.
     *
     * @param stringBuilder       StringBuilder instance
     * @param filePathsCollection Collection of file paths
     */
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        String[] filePaths = filePathsCollection.toArray(String[]::new);
        Arrays.sort(filePaths);
        for (String filePath : filePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
        stringBuilder.append("\n");
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
        Map<String, String> newTrackedFilesMap = stagingArea.commit();
        stagingArea.save();
        Commit newCommit = new Commit(message, new String[]{HEADCommit.getId()}, newTrackedFilesMap);
        newCommit.save();
        changeBranchHead(currentBranch, newCommit.getId());
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

    /**
     * Print log of the current branch.
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        Commit currentCommit = HEADCommit;
        while (true) {
            logBuilder.append(currentCommit.getLog()).append("\n");
            String[] parentCommitIds = currentCommit.getParents();
            if (parentCommitIds.length == 0) {
                break;
            }
            String firstParentCommitId = parentCommitIds[0];
            currentCommit = Commit.fromFile(firstParentCommitId);
        }
        System.out.print(logBuilder);
    }

    /**
     * Print the status.
     */
    @SuppressWarnings("ConstantConditions")
    public void status() {
        StringBuilder statusBuilder = new StringBuilder();

        // branches
        statusBuilder.append("=== Branches ===").append("\n");
        statusBuilder.append("*").append(currentBranch).append("\n");
        String[] branches = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch));
        Arrays.sort(branches);
        for (String branch : branches) {
            statusBuilder.append(branch).append("\n");
        }
        statusBuilder.append("\n");
        // end

        Map<String, String> trackedFilesMap = stagingArea.getTracked();
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Map<String, String> modifiedFilesMap = stagingArea.getModified();
        Set<String> removedFilePathsSet = stagingArea.getRemoved();

        // staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        List<String> stagedFilePaths = new ArrayList<>();
        stagedFilePaths.addAll(addedFilesMap.keySet());
        stagedFilePaths.addAll(modifiedFilesMap.keySet());
        stagedFilePaths.sort(String::compareTo);
        for (String filePath : stagedFilePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName).append("\n");
        }
        statusBuilder.append("\n");
        // end

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        // end

        Map<String, String> currentFilesMap = new HashMap<>();

        File[] currentFiles = CWD.listFiles(File::isFile);
        for (File file : currentFiles) {
            String filePath = file.getPath();
            String blobId = Blob.generateId(file);
            currentFilesMap.put(filePath, blobId);
        }

        Set<String> modifiedNotStageFilePaths = new HashSet<>();
        Set<String> deletedNotStageFilePaths = new HashSet<>();
        Set<String> untrackedFilePaths = new HashSet<>();

        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();
            String currentFileBlobId = currentFilesMap.get(filePath);
            if (currentFileBlobId != null) {
                if (removedFilePathsSet.contains(filePath)) {
                    untrackedFilePaths.add(filePath);
                } else {
                    if (currentFileBlobId.equals(blobId)) {
                        if (modifiedFilesMap.containsKey(filePath)) {
                            modifiedNotStageFilePaths.add(filePath);
                        }
                    } else {
                        String modifiedFileId = modifiedFilesMap.get(filePath);
                        if (!currentFileBlobId.equals(modifiedFileId)) {
                            modifiedNotStageFilePaths.add(filePath);
                        }
                    }
                }
                currentFilesMap.remove(filePath);
            } else {
                if (!removedFilePathsSet.contains(filePath)) {
                    deletedNotStageFilePaths.add(filePath);
                }
            }
        }

        String[] currentFilePaths = currentFilesMap.keySet().toArray(String[]::new);
        for (String filePath : currentFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String addedBlobId = addedFilesMap.get(filePath);
            if (addedBlobId != null) {
                if (!addedBlobId.equals(blobId)) {
                    modifiedNotStageFilePaths.add(filePath);
                }
                addedFilesMap.remove(filePath);
            } else {
                untrackedFilePaths.add(filePath);
            }
            currentFilesMap.remove(filePath);
        }

        for (String filePath : addedFilesMap.keySet()) {
            if (!currentFilesMap.containsKey(filePath)) {
                deletedNotStageFilePaths.add(filePath);
            }
        }

        // modifications not staged for commit
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        List<String> pathsOfFileWithModificationsNotStaged = new ArrayList<>();
        pathsOfFileWithModificationsNotStaged.addAll(modifiedNotStageFilePaths);
        pathsOfFileWithModificationsNotStaged.addAll(deletedNotStageFilePaths);
        pathsOfFileWithModificationsNotStaged.sort(String::compareTo);
        for (String filePath : pathsOfFileWithModificationsNotStaged) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (modifiedNotStageFilePaths.contains(filePath)) {
                statusBuilder.append(" (modified)");
            } else {
                statusBuilder.append(" (deleted)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");
        // end

        // untracked files
        statusBuilder.append("=== Untracked Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, untrackedFilePaths);
        // end

        System.out.print(statusBuilder);
    }
}

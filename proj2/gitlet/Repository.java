package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.MyUtils.*;
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
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getHeadCommit(branchHeadFile);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchHeadFile File instance
     * @return Commit instance
     */
    private static Commit getHeadCommit(File branchHeadFile) {
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
     * @param commitId   Commit SHA1 id
     */
    private static void changeBranchHead(String branchName, String commitId) {
        File branchHeadFile = getBranchHeadFile(branchName);
        changeBranchHead(branchHeadFile, commitId);
    }

    /**
     * Change branch head.
     *
     * @param branchHeadFile File instance
     * @param commitId       Commit SHA1 id
     */
    private static void changeBranchHead(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
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
     * Get a Map of file paths and their SHA1 id from CWD.
     *
     * @return Map with file path as key and SHA1 id as value
     */
    @SuppressWarnings("ConstantConditions")
    private static Map<String, String> getCurrentFilesMap() {
        File[] currentFiles = CWD.listFiles(File::isFile);
        return getFilesMap(currentFiles);
    }

    /**
     * Get a Map of file paths and their SHA1 id.
     *
     * @return Map with file path as key and SHA1 id as value
     */
    private static Map<String, String> getFilesMap(File[] files) {
        Map<String, String> filesMap = new HashMap<>();
        for (File file : files) {
            String filePath = file.getPath();
            String blobId = Blob.generateId(file);
            filesMap.put(filePath, blobId);
        }
        return filesMap;
    }

    /**
     * Append lines of file name in order from files paths Set to StringBuilder.
     *
     * @param stringBuilder       StringBuilder instance
     * @param filePathsCollection Collection of file paths
     */
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        List<String> filePathsList = new ArrayList<>(filePathsCollection);
        appendFileNamesInOrder(stringBuilder, filePathsList);
    }

    /**
     * Append lines of file name in order from files paths Set to StringBuilder.
     *
     * @param stringBuilder StringBuilder instance
     * @param filePathsList List of file paths
     */
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, List<String> filePathsList) {
        filePathsList.sort(String::compareTo);
        for (String filePath : filePathsList) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
    }

    /**
     * Get the whole commit id. Exit with message if it does not exist.
     *
     * @param commitId Abbreviate or Whole commit SHA1 id
     * @return Whole commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static String getActualCommitId(String commitId) {
        if (commitId.length() < UID_LENGTH) {
            if (commitId.length() < 4) {
                exit("Commit id should contain at least 4 characters.");
            }
            String objectDirName = getObjectDirName(commitId);
            File objectDir = join(OBJECTS_DIR, objectDirName);
            if (!objectDir.exists()) {
                exit("No commit with that id exists.");
            }
            String objectFileNamePrefix = getObjectFileName(commitId);
            boolean isFound = false;
            File[] objectFiles = objectDir.listFiles();
            for (File objectFile : objectFiles) {
                String objectFileName = objectFile.getName();
                if (objectFileName.startsWith(objectFileNamePrefix) && isFileInstanceOf(objectFile, Commit.class)) {
                    if (isFound) {
                        exit("More than 1 commit has the same id prefix.");
                    }
                    commitId = objectDirName + objectFileName;
                    isFound = true;
                }
            }
            if (!isFound) {
                exit("No commit with that id exists.");
            }
        } else {
            if (!getObjectFile(commitId).exists()) {
                exit("No commit with that id exists.");
            }
        }
        return commitId;
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
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");
        // end

        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.getTracked();
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Map<String, String> modifiedFilesMap = stagingArea.getModified();
        Set<String> removedFilePathsSet = stagingArea.getRemoved();

        // staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        List<String> stagedFilePaths = new ArrayList<>();
        stagedFilePaths.addAll(addedFilesMap.keySet());
        stagedFilePaths.addAll(modifiedFilesMap.keySet());
        appendFileNamesInOrder(statusBuilder, stagedFilePaths);
        statusBuilder.append("\n");
        // end

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");
        // end

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
        statusBuilder.append("\n");
        // end

        System.out.print(statusBuilder);
    }

    /**
     * Checkout file from HEAD commit.
     *
     * @param fileName Name of the file
     */
    public void checkout(String fileName) {
        String filePath = getFileFromCWD(fileName).getPath();
        if (!HEADCommit.restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * Checkout file from specific commit id.
     *
     * @param commitId Commit SHA1 id
     * @param fileName Name of the file
     */
    public void checkout(String commitId, String fileName) {
        commitId = getActualCommitId(commitId);
        String filePath = getFileFromCWD(fileName).getPath();
        if (!Commit.fromFile(commitId).restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * Checkout to branch.
     *
     * @param branchName Name of the branch
     */
    public void checkoutBranch(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        if (!branchHeadFile.exists()) {
            exit("No such branch exists.");
        }
        if (branchName.equals(currentBranch)) {
            exit("No need to checkout the current branch.");
        }
        String branchHeadCommitId = readContentsAsString(branchHeadFile);
        checkoutCommit(branchHeadCommitId);
        changeCurrentBranch(branchName);
    }

    /**
     * Checkout to specific commit.
     *
     * @param commitId Commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private void checkoutCommit(String commitId) {
        File[] currentFiles = CWD.listFiles(File::isFile);
        Map<String, String> currentFilesMap = getFilesMap(currentFiles);
        Map<String, String> trackedFilesMap = HEADCommit.getTracked();
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Set<String> removedFilePathsSet = stagingArea.getRemoved();

        Set<String> untrackedFilePaths = new HashSet<>();

        for (String filePath : currentFilesMap.keySet()) {
            if (trackedFilesMap.containsKey(filePath)) {
                if (removedFilePathsSet.contains(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            } else {
                if (!addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            }
        }

        Commit targetCommit = Commit.fromFile(commitId);
        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();

        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        stagingArea.clear();
        stagingArea.save();
        for (File file : currentFiles) {
            rm(file);
        }
        targetCommit.restoreAllTracked();
    }

    /**
     * Create a new branch.
     *
     * @param branchName Name of the branch
     */
    public void branch(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        if (branchHeadFile.exists()) {
            exit("A branch with that name already exists.");
        }
        changeBranchHead(branchHeadFile, HEADCommit.getId());
    }

    /**
     * Delete the branch.
     *
     * @param branchName Name of the branch
     */
    public void rmBranch(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        if (!branchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (branchName.equals(currentBranch)) {
            exit("Cannot remove the current branch.");
        }
        rm(branchHeadFile);
    }

    /**
     * Reset to commit with the id.
     *
     * @param commitId Commit SHA1 id
     */
    public void reset(String commitId) {
        commitId = getActualCommitId(commitId);
        checkoutCommit(commitId);
        changeBranchHead(currentBranch, commitId);
    }
}

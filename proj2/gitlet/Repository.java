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
     * Default branch name.
     */
    private static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * HEAD ref prefix.
     */
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";

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
     * Files in the current working directory.
     */
    private static final Lazy<File[]> currentFiles = lazy(() -> CWD.listFiles(File::isFile));

    /**
     * The current branch name.
     */
    private final Lazy<String> currentBranch = lazy(() -> {
        String HEADContent = readContentsAsString(HEAD);
        return HEADContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    });

    /**
     * The commit that HEAD points to.
     */
    private final Lazy<Commit> HEADCommit = lazy(() -> getBranchHeadCommit(currentBranch.get()));

    /**
     * The staging area instance. Initialized in the constructor.
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea s;
        if (INDEX.exists()) {
            s = StagingArea.fromFile();
        } else {
            s = new StagingArea();
        }
        s.setTracked(HEADCommit.get().getTracked());
        return s;
    });

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
        setCurrentBranch(DEFAULT_BRANCH_NAME);
        createInitialCommit();
    }

    /**
     * Print all commit logs ever made.
     */
    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        // As the project spec goes, the runtime should be O(N) where N is the number of commits ever made.
        // But here I choose to log the commits in the order of created date, which has a runtime of O(NlogN).
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
        forEachCommit(commit -> {
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
     * Set current branch.
     *
     * @param branchName Name of the branch
     */
    private static void setCurrentBranch(String branchName) {
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchHeadFile File instance
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(File branchHeadFile) {
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * Set branch head.
     *
     * @param branchName Name of the branch
     * @param commitId   Commit SHA1 id
     */
    private static void setBranchHeadCommit(String branchName, String commitId) {
        File branchHeadFile = getBranchHeadFile(branchName);
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * Set branch head.
     *
     * @param branchHeadFile File instance
     * @param commitId       Commit SHA1 id
     */
    private static void setBranchHeadCommit(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
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
     * Create an initial commit.
     */
    private static void createInitialCommit() {
        Commit initialCommit = new Commit();
        initialCommit.save();
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    /**
     * Iterate all commits in the order of created date
     * and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsPriorityQueue = new PriorityQueue<>(commitComparator);
        forEachCommit(cb, commitsPriorityQueue);
    }

    /**
     * Iterate all commits and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    private static void forEachCommit(Consumer<Commit> cb) {
        Queue<Commit> commitsQueue = new ArrayDeque<>();
        forEachCommit(cb, commitsQueue);
    }

    /**
     * Helper method to iterate all commits.
     *
     * @param cb                 Callback function executed on the current commit
     * @param queueToHoldCommits New Queue instance to hold the commits while iterating
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommit(Consumer<Commit> cb, Queue<Commit> queueToHoldCommits) {
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
            queueToHoldCommits.add(branchHeadCommit);
        }

        while (true) {
            Commit nextCommit = queueToHoldCommits.poll();
            cb.accept(nextCommit);
            String[] parentCommitIds = nextCommit.getParents();
            if (parentCommitIds.length == 0) {
                break;
            }
            for (String parentCommitId : parentCommitIds) {
                if (checkedCommitIds.contains(parentCommitId)) {
                    continue;
                }
                checkedCommitIds.add(parentCommitId);
                Commit parentCommit = Commit.fromFile(parentCommitId);
                queueToHoldCommits.add(parentCommit);
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
    private static Map<String, String> getCurrentFilesMap() {
        Map<String, String> filesMap = new HashMap<>();
        for (File file : currentFiles.get()) {
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
     * Get the id of the latest common ancestor of the two commits.
     *
     * @param commitA Commit instance
     * @param commitB Commit instance
     * @return Commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static Commit getLatestCommonAncestorCommit(Commit commitA, Commit commitB) {
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsQueue = new PriorityQueue<>(commitComparator);
        commitsQueue.add(commitA);
        commitsQueue.add(commitB);
        Set<String> checkedCommitIds = new HashSet<>();
        while (true) {
            Commit latestCommit = commitsQueue.poll();
            String[] parentCommitIds = latestCommit.getParents();
            for (String parentCommitId : parentCommitIds) {
                Commit parentCommit = Commit.fromFile(parentCommitId);
                if (checkedCommitIds.contains(parentCommitId)) {
                    return parentCommit;
                }
                commitsQueue.add(parentCommit);
                checkedCommitIds.add(parentCommitId);
            }
        }
    }

    /**
     * Merge the conflicted blob content and return a new String.
     *
     * @param currentBlobId Current Blob SHA1 id
     * @param targetBlobId  Target Blob SHA1 id
     * @return New content
     */
    private static String getConflictContent(String currentBlobId, String targetBlobId) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<<<<<<< HEAD").append("\n");
        if (currentBlobId != null) {
            Blob currentBlob = Blob.fromFile(currentBlobId);
            contentBuilder.append(currentBlob.getContentAsString());
        }
        contentBuilder.append("=======").append("\n");
        if (targetBlobId != null) {
            Blob targetBlob = Blob.fromFile(targetBlobId);
            contentBuilder.append(targetBlob.getContentAsString());
        }
        contentBuilder.append(">>>>>>>");
        return contentBuilder.toString();
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
        if (stagingArea.get().addFile(file)) {
            stagingArea.get().save();
        }
    }

    /**
     * Perform a commit with message.
     *
     * @param message Commit message
     */
    public void commit(String message) {
        commit(message, null);
    }

    /**
     * Perform a commit with message and two parents.
     *
     * @param message      Commit message
     * @param secondParent Second parent Commit SHA1 id
     */
    private void commit(String message, String secondParent) {
        if (stagingArea.get().isClean()) {
            exit("No changes added to the commit.");
        }
        Map<String, String> newTrackedFilesMap = stagingArea.get().commit();
        stagingArea.get().save();
        String[] parents = secondParent == null
            ? new String[]{HEADCommit.get().getId()}
            : new String[]{HEADCommit.get().getId(), secondParent};
        Commit newCommit = new Commit(message, parents, newTrackedFilesMap);
        newCommit.save();
        setBranchHeadCommit(currentBranch.get(), newCommit.getId());
    }

    /**
     * Remove file.
     *
     * @param fileName Name of the file
     */
    public void remove(String fileName) {
        File file = getFileFromCWD(fileName);
        if (stagingArea.get().removeFile(file)) {
            stagingArea.get().save();
        } else {
            exit("No reason to remove the file.");
        }
    }

    /**
     * Print log of the current branch.
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        Commit currentCommit = HEADCommit.get();
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
        statusBuilder.append("*").append(currentBranch.get()).append("\n");
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch.get()));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");
        // end

        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();

        // staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, addedFilesMap.keySet());
        statusBuilder.append("\n");
        // end

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");
        // end

        // modifications not staged for commit
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        List<String> modifiedNotStageFilePaths = new ArrayList<>();
        Set<String> deletedNotStageFilePaths = new HashSet<>();

        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();

        trackedFilesMap.putAll(addedFilesMap);
        for (String filePath : removedFilePathsSet) {
            trackedFilesMap.remove(filePath);
        }

        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();
            String currentFileBlobId = currentFilesMap.get(filePath);
            if (currentFileBlobId != null) {
                if (!currentFileBlobId.equals(blobId)) {
                    // 1. Tracked in the current commit, changed in the working directory, but not staged; or
                    // 2. Staged for addition, but with different contents than in the working directory.
                    modifiedNotStageFilePaths.add(filePath);
                }
                currentFilesMap.remove(filePath);
            } else {
                // 3. Staged for addition, but deleted in the working directory; or
                // 4. Not staged for removal, but tracked in the current commit and deleted from the working directory.
                modifiedNotStageFilePaths.add(filePath);
                deletedNotStageFilePaths.add(filePath);
            }
        }

        modifiedNotStageFilePaths.sort(String::compareTo);

        for (String filePath : modifiedNotStageFilePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (deletedNotStageFilePaths.contains(filePath)) {
                statusBuilder.append(" (deleted)");
            } else {
                statusBuilder.append(" (modified)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");
        // end

        // untracked files
        statusBuilder.append("=== Untracked Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, currentFilesMap.keySet());
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
        if (!HEADCommit.get().restoreTracked(filePath)) {
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
     * @param targetBranchName Name of the target branch
     */
    public void checkoutBranch(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("No such branch exists.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("No need to checkout the current branch.");
        }
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        checkUntracked(targetBranchHeadCommit);
        checkoutCommit(targetBranchHeadCommit);
        setCurrentBranch(targetBranchName);
    }

    /**
     * Checkout to specific commit.
     *
     * @param targetCommit Commit instance
     */
    private void checkoutCommit(Commit targetCommit) {
        stagingArea.get().clear();
        stagingArea.get().save();
        for (File file : currentFiles.get()) {
            rm(file);
        }
        targetCommit.restoreAllTracked();
    }

    /**
     * Exit with message if target commit would overwrite the untracked files.
     *
     * @param targetCommit Commit SHA1 id
     */
    private void checkUntracked(Commit targetCommit) {
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();

        List<String> untrackedFilePaths = new ArrayList<>();

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

        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();

        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * Create a new branch.
     *
     * @param newBranchName Name of the new branch
     */
    public void branch(String newBranchName) {
        File newBranchHeadFile = getBranchHeadFile(newBranchName);
        if (newBranchHeadFile.exists()) {
            exit("A branch with that name already exists.");
        }
        setBranchHeadCommit(newBranchHeadFile, HEADCommit.get().getId());
    }

    /**
     * Delete the branch.
     *
     * @param targetBranchName Name of the target branch
     */
    public void rmBranch(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot remove the current branch.");
        }
        rm(targetBranchHeadFile);
    }

    /**
     * Reset to commit with the id.
     *
     * @param commitId Commit SHA1 id
     */
    public void reset(String commitId) {
        commitId = getActualCommitId(commitId);
        Commit targetCommit = Commit.fromFile(commitId);
        checkUntracked(targetCommit);
        checkoutCommit(targetCommit);
        setBranchHeadCommit(currentBranch.get(), commitId);
    }

    /**
     * Merge branch.
     *
     * @param targetBranchName Name of the target branch
     */
    public void merge(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot merge a branch with itself.");
        }
        if (!stagingArea.get().isClean()) {
            exit("You have uncommitted changes.");
        }
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        checkUntracked(targetBranchHeadCommit);

        Commit lcaCommit = getLatestCommonAncestorCommit(HEADCommit.get(), targetBranchHeadCommit);
        String lcaCommitId = lcaCommit.getId();

        if (lcaCommitId.equals(targetBranchHeadCommit.getId())) {
            exit("Given branch is an ancestor of the current branch.");
        }
        if (lcaCommitId.equals(HEADCommit.get().getId())) {
            checkoutCommit(targetBranchHeadCommit);
            setCurrentBranch(targetBranchName);
            exit("Current branch fast-forwarded.");
        }

        boolean hasConflict = false;

        Map<String, String> HEADCommitTrackedFilesMap = HEADCommit.get().getTracked();
        Map<String, String> targetBranchHeadCommitTrackedFilesMap = targetBranchHeadCommit.getTracked();
        Map<String, String> lcaCommitTrackedFilesMap = lcaCommit.getTracked();

        for (Map.Entry<String, String> entry : lcaCommitTrackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            File file = new File(filePath);
            String blobId = entry.getValue();

            String targetBranchHeadCommitBlobId = targetBranchHeadCommitTrackedFilesMap.get(filePath);
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(filePath);

            if (targetBranchHeadCommitBlobId != null) { // exists in the target branch
                if (!targetBranchHeadCommitBlobId.equals(blobId)) { // modified in the target branch
                    if (HEADCommitBlobId != null) { // exists in the current branch
                        if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                            // case 1
                            Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                            stagingArea.get().addFile(file);
                        } else { // modified in the current branch
                            if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                                // case 8
                                hasConflict = true;
                                String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                                writeContents(file, conflictContent);
                                stagingArea.get().addFile(file);
                            } // else modified in the same ways
                            // case 3
                        }
                    } else { // deleted in current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(null, targetBranchHeadCommitBlobId);
                        writeContents(file, conflictContent);
                        stagingArea.get().addFile(file);
                    }
                } // else not modified in the target branch
                // case 2, case 7
            } else { // deleted in the target branch
                if (HEADCommitBlobId != null) { // exists in the current branch
                    if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                        // case 6
                        stagingArea.get().removeFile(file);
                    } else { // modified in the current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(HEADCommitBlobId, null);
                        writeContents(file, conflictContent);
                        stagingArea.get().addFile(file);
                    }
                } // else deleted in both branches
                // case 3
            }

            HEADCommitTrackedFilesMap.remove(filePath);
            targetBranchHeadCommitTrackedFilesMap.remove(filePath);
        }

        for (Map.Entry<String, String> entry : targetBranchHeadCommitTrackedFilesMap.entrySet()) {
            String targetBranchHeadCommitFilePath = entry.getKey();
            File targetBranchHeadCommitFile = new File(targetBranchHeadCommitFilePath);
            String targetBranchHeadCommitBlobId = entry.getValue();

            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(targetBranchHeadCommitFilePath);

            if (HEADCommitBlobId != null) { // added in both branches
                if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                    // case 8
                    hasConflict = true;
                    String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                    writeContents(targetBranchHeadCommitFile, conflictContent);
                    stagingArea.get().addFile(targetBranchHeadCommitFile);
                } // else modified in the same ways
                // case 3
            } else { // only added in the target branch
                // case 5
                Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                stagingArea.get().addFile(targetBranchHeadCommitFile);
            }
        }

        String newCommitMessage = "Merged " + targetBranchName + " into " + currentBranch.get() + ".";
        commit(newCommitMessage, targetBranchHeadCommit.getId());

        if (hasConflict) {
            message("Encountered a merge conflict.");
        }
    }
}

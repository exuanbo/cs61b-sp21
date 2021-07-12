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
     * Iterate all commits in the order of created date
     * and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        Queue<Commit> commitsToLog = new PriorityQueue<>((a, b) -> b.getDate().compareTo(a.getDate()));
        Set<String> commitIds = new HashSet<>();

        File[] branchHeads = BRANCH_HEADS_DIR.listFiles();
        Arrays.sort(branchHeads, Comparator.comparing(File::getName));

        for (File headFile : branchHeads) {
            String headId = readContentsAsString(headFile);
            if (commitIds.contains(headId)) {
                continue;
            }
            commitIds.add(headId);
            Commit headCommit = Commit.fromFile(headId);
            commitsToLog.add(headCommit);
        }

        while (true) {
            Commit latestCommit = commitsToLog.poll();
            cb.accept(latestCommit);
            String[] parents = latestCommit.getParents();
            if (parents.length == 0) {
                break;
            }
            for (String parentId : parents) {
                if (commitIds.contains(parentId)) {
                    continue;
                }
                commitIds.add(parentId);
                Commit parentCommit = Commit.fromFile(parentId);
                commitsToLog.add(parentCommit);
            }
        }
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

    /**
     * Print log of the current branch.
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        Commit currentCommit = HEADCommit;
        while (true) {
            logBuilder.append(currentCommit.getLog()).append("\n");
            String[] parents = currentCommit.getParents();
            if (parents.length == 0) {
                break;
            }
            String firstParentId = parents[0];
            currentCommit = Commit.fromFile(firstParentId);
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
        statusBuilder.append("*").append(currentBranchName).append("\n");
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranchName));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");
        // end branches

        Map<String, String> trackedFilesMap = stagingArea.getTracked();
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Map<String, String> modifiedFilesMap = stagingArea.getModified();
        Set<String> removedFilesPathsSet = stagingArea.getRemoved();

        // staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        List<String> stagedFilesPaths = new ArrayList<>();
        stagedFilesPaths.addAll(addedFilesMap.keySet());
        stagedFilesPaths.addAll(modifiedFilesMap.keySet());
        stagedFilesPaths.sort(String::compareTo);
        for (String filePath : stagedFilesPaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName).append("\n");
        }
        statusBuilder.append("\n");
        // end staged files

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        String[] removedFilesPaths = removedFilesPathsSet.toArray(String[]::new);
        Arrays.sort(removedFilesPaths);
        for (String filePath : removedFilesPaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName).append("\n");
        }
        statusBuilder.append("\n");
        // end removed files

        Map<String, String> currentFilesMap = new HashMap<>();

        File[] currentFiles = CWD.listFiles(File::isFile);
        for (File file : currentFiles) {
            String filePath = file.getPath();
            String id = Blob.generateId(file);
            currentFilesMap.put(filePath, id);
        }

        Set<String> modifiedNotStageFilesPathsSet = new HashSet<>();
        Set<String> deletedNotStageFilesPathsSet = new HashSet<>();
        Set<String> untrackedFilesPathsSet = new HashSet<>();

        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String id = entry.getValue();
            String currentFileId = currentFilesMap.get(filePath);
            if (currentFileId != null) {
                if (removedFilesPathsSet.contains(filePath)) {
                    untrackedFilesPathsSet.add(filePath);
                } else {
                    if (currentFileId.equals(id)) {
                        if (modifiedFilesMap.containsKey(filePath)) {
                            modifiedNotStageFilesPathsSet.add(filePath);
                        }
                    } else {
                        String modifiedFileId = modifiedFilesMap.get(filePath);
                        if (!currentFileId.equals(modifiedFileId)) {
                            modifiedNotStageFilesPathsSet.add(filePath);
                        }
                    }
                }
                currentFilesMap.remove(filePath);
            } else {
                if (!removedFilesPathsSet.contains(filePath)) {
                    deletedNotStageFilesPathsSet.add(filePath);
                }
            }
        }

        for (String filePath : currentFilesMap.keySet().toArray(String[]::new)) {
            String id = currentFilesMap.get(filePath);
            String addedFileId = addedFilesMap.get(filePath);
            if (addedFileId != null) {
                if (!addedFileId.equals(id)) {
                    modifiedNotStageFilesPathsSet.add(filePath);
                }
                addedFilesMap.remove(filePath);
            } else {
                untrackedFilesPathsSet.add(filePath);
            }
            currentFilesMap.remove(filePath);
        }

        for (String filePath : addedFilesMap.keySet()) {
            if (!currentFilesMap.containsKey(filePath)) {
                deletedNotStageFilesPathsSet.add(filePath);
            }
        }

        // modifications not staged for commit
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        List<String> modificationsNotStaged = new ArrayList<>();
        modificationsNotStaged.addAll(modifiedNotStageFilesPathsSet);
        modificationsNotStaged.addAll(deletedNotStageFilesPathsSet);
        modificationsNotStaged.sort(String::compareTo);
        for (String filePath : modificationsNotStaged) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (modifiedNotStageFilesPathsSet.contains(filePath)) {
                statusBuilder.append(" (modified)");
            } else {
                statusBuilder.append(" (deleted)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");
        // end modifications not staged for commit

        // untracked files
        statusBuilder.append("=== Untracked Files ===").append("\n");
        String[] untrackedFilesPaths = untrackedFilesPathsSet.toArray(String[]::new);
        Arrays.sort(untrackedFilesPaths);
        for (String filePath : untrackedFilesPaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName).append("\n");
        }
        statusBuilder.append("\n");
        // end untracked files

        System.out.print(statusBuilder);
    }
}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** Structure of commit history.
 *  @author Chris Seo
 */
public class Tree implements Serializable {

    /** Stores commit tree. */
    static final File TREE_DIR = Utils.join(Main.GITLET_DIR, "tree");

    /** Gitlet directory, where gitlet is stored. */
    static final File GITLET_DIR = Main.GITLET_DIR;

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = Main.WORKING_DIR;

    /** Staged for addition directory. */
    static final File STAGE_DIR = Stage.STAGE_DIR;

    /** Stores staged additions files. */
    static final File STAGED_SAVE = Stage.STAGED_SAVE;

    /** Staged for removal directory. */
    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    /** Initial commit message. */
    static final String INIT_MESSAGE = "initial commit";

    /** Constructor for gitlet commit tree. Makes an initial commit
     * and sets up branches. */
    public Tree() {
        try {
            TREE_DIR.createNewFile();
        } catch (Exception e) {
        }
        HashMap<String, Blob> initBlobs= new HashMap<>();
        for (File file : WORKING_DIR.listFiles()) {
            if (!file.isDirectory()) {
                Blob blob = new Blob(file);
                initBlobs.put(blob.getName(), blob);
                System.out.println(blob);
            }
        }

        Commit initCommit = new Commit(INIT_MESSAGE,
                null, initBlobs, false, true);

        createCommitFile(initCommit);
        _allCommits.add(initCommit.getID());

        _currHead = initCommit;
        _branchNames.put("master", initCommit);

        _currentBranch = "master";

        save();
    }

    /** Handles the commit command.
     * @param args takes commit + message */
    public void commitCommand(String[] args) {
        commitFromStage(args[1], false, null);
        Stage.clear();
        Stage.clearRemoved();
    }

    /** Creates a new commit from the stage. By default new commit is the same
     *  as parent commit.
     * @param message message of the commit */
    public void commitFromStage(String message, boolean isMerge, String parent2) {
        Stage staged = Utils.readObject(STAGED_SAVE, Stage.class);
        if (STAGE_RM_DIR.listFiles().length == 0 && staged.
                getStagedFiles().size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        HashMap<String, Blob> parentBlobs = _currHead.getBlobs();
        HashMap<String, Blob> blobs = new HashMap<>();
        if (parentBlobs != null) {
            blobs.putAll(parentBlobs);
        }
        File[] stagedFiles = STAGE_DIR.listFiles();
        File[] stagedRemovedFiles = STAGE_RM_DIR.listFiles();
        for (File file : stagedFiles) {
            if (!file.isDirectory()) {
                Blob stagedBlob = new Blob(file);
                String nameInStage = stagedBlob.getName();

                blobs.put(nameInStage, stagedBlob);
            }
        }
        for (File file : stagedRemovedFiles) {
            if (!file.isDirectory()) {
                if (blobs.containsKey(file.getName())) {
                    blobs.remove(file.getName());
                }
            }
        }
        Commit newCommit;
        if (isMerge) {
            newCommit = new Commit(message,
                    _currHead.getID(), blobs, true, false);
            newCommit.setParent2ID(parent2);
        } else {
            newCommit = new Commit(message,
                    _currHead.getID(), blobs, false, false);
        }
        _allCommits.add(newCommit.getID());
        createCommitFile(newCommit);
        setHead(newCommit);
    }

    /** Returns the current branch's head. */
    public Commit getCurrHead() {
        return _currHead;
    }

    /** Sets the current branch and global head to commit and saves to file.
     * @param newHead commit to be the new head */
    public void setHead(Commit newHead) {
        _branchNames.put(_currentBranch, newHead);
        _currHead = newHead;
        save();
    }

    /** Sets the current branch. Assumes that given branch exists.
     * @param branchName branch to be set to */
    public void setBranch(String branchName) {
        _currHead = _branchNames.get(branchName);
        _currentBranch = branchName;
        save();
    }

    /** Adds a branch to the tree.
     * @param branchName name of the branch */
    public void addBranch(String branchName) {
        if (_branchNames.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        _branchNames.put(branchName, _currHead);
        save();
    }

    /** Removes a given branch from the tree.
     * @param branchName name of branch to be removed */
    public void removeBranch(String branchName) {
        if (!_branchNames.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (_currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        _branchNames.remove(branchName);
        save();
    }

    /** Handles the reset command.
     * @param args takes reset + commit ID */
    public static void doReset(String[] args) {
        String commitID = args[1];
        commitID = Utils.checkAbbreviated(commitID);
        File commitFile = Utils.join(GITLET_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        Commit currCommit = workingTree.getCurrHead();
        Commit inputtedCommit = commitFromFile(commitFile);
        if (Checkout.trackedTest(currCommit, inputtedCommit)) {
            Checkout.checkoutHelper(inputtedCommit, currCommit);
            workingTree.setHead(inputtedCommit);
            Stage.clear();
            Stage.clearRemoved();
        } else {
            System.out.println("There is an untracked file in the "
                    + "way; delete it, or add and commit it first.");
            System.exit(0);
        }
    }

    /** Helper for reset command.
     * @param file name of commit
     * @return a commit */
    private static Commit commitFromFile(File file) {
        return Utils.readObject(file, Commit.class);
    }

    /** Creates a file for the given commit.
     * @param commit commit to be saved as a file */
    public static void createCommitFile(Commit commit) {
        File commitFile = Utils.join(GITLET_DIR, commit.getID());
        try {
            commitFile.createNewFile();
        } catch (IOException ignored) {
            return;
        }
        Utils.writeObject(commitFile, commit);
    }

    /** Returns current branch. */
    public String currentBranch() {
        return _currentBranch;
    }

    /** Returns all commits. */
    public HashSet<String> getAllCommits() {
        return _allCommits;
    }

    /** Returns the head commit at a given branch.
     * @param branchName name of branch
     * @return head of branch */
    public Commit getHead(String branchName) {
        return _branchNames.get(branchName);
    }

    /** Returns branches and branch heads.
     * @return HashMap of branch, branch head */
    public HashMap<String, Commit> getBranches() {
        return _branchNames;
    }

    /** Saves to a file in .gitlet for persistence. */
    private void save() {
        Utils.writeObject(TREE_DIR, this);
    }

    /** Branch names. */
    private HashMap<String, Commit> _branchNames = new HashMap<>();

    /** All commits ever made. */
    private HashSet<String> _allCommits = new HashSet<>();

    /** Current branch. */
    private String _currentBranch;

    /** Current branch's head. */
    private Commit _currHead;
}

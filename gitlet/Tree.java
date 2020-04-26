package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Class for the structure of commit history.
 *  @author Chris Seo
 */
public class Tree implements Serializable {

    static final File TREE_DIR = Utils.join(Main.GITLET_DIR, "tree");

    static final File GITLET_DIR = Main.GITLET_DIR;

    static final File WORKING_DIR = Main.WORKING_DIR;

    static final File STAGE_DIR = Stage.STAGE_DIR;

    static final File STAGED_SAVE = Stage.STAGED_SAVE;

    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    static final String INIT_DATE = "Wed Dec 31 16:00:00 1969 -0800";

    static final String INIT_MESSAGE = "initial commit";

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

        Commit initCommit = new Commit(INIT_DATE, INIT_MESSAGE,
                null, initBlobs);

        createCommitFile(initCommit);
        _allCommits.add(initCommit.getID());

        _currHead = initCommit;
        _branchNames.put("master", initCommit);

        _currentBranch = "master";

        save();
    }

    /** Creates a new commit from the stage. By default new commit is the same
     *  as parent commit.
     * @param message message of the commit */
    public void commitFromStage(String message) {

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

        String date = DateTimeFormatter.ofPattern("EEE " + "LLL " + "dd "
                + "HH:mm:ss " + "yyyy " + "Z").format(ZonedDateTime.now());

        Commit newCommit = new Commit(date, message,
                _currHead.getID(), blobs);
        _allCommits.add(newCommit.getID());

        createCommitFile(newCommit);

        setHead(newCommit);
    }

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

    /** Sets the current head commit to head of other (noncurrent) branch.
     *  Assumes that there are two branches when called. */
    public void swapBranches() {
        for (String branch : _branchNames.keySet()) {
            if (!branch.equals(_currentBranch)) {
                _currHead = _branchNames.get(branch);
                _currentBranch = branch;
                break;
            }
        }
        save();
    }

    public void addBranch(String branchName) {
        if (_branchNames.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        _branched = true;
        _branchNames.put(branchName, _currHead);
        save();
    }

    public void removeBranch(String branchName) {
        if (!_branchNames.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (_currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        _branched = false;
        _branchNames.remove(branchName);
        save();
    }

    public static void createCommitFile(Commit commit) {
        File commitFile = Utils.join(GITLET_DIR, commit.getID());
        try {
            commitFile.createNewFile();
        } catch (Exception ignored) {
        }
        Utils.writeObject(commitFile, commit);
    }

    public String currentBranch() {
        return _currentBranch;
    }

    public HashSet<String> getAllCommits() {
        return _allCommits;
    }

    public Commit getHead(String branchName) {
        return _branchNames.get(branchName);
    }

    public HashMap<String, Commit> getBranches() {
        return _branchNames;
    }

    /** Saves to a file in .gitlet for persistence. */
    private void save() {
        Utils.writeObject(TREE_DIR, this);
    }

    private HashMap<String, Commit> _branchNames = new HashMap<>();
    private HashSet<String> _allCommits = new HashSet<>();
    private String _currentBranch;
    private Commit _currHead;
    private boolean _branched = false;
}

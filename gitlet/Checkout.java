package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

public class Checkout {
    static final File WORKING_DIR = new File(System.getProperty("user.dir"));
    static final File STAGED_FILES = Stage.STAGED_SAVE;
    static final File TREE_DIR = Tree.TREE_DIR;
    static final File GITLET_DIR = Main.GITLET_DIR;

    public static void doCheckout(String[] args) {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        //case for if input is file name
        if (args.length == 3) {
            if (args[1].equals("--")) {
                String input = args[2];
                if (workingTree.getCurrHead().getBlobs().containsKey(input)) {
                    Commit currHead = workingTree.getCurrHead();
                    checkoutHelper(currHead, input);
                } else {
                    Utils.exit("File does not exist in that commit.");
                }
            } else {
                Utils.exit("Incorrect operands.");
            }
        //case for if input is specific commit + file name
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                String fileInput = args[3]; String commitID = args[1];
                if (commitID.length() < 40) {
                    HashSet<String> allCommits = workingTree.getAllCommits();
                    for (File file : GITLET_DIR.listFiles()) {
                        String fileName = file.getName();
                        if (allCommits.contains(fileName)
                                && fileName.contains(commitID)) {
                            commitID = fileName;
                        } else {
                            Utils.exit("No commit with that id exists.");
                        }
                    }
                }
                if (workingTree.getAllCommits().contains(commitID)) {
                    Commit selectedCommit =
                            Utils.readObject(Utils.join(GITLET_DIR,
                                    commitID), Commit.class);
                    if (selectedCommit.getBlobs().containsKey(fileInput)) {
                        checkoutHelper(selectedCommit, fileInput);
                    } else {
                        Utils.exit("File does not exist in that commit.");
                    }
                } else {
                    Utils.exit("No commit with that id exists.");
                }
            } else {
                Utils.exit("Incorrect operands.");
            }
        //case for if input is branch
        } else {
            String branchName = args[1];
            if (workingTree.getBranches().containsKey(branchName)) {
                //if branch inputted is current branch
                if (branchName.equals(workingTree.currentBranch())) {
                    Utils.exit("No need to checkout the current branch.");
                }
                Commit selectedCommit = workingTree.getHead(branchName);
                Commit currCommit = workingTree.getCurrHead();
                if (trackedTest(currCommit, selectedCommit)) {
                    checkoutHelper(selectedCommit, currCommit);
                    workingTree.swapBranches();
                    Stage.clear();
                    Stage.clearRemoved();
                } else {
                    Utils.exit("There is an untracked file in the "
                            + "way; delete it, or add and commit it first.");
                }
            } else {
                Utils.exit("No such branch exists.");
            }
        }
    }

    /** Writes the file from inputted commit to working directory and
     *  creates a new file if there is none. Assumes fileName is in commit.
     * @param commit commit to select from
     * @param fileName name of file */
    public static void checkoutHelper(Commit commit, String fileName) {
        File dest = Utils.join(WORKING_DIR, fileName);
        String newContents =
                commit.getBlobs().get(fileName).getContents();
        if (!dest.exists()) {
            try {
                dest.createNewFile();
            } catch (Exception ignored) {
            }
        }
        Utils.writeContents(dest, newContents);
    }

    /** Writes the files from inputted commit to working directory.
     *  Overwrites and creates new files as necessary. Also deletes
     *  files to match commit.
     * @param commit commit to select from */
    public static void checkoutHelper(Commit commit, Commit fromCommit) {
        HashMap<String, Blob> blobs = commit.getBlobs();
        HashMap<String, Blob> fromBlobs = fromCommit.getBlobs();
        HashSet<String> fileNames = new HashSet<>();

        for (File file : WORKING_DIR.listFiles()) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                fileNames.add(fileName);
            }
        }
        //will delete files tracked in fromCommit, but not in commit
        for (String fileName : fileNames) {
            if (!blobs.containsKey(fileName) && fromBlobs.
                    containsKey(fileName)) {
                File file = Utils.join(WORKING_DIR, fileName);
                file.delete();
            }
        }
        for (String blobName : blobs.keySet()) {
            String newContents = blobs.get(blobName).getContents();
            File workingFile = Utils.join(WORKING_DIR, blobName);
            if (!fileNames.contains(blobName)) {
                try {
                    workingFile.createNewFile();
                } catch (Exception ignored) {
                }
            }
            Utils.writeContents(workingFile, newContents);
        }
    }

    /** Returns a hashset with all untracked files
     * from the commit inputted.
     * @param commit commit to be surveyed
     * @return a hashset containing the untracked file names */
    public static HashSet<String> everythingTracked(Commit commit) {
        HashMap<String, Blob> blobs = commit.getBlobs();
        HashSet<String> fileNames = new HashSet<>();

        HashSet<String> result = new HashSet<>();

        for (File file : WORKING_DIR.listFiles()) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                fileNames.add(fileName);
            }
        }

        for (String file : fileNames) {
            if (!blobs.containsKey(file)) {
                result.add(file);
            }
        }
        return result;
    }

    /** Returns true if both commits share an untracked file
     *  and if current commit has all files tracked.
     * @param currCommit current commit head
     * @param selectedCom commit switching to
     * @return true if both untracked */
    public static boolean trackedTest(Commit currCommit,
                                        Commit selectedCom) {
        HashMap<String, Blob> blobs = selectedCom.getBlobs();
        for (String el : everythingTracked(currCommit)) {
            if (blobs.containsKey(el)) {
                return false;
            }
        }
        return true;
    }
}

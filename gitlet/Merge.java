package gitlet;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Merge {

    static final File WORKING_DIR = Main.WORKING_DIR;
    static final File TREE_DIR = Tree.TREE_DIR;
    static final File STAGE_DIR = Stage.STAGE_DIR;
    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;
    static final File GITLET_DIR = Main.GITLET_DIR;

    public static void doMerge(String[] args) {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        String inputBranch = args[1];

        mergeError(workingTree, inputBranch);

        Commit currHead = workingTree.getCurrHead();
        Commit inputHead = workingTree.getBranches().get(inputBranch);

        Commit splitPoint = findSplitPoint(workingTree, inputBranch);

        if (splitPoint.equals(inputHead)) {
            Utils.exit("Given branch is an ancestor of the current branch.");
        } else if (splitPoint.equals(currHead)) {
            //checkout the given branch
            Checkout.doCheckout(args);
            Utils.exit("Current branch fast-forwarded.");
        }

        doCheckouts(splitPoint, currHead, inputHead);

        // Any files present at the split point, unmodified in the current
        // branch, and absent in the given branch should be removed
        // and untracked.
        HashSet<String> unModCurr = unmodifiedSinceSplit(splitPoint,
                                                            currHead);
        for (String unmodifiedCurr : unModCurr) {
            if (removedSinceSplit(splitPoint, inputHead).
                                contains(unmodifiedCurr)) {
                Remove.doRemove(unmodifiedCurr);
            }
        }
        checkConflicts(splitPoint, currHead, inputHead);
        String commitMessage = "Merged "
                + inputBranch + " into "
                + workingTree.currentBranch() + ".";

        workingTree.commitFromStage(commitMessage, true,
                                    inputHead.getID());
    }

    /** Checks modifications in both branches.
     * @param splitPoint split point of tree
     * @param currHead current head of branch merging into
     * @param inputHead head of branch merging from */
    public static void checkConflicts(Commit splitPoint, Commit currHead,
                                      Commit inputHead) {
        HashSet<String> currMod = modifiedSinceSplit(splitPoint, currHead);
        HashSet<String> inputMod = modifiedSinceSplit(splitPoint, inputHead);
        HashSet<String> currAdded = addedSinceSplit(splitPoint, currHead);
        HashSet<String> inputAdded = addedSinceSplit(splitPoint, inputHead);
        HashSet<String> currRemoved = removedSinceSplit(splitPoint, currHead);
        HashSet<String> inputRemoved = removedSinceSplit(splitPoint,
                                    inputHead);

        //string names file, blob at index 0 names current blob
        HashMap<String, ArrayList<Blob>> conflictBlobs = new HashMap<>();

        // if contents of both are changed and different from other
        for (String modInInput : inputMod) {
            // if both have been modified
            if (currMod.contains(modInInput)) {
                // if two modified files are different
                Blob currentModified = currHead.getBlobs().get(modInInput);
                Blob inputModified = inputHead.getBlobs().get(modInInput);
                if (!currentModified.getContents().equals(inputModified.
                        getContents())) {
                    ArrayList<Blob> conflicted = new ArrayList<>();
                    conflicted.add(currentModified);
                    conflicted.add(inputModified);
                    conflictBlobs.put(modInInput, conflicted);
                }
            }
        }
        // contents of one are changed and the other file is deleted
        for (String modInInput : inputMod) {
            // if modified in input and deleted in current
            if (currRemoved.contains(modInInput)) {
                Blob inputModified = inputHead.getBlobs().get(modInInput);
                ArrayList<Blob> conflicted = new ArrayList<>();
                conflicted.add(null);
                conflicted.add(inputModified);
                conflictBlobs.put(modInInput, conflicted);
            }
        }
        for (String modInCurr : currMod) {
            // if modified in curr and deleted in input
            if (inputRemoved.contains(modInCurr)) {
                Blob currentModified = currHead.getBlobs().get(modInCurr);
                ArrayList<Blob> conflicted = new ArrayList<>();
                conflicted.add(currentModified);
                conflicted.add(null);
                conflictBlobs.put(modInCurr, conflicted);
            }
        }
        // file was absent at the split point and has different contents
        // in the given and current branches
        for (String addedInInput : inputAdded) {
            // if both have been added
            if (currAdded.contains(addedInInput)) {
                // if two modified files are different
                Blob currentHeadAdded = currHead.getBlobs().get(addedInInput);
                Blob inputHeadAdded = inputHead.getBlobs().get(addedInInput);
                if (!currentHeadAdded.getContents().equals(inputHeadAdded.
                        getContents())) {
                    ArrayList<Blob> conflicted = new ArrayList<>();
                    conflicted.add(currentHeadAdded);
                    conflicted.add(inputHeadAdded);
                    conflictBlobs.put(addedInInput, conflicted);
                }
            }
        }
        writeConflicts(conflictBlobs);
    }

    public static void writeConflicts(HashMap<String,
            ArrayList<Blob>> conflictBlobs) {
        if (!conflictBlobs.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
        }
        for (String fileName : conflictBlobs.keySet()) {
            ArrayList<Blob> conflict = conflictBlobs.get(fileName);
            Blob curr = conflict.get(0);
            Blob input = conflict.get(1);
            String currString;
            String inputString;

            if (curr == null) {
                currString = "";
            } else {
                currString = curr.getContents();
            }
            if (input == null) {
                inputString = "";
            } else {
                inputString = input.getContents();
            }
            File conflictedFile = Utils.join(GITLET_DIR, fileName);
            if (!conflictedFile.exists()) {
                try {
                    conflictedFile.createNewFile();
                } catch (Exception ignored) {
                }
            }
            String fileString = "<<<<<<< HEAD\n"
                    + currString + "\n"
                    + "=======\n"
                    + inputString + "\n"
                    + ">>>>>>>\n";
            Utils.writeContents(conflictedFile, fileString);
            Stage.add(fileName);
        }
    }

    /** File modified in given branch but not in current branch should be
     * checked out from version in given branch and staged. File added in
     * given branch but not in current branch should be checked out and
     * staged.
     * @param splitPoint split point of tree
     * @param currHead current branch's head
     * @param inputHead given branch's head */
    public static void doCheckouts(Commit splitPoint, Commit currHead,
                                   Commit inputHead) {
        HashSet<String> currMod = modifiedSinceSplit(splitPoint, currHead);
        HashSet<String> inputMod = modifiedSinceSplit(splitPoint, inputHead);

        // Any files that have been modified in the given branch since the
        // split point, but not modified in the current branch since the split
        // point should be checked out & staged
        for (String modInInput : inputMod) {
            if (!currMod.contains(modInInput)) {
                Checkout.checkoutCommitFile(modInInput, inputHead.getID());
                Stage.add(modInInput);
            }
        }

        HashSet<String> currAdded = addedSinceSplit(splitPoint, currHead);
        HashSet<String> inputAdded = addedSinceSplit(splitPoint, inputHead);

        // Any files that were not present at the split point and are present
        // only in the given branch should be checked out and staged.
        for (String addedInInput : inputAdded) {
            if (!currAdded.contains(addedInInput)) {
                Checkout.checkoutCommitFile(addedInInput, inputHead.getID());
                Stage.add(addedInInput);
            }
        }
    }


    /** Returns a hashset of files that have been changed since the split.
     * @param split splitpoint
     * @param head head commit
     * @return hashset */
    public static HashSet<String> modifiedSinceSplit(Commit split,
                                                     Commit head) {
        HashSet<String> result = new HashSet<>();

        HashMap<String, Blob> headBlobs = head.getBlobs();
        HashMap<String, Blob> splitBlobs = split.getBlobs();

        for (Blob headBlob : headBlobs.values()) {
            String blobName = headBlob.getName();
            if (splitBlobs.containsKey(blobName)) {
                Blob splitBlob = splitBlobs.get(blobName);
                if (!splitBlob.getContents().equals(headBlob.getContents())) {
                    result.add(blobName);
                }
            }
        }
        return result;
    }

    /** Returns a hashset of files that have not been changed since the split.
     * @param split splitpoint
     * @param head head commit
     * @return hashset */
    public static HashSet<String> unmodifiedSinceSplit(Commit split,
                                                     Commit head) {
        HashSet<String> result = new HashSet<>();

        HashMap<String, Blob> headBlobs = head.getBlobs();
        HashMap<String, Blob> splitBlobs = split.getBlobs();

        for (Blob headBlob : headBlobs.values()) {
            String blobName = headBlob.getName();
            if (splitBlobs.containsKey(blobName)) {
                Blob splitBlob = splitBlobs.get(blobName);
                if (splitBlob.getContents().equals(headBlob.getContents())) {
                    result.add(blobName);
                }
            }
        }
        return result;
    }

    /** Returns a hashset of files that have been removed since the split.
     * @param split splitpoint
     * @param head head commit
     * @return hashset */
    public static HashSet<String> removedSinceSplit(Commit split,
                                                  Commit head) {
        HashSet<String> result = new HashSet<>();

        HashMap<String, Blob> headBlobs = head.getBlobs();
        HashMap<String, Blob> splitBlobs = split.getBlobs();

        for (String splitBlob : splitBlobs.keySet()) {
            if (!headBlobs.containsKey(splitBlob)) {
                result.add(splitBlob);
            }
        }
        return result;
    }

    /** Returns a hashset of files that have been added since the split.
     * @param split splitpoint
     * @param head head commit
     * @return hashset */
    public static HashSet<String> addedSinceSplit(Commit split,
                                                    Commit head) {
        HashSet<String> result = new HashSet<>();

        HashMap<String, Blob> headBlobs = head.getBlobs();
        HashMap<String, Blob> splitBlobs = split.getBlobs();

        for (String headBlob : headBlobs.keySet()) {
            if (!splitBlobs.containsKey(headBlob)) {
                result.add(headBlob);
            }
        }
        return result;
    }

    public static void mergeError(Tree workingTree, String inputBranch) {
        if (!workingTree.getBranches().containsKey(inputBranch)) {
            Utils.exit("A branch with that name does not exist.");
        }
        if (workingTree.getBranches().size() == 1
                || workingTree.currentBranch().equals(inputBranch)) {
            Utils.exit("Cannot merge a branch with itself.");
        }
        File[] staged = STAGE_DIR.listFiles();
        File[] stagedRM = STAGE_RM_DIR.listFiles();
        if (staged.length != 0 || stagedRM.length != 0) {
            Utils.exit("You have uncommitted changes.");
        }

    }

    /** Finds the latest common ancestor of the two branch heads of a tree.
     *  Assumes there are two heads.
     * @param workingTree tree to be examined
     * @return the split point */
    public static Commit findSplitPoint(Tree workingTree, String inputtedBranch) {
        Commit currHead = workingTree.getCurrHead();
        Commit otherHead = workingTree.getBranches().get(inputtedBranch);
        HashSet<String> currAncestors = new HashSet<>();
        HashSet<String> otherAncestors = new HashSet<>();
        Commit splitPoint = null;
        while (currHead != null) {
            currAncestors.add(currHead.getID());
            //if commit is a merge commit, chooses ancestor on other branch
            if (currHead.isMerge()) {
                currHead = currHead.getParent2();
            } else {
                currHead = currHead.getParent();
            }
        }
        while (otherHead != null) {
            otherAncestors.add(otherHead.getID());
            otherHead = otherHead.getParent();
        }
        for (String ancestor : currAncestors) {
            if (otherAncestors.contains(ancestor)) {
                File file = Utils.join(GITLET_DIR, ancestor);
                splitPoint = Utils.readObject(file, Commit.class);
                break;
            }
        }
        return splitPoint;
    }
}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/** Handles merge command.
 *  @author Chris Seo
 */
public class Merge {

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = Main.WORKING_DIR;

    /** Stores commit tree. */
    static final File TREE_DIR = Tree.TREE_DIR;

    /** Staged for addition directory. */
    static final File STAGE_DIR = Stage.STAGE_DIR;

    /** Staged for removal directory. */
    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    /** Does the merge command.
     * @param args takes merge + branch name */
    public static void doMerge(String[] args) {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        String inputBranch = args[1];

        mergeError(workingTree, inputBranch);

        Commit currHead = workingTree.getCurrHead();
        Commit inputHead = workingTree.getBranches().get(inputBranch);
        Commit splitPoint = findSplitPoint(workingTree, inputBranch);

        if (splitPoint.getID().equals(inputHead.getID())) {
            Utils.exit("Given branch is an ancestor of the current branch.");
        } else if (splitPoint.getID().equals(currHead.getID())) {
            Checkout.doCheckout(args);
            Utils.exit("Current branch fast-forwarded.");
        }

        doCheckouts(splitPoint, currHead, inputHead);

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
        Stage.clear();
        Stage.clearRemoved();
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
        HashMap<String, ArrayList<Blob>> conflictBlobs = new HashMap<>();
        for (String modInInput : inputMod) {
            if (currMod.contains(modInInput)) {
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
        for (String modInInput : inputMod) {
            if (currRemoved.contains(modInInput)) {
                Blob inputModified = inputHead.getBlobs().get(modInInput);
                ArrayList<Blob> conflicted = new ArrayList<>();
                conflicted.add(null); conflicted.add(inputModified);
                conflictBlobs.put(modInInput, conflicted);
            }
        }
        for (String modInCurr : currMod) {
            if (inputRemoved.contains(modInCurr)) {
                Blob currentModified = currHead.getBlobs().get(modInCurr);
                ArrayList<Blob> conflicted = new ArrayList<>();
                conflicted.add(currentModified); conflicted.add(null);
                conflictBlobs.put(modInCurr, conflicted);
            }
        }
        for (String addedInInput : inputAdded) {
            if (currAdded.contains(addedInInput)) {
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

    /** Writes conflicts to conflicted file.
     * @param conflictBlobs HashMap of file name, and an ArrayList of
     * the conflicted blobs */
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
            File conflictedFile = Utils.join(WORKING_DIR, fileName);
            if (!conflictedFile.exists()) {
                try {
                    conflictedFile.createNewFile();
                } catch (IOException ignored) {
                    return;
                }
            }
            String fileString = "<<<<<<< HEAD\n"
                    + currString
                    + "=======\n"
                    + inputString
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
        for (String modInInput : inputMod) {
            if (!currMod.contains(modInInput)) {
                Checkout.checkoutCommitFile(modInInput, inputHead.getID());
                Stage.add(modInInput);
            }
        }
        HashSet<String> currAdded = addedSinceSplit(splitPoint, currHead);
        HashSet<String> inputAdded = addedSinceSplit(splitPoint, inputHead);
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

    /** Handles errors for merge.
     * @param workingTree tree doing merge on
     * @param inputBranch given branch for the merge */
    public static void mergeError(Tree workingTree, String inputBranch) {
        Commit currHead = workingTree.getCurrHead();
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
        if (!Checkout.everythingTracked(currHead).isEmpty()) {
            Utils.exit("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
        }
    }

    /** Finds the latest common ancestor of the current branch's head and
     *  the inputted head. Assumes there is more than 1 head.
     * @param workingTree tree to be examined
     * @param inputtedBranch branch given
     * @return the split point */
    public static Commit findSplitPoint(Tree workingTree,
                                        String inputtedBranch) {
        Commit splitPoint;
        Commit givenHead = workingTree.getBranches().get(inputtedBranch);
        Commit currHead = workingTree.getCurrHead();
        HashSet<String> givenAncestors = findAllAncestors(givenHead);
        while (currHead != null) {
            if (currHead.isMerge()) {
                HashMap<Integer, Commit> compares = new HashMap<>();
                Commit parent1 = currHead.getParent();
                Commit parent2 = currHead.getParent2();
                int counter1 = 0;
                while (parent1 != null) {
                    if (givenAncestors.contains(parent1.getID())) {
                        compares.put(counter1, parent1);
                        break;
                    }
                    parent1 = parent1.getParent();
                    counter1++;
                }
                int counter2 = 0;
                while (parent2 != null) {
                    if (givenAncestors.contains(parent2.getID())) {
                        compares.put(counter2, parent2);
                        break;
                    }
                    parent2 = parent2.getParent();
                    counter2++;
                }
                int closest = Collections.min(compares.keySet());
                splitPoint = compares.get(closest);
                return splitPoint;
            } else if (givenAncestors.contains(currHead.getID())) {
                splitPoint = currHead;
                return splitPoint;
            }
            currHead = currHead.getParent();
        }
        return null;
    }

    /** Finds all ancestors of head and stores in a HashSet.
     * @param head commit as head
     * @return HashSet of all ancestors */
    public static HashSet<String> findAllAncestors(Commit head) {
        HashSet<String> result = new HashSet<>();
        while (head != null) {
            if (head.isMerge()) {
                result.addAll(findAllAncestors(head.getParent2()));
            }
            result.add(head.getID());
            head = head.getParent();
        }
        return result;
    }
}

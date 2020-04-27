package gitlet;

import java.io.File;
import java.util.HashSet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  Initially checks for correct argument lengths.
 *  @author Chris Seo
 */
public class Main {

    static final File WORKING_DIR = new File(System.getProperty("user.dir"));

    static final File GITLET_DIR = Utils.join(WORKING_DIR, ".gitlet");

    static final File STAGE_DIR = Stage.STAGE_DIR;

    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    static final File STAGED_FILES = Stage.STAGED_SAVE;

    static final File TREE_DIR = Tree.TREE_DIR;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        if ("init".equals(args[0])) {
            init(args);
        } else {
            if (GITLET_DIR.exists()) {
                switch (args[0]) {
                case "test":
                    test();
                    break;
                case "add":
                    addFile(args);
                    break;
                case "commit":
                    commit(args);
                    break;
                case "checkout":
                    checkout(args);
                    break;
                case "log":
                    log(args, false);
                    break;
                case "global-log":
                    log(args, true);
                    break;
                case "rm":
                    rm(args);
                    break;
                case "find":
                    find(args);
                    break;
                case "status":
                    status(args);
                    break;
                case "branch":
                    branch(args);
                    break;
                case "rm-branch":
                    removeBranch(args);
                    break;
                case "reset":
                    reset(args);
                    break;
                case "merge":
                    merge(args);
                    break;
                default:
                    System.out.println("No command with "
                            + "that name exists.");
                    System.exit(0);
                }
            } else {
                System.out.println("Not in an initialized "
                        + "Gitlet directory.");
                System.exit(0);
            }
        }
    }

    private static void init(String[] args) {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system " +
                    "already exists in the current directory.");
            System.exit(0);
        }
        if (args.length > 1) {
            throw new GitletException("Incorrect operands.");
        }
        GITLET_DIR.mkdir();
        Stage initialStage = new Stage();
        Tree initializeTree = new Tree();
    }

    private static void addFile(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Stage.add(args[1]);
    }

    private static void commit(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (args.length == 1 || args[1].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        workingTree.commitFromStage(args[1], false, null);
        Stage.clear();
        Stage.clearRemoved();
    }

    private static void checkout(String[] args) {
        if (args.length > 4) {
            Utils.exit("Incorrect operands.");
        }
        Checkout.doCheckout(args);
    }

    private static void log(String[] args, boolean global) {
        if (args.length > 1) {
            Utils.exit("Incorrect operands.");
        }

        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        if (global) {
            for (String commitID : workingTree.getAllCommits()) {
                File commitFile = findFromGitletDir(commitID);
                Commit commit = Utils.readObject(commitFile, Commit.class);
                System.out.println("===\n"
                        + "commit " + commit.getID() + "\n"
                        + "Date: " + commit.getTimestamp() + "\n"
                        + commit.getMessage() + "\n");
            }
        } else {
            Commit currHead = workingTree.getCurrHead();
            while (currHead != null) {
                System.out.println("===\n"
                        + "commit " + currHead.getID() + "\n"
                        + "Date: " + currHead.getTimestamp() + "\n"
                        + currHead.getMessage() + "\n");
                currHead = currHead.getParent();
            }
        }
    }

    private static void rm(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String fileName = args[1];
        Remove.doRemove(fileName);
    }

    private static void find(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        HashSet<String> allCommits = workingTree.getAllCommits();
        String message = args[1];
        HashSet<String> result = new HashSet<>();

        for (File file : GITLET_DIR.listFiles()) {
            if (!file.isDirectory()) {
                // if the file is a commit file, then check its message
                // against input
                if (allCommits.contains(file.getName())) {
                    Commit currCommit = Utils.readObject(file, Commit.class);
                    if (currCommit.getMessage().equals(message)) {
                        System.out.println(currCommit.getID());
                        result.add(currCommit.getID());
                    }
                }
            }
        }
        if (result.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    private static void status(String[] args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Status.doStatus();
    }

    private static void branch(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        workingTree.addBranch(args[1]);
    }

    private static void removeBranch(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        workingTree.removeBranch(args[1]);
    }

    private static void reset(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File commitFile = Utils.join(GITLET_DIR, args[1]);
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

    private static void merge(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Merge.doMerge(args);
    }

    private static Commit commitFromFile(File file) {
        return Utils.readObject(file, Commit.class);
    }

    private static File findFromGitletDir(String fileName) {
        for (File file : GITLET_DIR.listFiles()) {
            if (!file.isDirectory()) {
                if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    public static void test() {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);

    }
}

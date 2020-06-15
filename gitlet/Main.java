package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  Initially checks for correct argument lengths.
 *  @author Chris Seo
 */
public class Main {

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = new File(System.getProperty("user.dir"));

    /** Gitlet directory, where gitlet is stored. */
    static final File GITLET_DIR = Utils.join(WORKING_DIR, ".gitlet");

    /** Stores commit tree. */
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

    /** Handle init.
     * @param args takes init command */
    private static void init(String[] args) {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        if (args.length > 1) {
            throw new GitletException("Incorrect operands.");
        }
        GITLET_DIR.mkdir();
        Stage initialStage = new Stage();
        Tree initializeTree = new Tree();
    }

    /** Handle add.
     * @param args takes add + file name */
    private static void addFile(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Stage.add(args[1]);
    }

    /** Handle commit.
     * @param args takes commit + message */
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
        workingTree.commitCommand(args);
    }

    /** Handle checkout.
     * @param args takes checkout + args */
    private static void checkout(String[] args) {
        if (args.length > 4) {
            Utils.exit("Incorrect operands.");
        }
        Checkout.doCheckout(args);
    }

    /** Handle logs (global and branch).
     * @param args takes log command
     * @param global true if global log */
    private static void log(String[] args, boolean global) {
        if (args.length > 1) {
            Utils.exit("Incorrect operands.");
        }
        Archive.doLog(global);
    }

    /** Handle remove.
     * @param args takes remove + file name */
    private static void rm(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String fileName = args[1];
        Remove.doRemove(fileName);
    }

    /** Handles find.
     * @param args find + message of commit */
    private static void find(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Archive.doFind(args);
    }

    /** Handle status.
     * @param args takes status command */
    private static void status(String[] args) {
        if (args.length > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Status.doStatus();
    }

    /** Handle branch.
     * @param args takes branch name */
    private static void branch(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        workingTree.addBranch(args[1]);
    }

    /** Handle rm-branch.
     * @param args takes rm-branch command */
    private static void removeBranch(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        workingTree.removeBranch(args[1]);
    }

    /** Handle reset.
     * @param args takes reset + commit ID */
    private static void reset(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Tree.doReset(args);
    }

    /** Handle merge.
     * @param args takes merge + branch name */
    private static void merge(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Merge.doMerge(args);
    }
}

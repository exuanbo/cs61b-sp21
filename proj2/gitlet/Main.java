package gitlet;

import static gitlet.MyUtils.exit;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Exuanbo
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exit("Please enter a command.");
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init" -> {
                validateNumArgs(args, 1);
                Repository.init();
            }
            case "add" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String fileName = args[1];
                new Repository().add(fileName);
            }
            case "commit" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String message = args[1];
                if (message.length() == 0) {
                    exit("Please enter a commit message.");
                }
                new Repository().commit(message);
            }
            case "rm" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String fileName = args[1];
                new Repository().remove(fileName);
            }
            case "log" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().log();
            }
            case "global-log" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                Repository.globalLog();
            }
            case "find" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String message = args[1];
                if (message.length() == 0) {
                    exit("Found no commit with that message.");
                }
                Repository.find(message);
            }
            case "status" -> {
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().status();
            }
            case "checkout" -> {
                Repository.checkWorkingDir();
                Repository repository = new Repository();
                switch (args.length) {
                    case 3 -> {
                        if (!args[1].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        String fileName = args[2];
                        repository.checkout(fileName);
                    }
                    case 4 -> {
                        if (!args[2].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        String commitId = args[1];
                        String fileName = args[3];
                        repository.checkout(commitId, fileName);
                    }
                    case 2 -> {
                        String branch = args[1];
                        repository.checkoutBranch(branch);
                    }
                    default -> exit("Incorrect operands.");
                }
            }
            default -> exit("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number.
     *
     * @param args Argument array from command line
     * @param n    Number of expected arguments
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}

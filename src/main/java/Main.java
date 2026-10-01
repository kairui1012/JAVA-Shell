import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {
        Set<String> builtins = Set.of("echo", "exit", "type", "pwd", "cd");
        while(true){
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");

            // Captures the user's command in the "command" variable
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();

            if (command.equals("exit")) {
                break;
            }

            if (command.startsWith("echo ")) {
                System.out.println(command.substring(5));
                continue;
            }

            if (command.startsWith("type ")) {
                // 取得 type 后面的命令名称，例如 "type echo" 得到 "echo"
                String target = command.substring(5).trim();

                // 先判断是不是 Shell 内建命令
                if (builtins.contains(target)) {
                    System.out.println(target + " is a shell builtin");
                } else {
                    // 获取系统 PATH，例如：
                    // /opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin
                    String systemPath = System.getenv("PATH");

                    // 使用当前操作系统的 PATH 分隔符拆分目录
                    String[] directories = systemPath.split(
                            Pattern.quote(File.pathSeparator)
                    );

                    boolean found = false;

                    // 按照 PATH 的顺序逐个寻找命令
                    for (String directory : directories) {
                        Path candidate = Path.of(directory, target);

                        // 必须是普通文件，并且当前用户拥有执行权限
                        if (Files.isRegularFile(candidate)
                                && Files.isExecutable(candidate)) {

                            System.out.println(target + " is " + candidate);

                            found = true;

                            // Shell 只使用 PATH 中第一个匹配的可执行文件
                            break;
                        }
                    }

                    // 搜索完全部 PATH 目录后仍未找到
                    if (!found) {
                        System.out.println(target + ": not found");
                    }
                }

                // type 命令处理完毕，进入 Shell 的下一轮循环
                continue;
            }

            // Prints the "<command>: command not found" message
            System.out.println(command + ": command not found");

        }
    }
}

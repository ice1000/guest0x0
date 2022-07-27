import org.aya.gradle.BuildUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateVersionTask extends DefaultTask {
  {
    className = "GeneratedVersion"
    group = "build setup"
  }

  @OutputDirectory File outputDir
  @Input String className
  @Input def basePackage = project.group
  @Input def taskVersion = project.version

  @TaskAction def run() {
    def stdout = BuildUtil.gitRev(project.rootDir)
    def code = """\
      package ${basePackage}.prelude;
      import org.aya.util.Version;
      import org.jetbrains.annotations.NotNull;
      import org.jetbrains.annotations.NonNls;
      public class $className {
        public static final @NotNull @NonNls String VERSION_STRING = "$taskVersion";
        public static final @NotNull @NonNls String COMMIT_HASH = "${stdout.toString().trim()}";
        public static final @NotNull Version VERSION = Version.create(VERSION_STRING);
      }""".stripIndent()
    outputDir.mkdirs()
    def outFile = new File(outputDir, "${className}.java")
    if (!outFile.exists()) assert outFile.createNewFile()
    outFile.write(code)
  }
}

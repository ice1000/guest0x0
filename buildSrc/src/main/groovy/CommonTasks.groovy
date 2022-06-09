import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

import java.text.SimpleDateFormat

class CommonTasks {
  static TaskProvider<Jar> fatJar(Project project, String mainClass) {
    project.tasks.register('fatJar', Jar) {
      archiveClassifier.set 'fat'
      from project.configurations.runtimeClasspath.collect {
        if (it.isDirectory()) it else project.zipTree(it)
      }
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
      exclude '**/module-info.class'
      exclude '*.html'
      exclude 'META-INF/ECLIPSE_.*'
      manifest.attributes(
        'Main-Class': mainClass,
        'Build': new SimpleDateFormat('yyyy/M/dd HH:mm:ss').format(new Date())
      )
      def jar = project.tasks.jar
      dependsOn(jar)
      //noinspection GroovyAssignabilityCheck
      with jar
    }
  }
}

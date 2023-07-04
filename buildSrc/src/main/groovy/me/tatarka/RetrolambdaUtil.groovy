package me.tatarka

import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class RetrolambdaUtil {
    static String capitalize(CharSequence self) {
        return self.length() == 0 ? "" : "" + Character.toUpperCase(self.charAt(0)) + self.subSequence(1, self.length())
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    static File javaOutputDir(SourceSet set) {
        try {
            return set.java.destinationDirectory.asFile.get()
        } catch (Exception e) {
            return set.output.classesDirs
        }
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    static File groovyOutputDir(SourceSet set) {
        try {
            return set.groovy.outputDirs
        } catch (Exception e) {
            return set.output.classesDirs
        }
    }
}

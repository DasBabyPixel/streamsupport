package me.tatarka

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.FileChange

/**
 Copyright 2014 Evan Tatarka

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import org.gradle.work.Incremental
import org.gradle.work.InputChanges

import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

/**
 * A task that runs retrolambda
 */
@CompileStatic
class RetrolambdaTask extends DefaultTask {
    @Incremental
    @InputDirectory
    final DirectoryProperty inputDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    @InputFiles
    FileCollection classpath

    @Input
    JavaVersion javaVersion = JavaVersion.VERSION_1_6

    @Input
    List<String> jvmArgs = []

    @TaskAction
    def execute(InputChanges inputs) {
        def retrolambda = project.extensions.getByType(RetrolambdaExtension)


        List<FileChange> changes = []
        inputs.getFileChanges(inputDir).forEach {
            changes.add(it)
        }

        // Ensure output is cleared if build is not incremental.
        if (inputs.incremental && !changes.isEmpty() && !retrolambda.incremental) {
            outputDir.asFile.get().eachFile { it.delete() }
        } else {
            for (FileChange change : changes) {
                if (change.changeType != ChangeType.ADDED) deleteRelated(toOutput(change.file))
            }
        }

        logging.captureStandardOutput(LogLevel.INFO)

        if (!inputs.incremental || !changes.isEmpty()) {
            RetrolambdaExec exec = new RetrolambdaExec(project)
            exec.inputDir = inputDir.asFile.get()
            exec.outputDir = outputDir.asFile.get()
            exec.bytecodeVersion = javaVersionToBytecode(javaVersion)
            exec.classpath = classpath
            if (inputs.incremental && retrolambda.incremental) {
                exec.includedFiles = project.files(changes*.file)
            }
            exec.defaultMethods = retrolambda.defaultMethods
            exec.jvmArgs = jvmArgs
            exec.exec()
        }

        for (FileChange change : changes) {
            if (change.changeType == ChangeType.REMOVED) {
                def outFile = toOutput(change.file)
                outFile.delete()
                project.logger.debug("Deleted " + outFile)
                deleteRelated(outFile)
            }
        }
    }

    File toOutput(File file) {
        return outputDir.asFile.get().toPath().resolve(inputDir.asFile.get().toPath().relativize(file.toPath())).toFile()
    }

    void deleteRelated(File file) {
        def className = file.name.replaceFirst(/\.class$/, '')
        // Delete any generated Lambda classes
        project.logger.debug("Deleting related for " + className + " in " + file.parentFile)
        file.parentFile.eachFile {
            if (it.name.matches(/$className\$\$/ + /Lambda.*\.class$/)) {
                project.logger.debug("Deleted " + it)
                it.delete()
            }
        }
    }
}

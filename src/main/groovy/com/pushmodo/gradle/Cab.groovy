package com.pushmodo.gradle

import org.gradle.api.*
import org.apache.commons.lang3.SystemUtils
import groovy.text.SimpleTemplateEngine

class Cab implements Plugin<Project> {
	private final String windowsPrefix = 'cmd /c '
	private final String unixPrefix = 'sh -c '

	private String prefix
	private Project project

	private void run(cmd, cwd) {
		cmd = "${prefix} \"${cmd}\""
		println "shell: ${cmd}"
		ProcessBuilder builder = new ProcessBuilder( cmd.split(' ') )
 		builder.directory(cwd)
		builder.redirectErrorStream(true)

		Process process = builder.start()

		InputStream stdout = process.getInputStream ()
		BufferedReader reader = new BufferedReader (new InputStreamReader(stdout))

		def line
		while ((line = reader.readLine ()) != null) {
		   println "*: ${line}"
		}
	}

	private void createProject() {
		def projectDir = project.projectDir
		def buildDir = project.buildDir
		def id = project.cab.id
		def title = project.cab.title
		run("cordova create ${buildDir.getPath()} ${id} ${title}", projectDir)
	}

	private void addPlatforms() {
		project.cab.platforms.each() {
			run("cordova platform add ${it}", project.buildDir)
		}
	}

	private void addPlugins() {
		project.cab.plugins.each() {
			run("cordova plugin add ${it}", project.buildDir)
		}
	}

	private void copySource() {
		// delete www folder in cordova base project
		def targetWWW = new File(project.buildDir, 'www')
		def appDir = project.cab.appDir

		if (appDir == null) {
			appDir = new File(project.projectDir, 'www')
		}

		println("Deleting ${targetWWW.getPath()}")
		targetWWW.deleteDir()

		// copy the web app source to the cordova www folder
		println("Copying ${appDir.getPath()}")
		println("into ${targetWWW.getPath()}")
		project.copy {
		    from appDir
 		    into targetWWW
		}
	}

	private void copyConfig() {
		def configDir = project.cab.configDir

		if (configDir == null) {
			configDir = new File(project.projectDir, 'config')
		}

		def srcConfigFile = new File(configDir, 'config.xml')
		def outConfigFile = new File(project.buildDir, 'config.xml')

		if (!srcConfigFile.exists()) {
			println 'No config template found'
			return
		}

		println "Generating config.xml from template ${srcConfigFile.getPath()}"
		println "with bindings ${project.cab.configBindings.inspect()}"

		def engine = new SimpleTemplateEngine()
		def template = engine.createTemplate(srcConfigFile.getText('UTF-8')).make(project.cab.configBindings)

		def w = outConfigFile.newWriter('UTF-8')
		w << template.toString()
		w.flush()
		w.close()
	}

	void apply(Project project) {
		this.project = project

		project.extensions.create('cab', CabExtension)

		if (SystemUtils.IS_OS_WINDOWS) {
			prefix = windowsPrefix
		} else {
			prefix = unixPrefix
		}

		project.task('clean') << {
			project.buildDir.deleteDir()
		}

		def createProjectTask = project.task('createProject') << {
			createProject()
		}

		def addPlatformsTask = project.task('addPlatforms') << {
			addPlatforms()
		}

		def addPluginsTask = project.task('addPlugins') << {
			addPlugins()
		}

		def copySourceTask = project.task('copySource') << {
			copySource()
		}

		def copyConfigTask = project.task('copyConfig') << {
			copyConfig()
		}

		def buildTask = project.task('build') << {
			//placeholder task
		}

		buildTask.dependsOn(addPluginsTask)
		addPluginsTask.dependsOn(addPlatformsTask)
		addPlatformsTask.dependsOn(copySourceTask)
		copySourceTask.dependsOn(copyConfigTask)
		copyConfigTask.dependsOn(createProjectTask)
	}
}

class CabExtension {
	String id
	String title
    ArrayList platforms
    ArrayList plugins
    File configDir
    File appDir
    Map<String,String> configBindings
}

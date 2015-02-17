package com.pushmodo.gradle

import org.gradle.api.*
import org.apache.commons.lang3.SystemUtils

class Cab implements Plugin<Project> {
	private final String windowsPrefix = 'cmd /c '
	private final String unixPrefix = 'sh -c '
	
	private File buildDir
	private File projectDir
	private String prefix

	private void run(cmd, cwd) {
		cmd = prefix + cmd
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

	private void createProject(id, title) {
		run("cordova create ${buildDir.getPath()} ${id} ${title}", projectDir)
	}

	private void addPlatforms(platforms) {
		platforms.each() {
			run("cordova platform add ${it}", buildDir)
		}
	}

	private void addPlugins(plugins) {
		plugins.each() {
			run("cordova plugin add ${it}", buildDir)
		}
	}

	void apply(Project project) {
		if (SystemUtils.IS_OS_WINDOWS) {
			prefix = windowsPrefix
		} else {
			prefix = unixPrefix
		}
		projectDir = project.projectDir
		buildDir = project.buildDir
		project.extensions.create('cab', CabExtension)

		project.task('clean') << {
			buildDir.deleteDir()
		}

		def createProjectTask = project.task('createProject') << {
			createProject(project.cab.id, project.cab.title)
		}

		def addPlatformsTask = project.task('addPlatforms') << {
			addPlatforms(project.cab.platforms)
		}

		def addPluginsTask = project.task('addPlugins') << {
			addPlugins(project.cab.plugins)
		}

		def copySourceTask = project.task('copySource') << {
			// delete www folder in cordova base project
			def targetWWW = new File(buildDir, 'www')
			def sourceWWW = new File(projectDir, 'www')

			println("Deleting ${targetWWW.getPath()}")
			targetWWW.deleteDir()

			// copy the web app source to the cordova www folder
			println("Copying ${sourceWWW.getPath()}")
			println("into ${targetWWW.getPath()}")
			project.copy {
			    from sourceWWW
     		    into targetWWW
			}
		}

		def buildTask = project.task('build') << {
			//placeholder task	
		}

		buildTask.dependsOn(addPluginsTask)
		addPluginsTask.dependsOn(addPlatformsTask)
		addPlatformsTask.dependsOn(copySourceTask)
		copySourceTask.dependsOn(createProjectTask)
	}
}

class CabExtension {
	String id
	String title
    ArrayList platforms
    ArrayList plugins
}
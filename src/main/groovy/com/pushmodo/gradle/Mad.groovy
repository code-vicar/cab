package com.pushmodo.gradle

import org.gradle.api.*
import org.apache.commons.lang3.SystemUtils

class Mad implements Plugin<Project> {
	private final String windowsPrefix = 'cmd /c '
	private final String unixPrefix = 'sh -c '
	
	private File buildDir
	private File projectDir
	private String prefix

	private void run(cmd, cwd) {
		cmd = prefix + cmd
		println "**: ${cmd}"
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
		project.extensions.create('mad', MadExtension)

		project.task('clean') << {
			buildDir.deleteDir()
		}

		project.task('build') << {
			createProject(project.mad.id, project.mad.title)
			addPlatforms(project.mad.platforms)
			addPlugins(project.mad.plugins)
		}
	}
}

class MadExtension {
	String id
	String title
    ArrayList platforms
    ArrayList plugins
}
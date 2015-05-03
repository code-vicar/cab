package com.pushmodo.gradle

import org.gradle.api.*
import org.apache.commons.lang3.SystemUtils
import groovy.text.SimpleTemplateEngine

class Cab implements Plugin<Project> {

  private String cordovaCmd

	private Project project

  private String[] prepareCmd(cmd) {
    def _cmd = [cordovaCmd]
    _cmd.addAll(cmd)
    return _cmd as String[]
  }

	private void run(cmd, cwd) {
    try {
      def _cmd = prepareCmd(cmd)
      println "shell: ${_cmd.toString()}"
      ProcessBuilder builder = new ProcessBuilder(_cmd)
      builder.directory(cwd)
      builder.redirectErrorStream(true)

      Process process = builder.start()

      InputStream stdout = process.getInputStream ()
      BufferedReader reader = new BufferedReader (new InputStreamReader(stdout))

      def line
      while ((line = reader.readLine ()) != null) {
         println "*: ${line}"
      }

      process.waitFor()
      if(process.exitValue() != 0) {
          throw new GradleException("Error occurred running the command ${cmd}");
      }
    } catch (IOException e) {
      println e
      throw new GradleException("Error occurred running the command ${cmd}");
    }
	}

	private void createProject() {
		def projectDir = project.projectDir
		def buildDir = project.buildDir
		def id = project.cab.id
		def title = project.cab.title
		run(['create', "${buildDir.getPath()}", "${id}", "${title}"], projectDir)
	}

	private void addPlatforms() {
		project.cab.platforms.each() {
			run(['platform', 'add', "${it}"], project.buildDir)
		}
	}

	private void addPlugins() {
		project.cab.plugins.each() {
			run(['plugin', 'add', "${it}"], project.buildDir)
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
    cordovaCmd = project.getRootDir().getPath() + "${File.separator}node_modules${File.separator}cordova${File.separator}bin${File.separator}" + (SystemUtils.IS_OS_WINDOWS ? 'cordova.cmd' : 'cordova');
		this.project = project

		project.extensions.create('cab', CabExtension)

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

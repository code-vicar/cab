# Cordova App Builder (cab)

A gradle plugin for orchestrating the cordova build process

Tasks:
- clean: Deletes previous build
- createProject: Creates the cordova project
- copyConfig: Overwrites cordova config.xml file with templatized version
- copySource: Overwrites cordova project www folder with application contents
- addPlatforms: Add platforms to cordova project
- addPlugins: Add cordova plugins
- build: Builds the cordova app
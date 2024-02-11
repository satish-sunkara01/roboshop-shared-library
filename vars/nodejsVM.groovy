def call(Map configMap){
    pipeline {
        agent {
            node{
                label 'AGENT-1'
            }
        }
        environment { 
            packageVersion = ''
            // Placed Nexus url at pipeline Globals
            // nexusURL = '172.31.5.163:8081'
        }
        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        parameters {
                booleanParam(name: 'Deploy', defaultValue: false, description: 'Run Deploy when it is true')
        }

        

        stages {
            stage('Get the version') {
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        packageVersion = packageJson.version
                        echo "Application Version : $packageVersion"
                    }
                }
            }
            stage('Installing dependecies') {
                steps {
                    sh """
                        npm install
                    """
                }
            }
            stage('Unit testing') {
                steps {
                    sh """
                        echo "This is for Unit test"
                    """
                }
            }
            stage('Sonar Scanner') {
                steps {
                    sh """
                        echo "This must be run but now avoiding for cost optimization"
                        echo "Sonar Scan will run Here"

                    """
                }
            }
            stage('Build') {
                steps {
                    sh """
                        ls -la
                        zip -q -r ${configMap.component}.zip ./* -x ".git/" -x ".zip"
                        ls -ltr
                    """                
                }
            }
            stage('nexus_artifact') {
                steps {
                    nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            protocol: 'http',
                            nexusUrl: pipelineGlobals.nexusURL(),
                            groupId: 'com.roboshop',
                            version: "${packageVersion}",
                            repository: "${configMap.component}",
                            credentialsId: 'nexus-auth',
                            artifacts: [
                                [artifactId: "${configMap.component}",
                                classifier: '',
                                file: "${configMap.component}.zip",
                                type: 'zip']
                            ]
                        )               
                }
            }
            stage('Deploy') {
                when {
                    expression{
                        params.Deploy
                    }
                }
                steps {
                    build job: "../${configMap.component}-deploy", wait : true, parameters: [string(name: 'version', value: "${packageVersion}"),
                    string(name: 'environment', value: 'dev')]               
                }
            }
        }
        post { 
            always { 
                echo 'I will always say Hello again!'
                deleteDir()
            }
            failure { 
                echo 'Script is failed'
            }
            success { 
                echo 'Hey Script is success!'
            }
        }
    }

}

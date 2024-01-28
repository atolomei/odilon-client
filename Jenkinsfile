pipeline  {
    agent any 
	tools {
		jdk 'jdk_11'
	}
	options {
		buildDiscarder(logRotator(daysToKeepStr: '30', artifactNumToKeepStr: '2'))
	}


    stages {
        stage ('Build') {
            steps {
				withMaven(options: [pipelineGraphPublisher(disabled: true)], maven : 'maven_3.6'){
					sh 'mvn clean install -Dmaven.source.skip' 
				}
            }
        }
    }
	post {

        failure {
			script {
					echo "Sending failure email"
                    emailext subject: '$DEFAULT_SUBJECT',
                        body: '$DEFAULT_CONTENT',
                        recipientProviders: [
                            [$class: 'CulpritsRecipientProvider'],
                            [$class: 'DevelopersRecipientProvider'],
                            [$class: 'RequesterRecipientProvider']
                        ], 
                        replyTo: '$DEFAULT_REPLYTO',
                        to: '$DEFAULT_RECIPIENTS'
            }
			echo 'This will run only on failure'
        }
        changed {
			script {
				if (currentBuild.currentResult == 'SUCCESS') {
					echo "Sending back to normal email"
                    emailext subject: '$DEFAULT_SUBJECT',
                        body: '$DEFAULT_CONTENT',
                        recipientProviders: [
                            [$class: 'CulpritsRecipientProvider'],
                            [$class: 'DevelopersRecipientProvider'],
                            [$class: 'RequesterRecipientProvider']
                        ], 
                        replyTo: '$DEFAULT_REPLYTO',
                        to: '$DEFAULT_RECIPIENTS'
				}
			}
        }
    }
}
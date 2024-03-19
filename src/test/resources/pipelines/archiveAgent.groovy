//file:noinspection GrPackage
pipeline {
    agent {
        label('agent')
    }
    stages {
        stage('Archive artifact') {
            steps {
                script {
                    if (isUnix()) {
                        sh "echo 'Hello, World!' > artifact.txt"
                    } else {
                        bat "echo Hello, World! > artifact.txt"
                    }
                }
            }
        }
    }
}

//file:noinspection GrPackage
pipeline {
    agent {
        label('built-in')
    }
    stages {
        stage('Archive artifact') {
            steps {
                script {
                    if (isUnix()) {
                        sh "echo 'Hello, World!' > 'my artifact.txt'"
                    } else {
                        bat "echo Hello, World! > my 'artifact.txt'"
                    }
                }
            }
        }
    }
}

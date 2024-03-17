//file:noinspection GrPackage
pipeline {
    agent {
        label('built-in')
    }
    stages {
        stage('Archive artifact') {
            steps {
                sh "echo 'Hello, World!' > artifact.txt"
                archiveArtifacts artifacts: 'artifact.txt'
            }
        }
    }
}

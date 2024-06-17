//file:noinspection GrPackage
pipeline {
    agent {
        label('agent')
    }
    stages {
        stage('Archive artifact') {
            steps {
                writeFile file: 'artifact.txt', text: 'Hello, World!'
                archiveArtifacts artifacts: '*.txt', allowEmptyArchive: false
            }
        }
    }
}

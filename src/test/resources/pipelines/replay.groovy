//file:noinspection GrPackage
pipeline {
    agent none
    options {
        preserveStashes()
    }
    stages {
        stage('Agent 1') {
            agent {
                label 'built-in'
            }
            steps {
                dir('target') {
                    writeFile file: 'foo.txt', text: 'Hello'
                }
                stash(name: 'stash', includes: 'target/**')
            }
        }
        stage('Result') {
            agent {
                label 'built-in'
            }
            steps {
                unstash(name: 'stash')
            }
        }
    }
}

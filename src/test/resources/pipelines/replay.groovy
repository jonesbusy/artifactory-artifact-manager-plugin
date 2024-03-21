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
            script {
              if (isUnix()) {
                sh 'mkdir target && echo "Hello" > target/foo.txt'
              } else {
                bat 'mkdir target && echo "Hello" > target/foo.txt'
              }
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
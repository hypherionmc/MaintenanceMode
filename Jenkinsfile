pipeline {
    agent {
        label "master"
    }
    tools {
        jdk "JAVA16"
    }
    stages {
        stage("Notify Discord") {
            steps {
                discordSend webhookURL: env.FDD_WH_ADMIN,
                        title: "Deploy Started: Maintenance Mode 1.17.x Deploy #${BUILD_NUMBER}",
                        link: env.BUILD_URL,
                        result: 'SUCCESS',
                        description: "Build: [${BUILD_NUMBER}](${env.BUILD_URL})"
            }
        }
        stage("Prepare & Publish") {
            steps {
                sh "wget -O changelog-forge.md https://raw.githubusercontent.com/hypherionmc/changelogs/main/mmode/changelog-forge.md"
                sh "wget -O changelog-fabric.md https://raw.githubusercontent.com/hypherionmc/changelogs/main/mmode/changelog-fabric.md"
                sh "chmod +x ./gradlew"
                sh "./gradlew clean"
                sh "./gradlew modrinth curseforge -Prelease=true"
            }
        }
    }
    post {
        always {
            sh "./gradlew --stop"
            deleteDir()

            discordSend webhookURL: env.FDD_WH_ADMIN,
                    title: "Maintenance Mode 1.17.x Deploy #${BUILD_NUMBER}",
                    link: env.BUILD_URL,
                    result: currentBuild.currentResult,
                    description: "Build: [${BUILD_NUMBER}](${env.BUILD_URL})\nStatus: ${currentBuild.currentResult}"
        }
    }
}

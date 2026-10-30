pipeline {
    agent any

    // GitHub webhook (needs the GitHub plugin + a public Jenkins URL, e.g. via ngrok).
    // Poll SCM every 2 minutes is the fallback if the webhook can't reach Jenkins.
    triggers {
        githubPush()
        pollSCM('H/2 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()   // two deploys at once would fight over the same containers
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        // Jenkins credential of type "Username with password", ID = dockerhub-creds.
        // This exposes DOCKERHUB_CREDS_USR and DOCKERHUB_CREDS_PSW (masked in logs).
        DOCKERHUB_CREDS = credentials('dockerhub-creds')
        DOCKERHUB_USER  = "${DOCKERHUB_CREDS_USR}"
        IMAGE_NAME      = "${DOCKERHUB_CREDS_USR}/calculator"
        IMAGE_TAG       = "${BUILD_NUMBER}"
        HOST_PORT       = '8090'
        // The default "java" on this machine is a Java 21 JRE (no javac), so point Maven (which honours JAVA_HOME) at the full JDK 17
        JAVA_HOME       = '/usr/lib/jvm/java-17-openjdk-amd64'
        NOTIFY_EMAIL    = 'pandyashivam76@gmail.com'   // TODO: change to your email
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .
                    docker image ls ${IMAGE_NAME}
                '''
            }
        }

        stage('Push') {
            steps {
                sh '''
                    echo "$DOCKERHUB_CREDS_PSW" | docker login -u "$DOCKERHUB_CREDS_USR" --password-stdin
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest
                '''
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // "up -d" only recreates containers whose image/config changed,
                // so re-running the pipeline is safe (idempotent).
                sh '''
                    docker compose up -d --remove-orphans
                    docker compose ps
                '''
                // Verify through the Nginx proxy: wait up to ~2 minutes for UP
                sh '''
                    for i in $(seq 1 24); do
                        if curl -fs http://localhost:${HOST_PORT}/actuator/health | grep -q '"status":"UP"'; then
                            echo "Health check passed"
                            curl -s "http://localhost:${HOST_PORT}/api/v1/add?a=2&b=3"; echo
                            exit 0
                        fi
                        echo "Waiting for app to become healthy ($i/24)..."
                        sleep 5
                    done
                    echo "Deployment did not become healthy"
                    docker compose logs --tail=50
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "SUCCESS: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """<p>Build <b>#${BUILD_NUMBER}</b> of <b>${JOB_NAME}</b> succeeded.</p>
                         <p>Image: ${IMAGE_NAME}:${IMAGE_TAG}<br/>
                         App: http://localhost:${HOST_PORT}/api/v1/add?a=2&amp;b=3</p>
                         <p><a href="${BUILD_URL}">Open build</a></p>""",
                mimeType: 'text/html'
            )
        }
        failure {
            emailext(
                to: "${NOTIFY_EMAIL}",
                subject: "FAILURE: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """<p>Build <b>#${BUILD_NUMBER}</b> of <b>${JOB_NAME}</b> failed.</p>
                         <p><a href="${BUILD_URL}console">Console output</a></p>""",
                mimeType: 'text/html',
                attachLog: true
            )
        }
        always {
            // Drop dangling layers from earlier builds so the disk doesn't fill up
            sh 'docker image prune -f || true'
        }
    }
}

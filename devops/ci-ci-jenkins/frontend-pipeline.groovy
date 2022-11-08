podTemplate(cloud: 'kubernetes', containers: [containerTemplate(args: 'cat', command: '/bin/sh -c', image: 'docker', livenessProbe: containerLivenessProbe(execArgs: '', failureThreshold: 0, initialDelaySeconds: 0, periodSeconds: 0, successThreshold: 0, timeoutSeconds: 0), name: 'docker-container', resourceLimitCpu: '', resourceLimitEphemeralStorage: '', resourceLimitMemory: '', resourceRequestCpu: '', resourceRequestEphemeralStorage: '', resourceRequestMemory: '', ttyEnabled: true, workingDir: '/home/jenkins/agent'),
                                             containerTemplate(args: 'cat', command: '/bin/sh -c', image: 'lachlanevenson/k8s-helm:latest', name: 'helm-container', ttyEnabled: true)
], label: 'questcode', name: 'docker', namespace: 'devops', volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]) {
  node('questcode'){
    def REPOS
    def IMAGE_NAME = "frontend"
    def ENVIRONMENT = "staging"
    def IMAGE_VERSION = "alpha-staging"
    def GIT_REPOS_URL = "git@github.com:alefligiero/frontend-questcode.git"
    def CHARTMUSEUM_URL = "http://helm-chartmuseum:8080"

    stage('Checkout') {
      echo 'Iniciando Clone do repositório'
      REPOS = git branch: 'main', credentialsId: 'github', url: GIT_REPOS_URL
    }
    stage('Package') {
      container('docker-container') {
        echo 'Iniciando empacotamento com Docker'
        withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USER')]) {
            sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
            sh "docker build -t ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION} . --build-arg NPM_ENV='${ENVIRONMENT}'"
            sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION}"
        }
      }
    }
    stage('Deploy') {
        container('helm-container') {
            echo 'Iniciando Deploy com Helm'
            sh 'ls -ltra'
            sh 'helm repo add bitnami https://charts.bitnami.com/bitnami'
            sh "helm repo add questcode ${CHARTMUSEUM_URL}"
            sh 'helm repo update'
        }
    }
  }
}
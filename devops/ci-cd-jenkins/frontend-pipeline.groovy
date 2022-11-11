podTemplate(cloud: 'kubernetes', 
    containers: [
        containerTemplate(args: 'cat', command: '/bin/sh -c', image: 'docker', name: 'docker-container', ttyEnabled: true),
        containerTemplate(args: 'cat', command: '/bin/sh -c', image: 'lachlanevenson/k8s-helm:latest', name: 'helm-container', ttyEnabled: true)
        ], 
    label: 'questcode', 
    namespace: 'devops', 
    volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]
) 
{
    node ('questcode') {
        def REPOS
        def IMAGE_NAME = "questcode-frontend"
        def ENVIRONMENT = "staging"
        def IMAGE_VERSION = "staging"
        def GIT_REPOS_URL = "git@github.com:alefligiero/frontend-questcode.git"
        def CHARTMUSEUM_URL = "http://my-chartmuseum:8080"
        
        stage('Checkout') {
            echo 'Iniciando clone do repositório'
            REPOS = git credentialsId: 'github', url: GIT_REPOS_URL
            IMAGE_VERSION = sh returnStdout: true, script: 'sh read-package-version.sh'
            IMAGE_VERSION = IMAGE_VERSION.trim()
        }
        stage('Package') {
            container('docker-container') {
                echo 'Iniciando empacotamento com o Docker'
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USER')]) {
                    sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
                    sh "docker build -t ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION} . --build-arg NPM_ENV='${ENVIRONMENT}'"
                    sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION}"
                }
            }
            
        }
        stage('Deploy') {
            container('helm-container'){
                echo 'Iniciando Deploy com Helm'
                sh 'ls -ltra'
                sh "helm repo add questcode ${CHARTMUSEUM_URL}"
                sh 'helm search repo questcode'
                sh 'helm repo update'
                sh "helm upgrade staging-frontend questcode/frontend --set image.tag=${IMAGE_VERSION} -n staging"
            }
            
        }
    }
}

# keycloak-javalin-sample
A basic sample of keyCloak integration into javalin webapp.

This is a sample application for learning purpose only.

![Sample App Flow](https://github.com/struggzard/keycloak-javalin-sample/blob/main/res/AuthFlow-sample_app_flow.png "sample app flow")

## Build Application
Maven is used as a build tool thus application can be build with following command:
```
mvn clean install
```

## Run Applications

### Web App
Sample web application should be run as a standalone (fat jar configured) or 
from IDEA. Keep in mind that keycloak properties like realm, client id/secret are 
hardcoded and need to be changed by hand in App code.

Application URL: http://localhost:7000

### KeyCloak

KeyCloak environment with persistent volume configured at docker compose:
```
docker-compose up
```
KeyCloak server can be accessed with URL http://localhost:7001


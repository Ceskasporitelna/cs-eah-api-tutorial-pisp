Tutorial - how to connect to CSAS PISP API
=====================================
Java skeleton web-based application for connection to CSAS.
For more information visit: https://developers.erstegroup.com/docs/guides/csas-tutorials-initiate-payment

This is a Java 8, spring-boot project. To run the app execute:

`mvn spring-boot:run`

## Testing the skeleton app
Import insomnia_project_2019-02-08.json to your Insomnia and call prepared requests.
The controller will serve your request and call bank sandbox API.

## Settings 
Basic settings are in application.properties file. By default, CSAS sandbox environment is preset. 
- To change it to your app, 
    - change "your app" section
    - change URLs in "endpoints" section
- To set proxy, change "proxy" section (leave empty for no proxy)

## Other
- The paging and sorting in responses doesn't work in sandbox environment.
- For simplicity the /auth call is set not to follow redirection, but accepts 
whole 302 response instead and uses the code value

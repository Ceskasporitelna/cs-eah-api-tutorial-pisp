Tutorial - how to connect to Csas API
=====================================
Java skeleton web-based application for connection to Csas.
For more information visit: URL

This is a Java 8, spring-boot project. To run the app execute:

`mvn spring-boot:run`

## Testing the skeleton app
Import insomnia_project_2019-02-08.json to your insomnia and call prepared requests.
The controller will serve your request and call bank sandbox API.

## Settings 
Basic settings is in application.properties file. By default, csas sandbox is preset. 
- To change it to your app, 
    - change "your app" section
    - change urls in "endpoints" section
- To set proxy, change "proxy" section (leave empty for no proxy)

## Other
- The paging and sorting in responses does't work in sandbox environment.
- For simplicity the /auth call is set not to follow redirection, but accepts 
whole 302 response instead and uses the code value
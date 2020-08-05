### Intention
The service tends to be a mock web server which is able to test the pipelines of requests 
or emits the particular messages to the tested server. The interaction is performed by json-packages

### The typical case can be depicted as follows:
#### Pipeline:
There is a pipeline which needs to send some information to the server 
then in accordance with the server response process some logic and send the information back to the server. 

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/besok/test-server-flow/master/doc/example1.plantuml)




The server needs to receive a certain amount of events thereby we need to somehow send the group of messages regularly or one-time.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/besok/test-server-flow/master/doc/example2.plantuml)

### Overall functions
 - an ability to generate JSON file in accordance with a given structure consider the particular generation functions
 - an ability to receive a request from the test server and response on that accordingly
 - an ability to send a request to the test server in accordance with the particular trigger

### Json generator

### Endpoints and Parcels

#### Examples
- send generated jsons in parallel to the endpoint
- send a json, get a response, wait, send a new one or to another endpoint
- send a json, if the resp is 200 then do one thing if the resp is another do another thing.
- send a json, get a response, send a json with the information from the response.
...  
 
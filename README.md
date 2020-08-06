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
The server provides the ability to generate json files in accordance with given functions.

Example:
```json
{
  ">|sequence": "seq(1)",
  ">|string": "str(10)",
  ">|number": "num(1,100)",
  ">|str_file": "str_file(\\home\\user)",
  ">|num_file": "num_file(\\home\\user,;)",
  ">|string_from_list": "str_list(a,b,c,\"s\",and \\)",
  ">|num_from_list": "num_list(1,2,3,1.3,3e-1)",
  ">|uuid" :"uuid() => [var_name]",
  ">|date": "time(Y-m-d H:M:S) => [var_name]",
  ">|global_var": "from_ctx(var_name)",
  ">|array": "array(100,num(1,100))"
}
```

#### Prefix
To distinguish the field which carries the function to generate the field name comprases from the prefix char('>' by default) and '|' and field name,
for instance the field with name "id" and default separator will be ">|id" and after generation the field name will remain only last part namely "id".

#### Functions
| Generator | Arguments | Description | Example |
|----------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| seq | starting point | the sequentially-increase row of numbers (1,2,3,4 ...) | seq(10)  |
| str | size of row | the row composed of random letters and numbers, predefined length | str(10) |
| num | low bound and high bound | the random number lying in predefined bounds | num(1,100) |
| str_file | path to file | the list of string pulled off the predefined file note: delimiter can be omitted and the default delimiter(,) will be used  | str_file(\home\user\json) str_file(\home\user\json, \n) |
| num_file | path to file | the list of numbers pulled off the predefined file note: delimiter can be omitted and the default delimiter(,) will be used  | num_file(\home\user\json) num_file(\home\user\json, \n) |
| str_list | list of values | the list of string | random_str_from_list(a,b,c,d) |
| num_list | list of values | list of numbers | random_int_from_list(1,2,3,4,5) |
| uuid |  | generated uuid  | uuid() |
| time | format | the current date and time. By default can be ommited  and '%Y-%m-%d %H:%M:%S' will be used | currnet_date_time(%Y-%m-%d) |
| array | number of elements, generator for elements | the generator to get the array filled. | array(10,random_int(1,10)) |
| from_ctx | variable name | the variable from context| from_ctx(_endpoints.endpoint2.input.url.id) |
 
Though the generated field will turn up in the output, the possibility exists to save the value into the context to retrieve it afterwards in the other json
To do that needs to add "=> [var_name]" suffix like that: 
```json
{">|id": "num(1,100) => [entity1.var1]"}
```
and then it can be retrieved as follows:
```json
{">|id": "from_ctx(entity1.var1)"}
```
*note  : if the variable does not exist the null will be return ```json {"id":null}```*

Thus the following json template
 ```json
{
  "customer": {
    ">|id": "seq(1)",
    ">|name": "str(10)",
    ">|age": "num(10,70)",
    "address": {
      ">|street": "str_list(Albany, CastleRock, Lenina, Church)",
      ">|geo": "uuid() => [geo.address]"
    },
    ">|coming_date": "time() => [customer.coming_date]",
    "constant_flag": "flag"
  }
}
```
will be generated into:
```json
{
  "customer": {
       "id": 1,
       "name": "dZJi2iA92d",
       "age": 69,
       "constant_flag": "flag",
       "coming_date": "8/5/20, 9:50 PM",
       "address": {
           "street": "Albany",
           "geo": "820dae67-2753-44b6-a9b8-16b20fca17d8"
    }
  }
}
``` 
 
 
### Endpoints
The server is able to receive requests from the test server and response to the particular answer.
For working with endpoints It requires to provide a configuration file.

Example:
```yaml
---
endpoint:
  name: endpoint1
  input:
    method: post
    url: /endpoint
  output:
    code: 200
    body: file:path\to\file\json_generator_template.json
    prefix: ">"
---
endpoint:
  name: endpoint2
  input:
    method: get
    url: /endpoint/{id}/{param}
  output:
    code: 200
    body: '{
        ">|field1":" from_ctx(geo.address)",
        ">|field2":" from_ctx(_endpoints.endpoint2.input.url.id)",
        ">|field3":" from_ctx(_endpoints.endpoint1.input.body.id)",
        ">|field4":" from_ctx(customer.coming_date)"
     }'
```
#### Structure
The file represents the list of the endpoints separated by '---'
The fields are defined inside the entity as follows:
- name : the name of the endpoint. Must be unique
- input section describes the mathing parameters
   - method: get,post,delete,put
   - url: the url for request. Also, it is supposed to have params which can be retrieved from the request and used afterwards. 
        For that they can be framed by {} like ```url: /endpoint/{onetwo} ```. Thereafter they can be invoked by function "from_ctx(_endpoints.name.input.urk.onetwo)"
- output section describes the response of the server
    - code : http code
    - body: JSON response. The response can be taken as from file by adding prefix 'file:' or from string ''. It can be a generator or genuine JSON
    - prefix: the symbol which indicates that the field carries a generator function. The prefix compounds with '|' 
    and with field name that eventually brings to the full name like that ">|id". After generation, the prefix with '|' omitted.       

### Parcels
The server can act as an active source of messages by sending parcels which can be configured analogously endpoints

Example:
```yaml
---
parcel:
  name: parcel1
  receiver:
    method: post
    url: 'http://localhost:9000/api/parcel1'
  message:
    body: file:C:\projects\test-server-flow\src\test\resources\jsons\simple_with_gen.json
    prefix: ">"
  trigger: endpoint(endpoint2)
---
parcel:
  name: parcel2
  receiver:
    method: get
    url: 'http://localhost:9000/api/parcel2/{_endpoints.endpoint2.input.url.id}/send'
  trigger: parcel(parcel1)
---
parcel:
  name: parcel3
  receiver:
    method: post
    url: 'http://localhost:9000/api/parcel3/send'
  message:
    body: '{">|id":"seq(10)"}'
  trigger: times(2,1,1)
```
    
#### Structure
The file represents the list of the endpoints separated by '---'
The fields are defined inside the entity as follows:
- name : the name of the parcel. Must be unique
- receiver: describes the endpoint which will receive the parcel
    - method: post,get,delete,put
    - url: url to receive. Url can be parametrised by framed {} like ```url: 'http://localhost:9000/api/parcel2/{_endpoints.endpoint2.input.url.id}/send'```
- message: describes the body to send. Can be wholly omitted.
    - body: JSON response. The response can be taken as from file by adding prefix 'file:' or from string ''. It can be a generator or genuine 
    - prefix: the symbol which indicates that the field carries a generator function. The prefix compounds with '|' 
        and with field name that eventually brings to the full name like that ">|id". After generation, the prefix with '|' omitted. 
- trigger: a trigger depicting the condition when the parcel should be sent.
    - endpoint(endpoint name) : when the request comes to the specific endpoint
    - parcel(parcel name): when the specific parcel has been sent
    - times(number of times, the gap between parcels in sec, initial delay in sec): the request which periodically repeated.
    

### How to run:
The console application expects to get at least the configuration file composes from endpoint and parcel configurations and port number. 
If the port is not set the default value will be used namely 9090

Example:
```cmd
server.jar c=C:\config\endpoints_parcels.yml -p=9999
```           
    
        
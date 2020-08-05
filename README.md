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

### Endpoints and Parcels

#### Examples
- send generated jsons in parallel to the endpoint
- send a json, get a response, wait, send a new one or to another endpoint
- send a json, if the resp is 200 then do one thing if the resp is another do another thing.
- send a json, get a response, send a json with the information from the response.
...  
 
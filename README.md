The server to test web.

#### Examples
- send generated jsons in parallel to the endpoint
- send a json, get a response, wait, send a new one or to another endpoint
- send a json, if the resp is 200 then do one thing if the resp is another do another thing.
- send a json, get a response, send a json with the information from the response.
...  
   
#### Modules
- json-parser
- curl / web module (sending the information)
- json-generator(mechanism)
- parameterization (when you get infromation from the previous request and inject it to the current one.) 
- flow dsl
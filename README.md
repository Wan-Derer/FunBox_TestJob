# FunBox_TestJob
TestJob for FunBox company

## Instructions:
- start Redis server on localhost with standard port 6379 (Redis host defined in REDIS_HOST constant, 
  Redis port - in REDIS_PORT constant);
- start the application, the WEB-server will be started at http://localhost:8080/ (HTTP port defined in HTTP_PORT constant);
- send POST query as required in the statement, for example:
  
    _POST /visited_links_
  
  _{_
  
  _"links": [_
  
  _"https://ya.ru",_
  
  _"https://ya.ru?q=123",_
  
  _"funbox.ru",_
  
  _"https://stackoverflow.com/questions/11828270/how-to-exit-the-vim-editor"_
  
  _]_
  
  and check returned status;
- using Redis console find two structures created:
    - ZRANGE funbox 0 -1 returns members of sorted set containing one record. Member name is UNIX time 
      in milliseconds;
    - LRANGE member_name 0 -1 returns links your added (use member name returned at previous step). 
      Incorrect links will not be added;
    - each POST was add a record to "funbox" set and new list with links.
- send GET query as required in the statement, for example: _GET /visited_domains?
  from=start_time&to=end_time_ wherer _start_time_ and _end_time_ are UNIX time in milliseconds and check returned Json with set of links and status.
  

## Technologies:
- Java;
- REST (Java without any framework);
- Redis as database engine (using Jedis client);
- JSON for data transfer (using Jackson library)
- Maven for project management.

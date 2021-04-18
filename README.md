# Scalajs CQRS Prototype

A project to demonstrate CQRS and event sourcing.  

It has two aggregates:
- otp: one-time-password domain dealing with issuance and verification
- org: company profile domain with name, location, etc


## Command
### Gateway: from Lambda handler to command
Command is sent from client with api call. The first step is to obtain the right command from the api method and request. 
Any data validation (string length, max value, etc) is done here through the use of Value Object constructor.

### Domain: process the command, reject or accept it as a new event
This is the main command in CQRS. Basically we load an aggregate stream from event store and replay it. 
Replaying events however is very difficult and dirty to do with imperative language like Java or Net as we have to work with states.

That's why Scala is a very good language for CQRS. Because in Scala replay is basically just a **fold left** operation. After 
replaying them we finally attempt to apply the new command to it. If it satisfies all the invariants then we turn it into a new event. If not we reject it. 

#### Notes
It's possible that we don't need to load the entire stream. If the command only depends on the latest state then we can just load the latest event. 
For example updating a profile. Here the invariant is simple: we just need to know if there's an old event to see that we don't update a profile that isn't created yet. 

### Event transaction: save and dispatch the event
To avoid collision with concurrent command-handling process, a newly created event is persisted in the event store with optimistic lock strategy: if the version of this event already exists then it is rejected.
Also persisting and publishing it to message broker has to be done in one atomic operation. In this project we enforce this transaction by compensating error e.g
if the publishing part fails we delete the event from the event store. Of course this assumes that the database is a more reliable service than the message broker.

#### Notes
With DynamoDb it's possible to use DynamoDb Stream to automatically send out new events. 
However, these are AWS events, so it's necessary to parse them back to CQRS events. In this project we don't 
use Stream as the parser would be a critical point of failure: if it's down then we would lose the stream unless we use a more complicated caching technique. 

## Query
#### Saving projection
This is where we can have freedom to choose any database. If command is set correctly then we will only need to persist **materialized view** or data that matches what user wants to see without any join. 

#### Serve query from client
Client will request the data with GET. When dealing with eventual consistency, client will likely do polling. 
In that case the response should be checked against the version the client already has.  


## AWS 

### What are AWS services needed ?
|name|purpose|
|---|---|
|Lambda|main program|
|Dynamodb|event store and read db|
|SQS FIFO|bridge between SNS and consuming Lambda|
|SNS FIFO|message broker|

### Is FIFO type in SNS and SQS necessary ?
Yes for any events emitted from command / event handler. With normal SNS, AWS cannot 100% guarantee that the order is preserved.
This means there could be a possibility that event with a wrong version arrives at the query side.

### How many lambdas do I need ? 
1

One single function includes

|name|purpose|source|sink|
|-----|-----|---|---|
|Command handler|turns client request to event|HTTP events (APIProxyGatewayEvent)|HTTP response|
|Event handler|handles other events, turns them to new events (optional) |SQS events|none|
|Projection|projecting latest state to read db|SQS events|none|
|Query handler|returns client's query from read db|HTTP events|HTTP response|


### Why Scala.js ?
Scala.js compiles to Javascript, so we can avoid JVM cold start in Lambda.  

## To run
- Set up necessary AWS services mentioned 
- Set environment variables as demanded by Config
- Run `sbt` from terminal then `universal:packageBin` to zip it.
- Upload it to Lambda


Very useful links:
- https://blog.leifbattermann.de/2017/04/21/12-things-you-should-know-about-event-sourcing/
- https://medium.com/the-theam-journey/benchmarking-aws-lambda-runtimes-in-2019-part-i-b1ee459a293d





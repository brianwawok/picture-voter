#Picture voting application:

## Overview
This is a simple picture voting application. The users can interact in the following ways.

* A user can send in a message via the /event api with a new picture. That message is saved to a shared dropbox folder that anyone can view.
* A user can send in the name of the picture they like via the /event api, and that picture gets a 'vote' (only 1 vote per person per picture!)
* A user can see a list of all votes via the /report api, to see how popular each picture was.


It was mostly an exercise to try learning how Akka HTTP works.

## Brief API overview

Here is an overview of the simple api:

### API Methods

**Posting a new picture** 

Posting a new picture is done via a post to `/event`

This is the API you post to in order to add a new picture or cast a vote. Adding a new picture should be JSON in the format
```
{ "type": "inboundMedia", "payload": "http:/picture-url.jpg", "fromNumber": "+15551234567", "toNumber": "+15551234567" }
```

Where payload has the url of the image you want to save to dropbox, fromNumber is the phone number (leading +) of the vote caster,
and toNumber is the phone number (leading+) of the server running the voting contest.

**Voting for a picture**


To vote for a new picture, you also post to `/event`

This is adding your vote to an existing picture in the shared dropbox folder. It should be JSON in the format

```
{ "type": "inboundText", "payload": "picture-name.jpg", "fromNumber": "+15551234567", "toNumber": "+15551234567" }

```

Note that the picture name should match up with only the last part of the picture above. So for example if 
a new picture is added via *http://my/path/cat.jpg*, you would just use *cat.jpg* to vote.

**Running a report**

To view a report of votes per picture, post to `/event` 

You need to pass in 1 parameter, which is the *toNumber* used in the voting contest. For example:
```
+15551234567
```


## Running the application

### Requirements

To compile and run the application, you need to have a *Java JDK 8* and *SBT 0.13.9* installed and available at the command line.

Java JDK 8 can be found at
> http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

SBT 0.13.9 can be found at:
> http://www.scala-sbt.org/download.html

### Running in test mode
This is a normal SBT application. To fire up an embedded in-memory database and run all tests, you simply need to execute
`sbt test`
from the root directory. When the tests are over, the in memory database will go away. The tests walk through a basic
workflow of adding some pictures and voting for them. At this time, no DropBox functionality is exercised in the test
suite.

### Running in prod mode
The project uses the sbt assembly plugin. The target jar can be created with:
`sbt clean assembly`

The default configurations can be used for all properties except for DropBox token. The DropBox token
must be specified at server launch time.

Then to run the in prod made, you can execute

```
java -jar -Ddropbox.token="xxxxxxxxxxx" ./target/scala-2.11/PictureVoterService.jar"
```

Where *xxxxxxxxxxx* is your DropBox API token token. For information about your DropBox token, see

> https://blogs.dropbox.com/developers/2014/05/generate-an-access-token-for-your-own-account/




## Making manual requests via curl

An example of requests you can send via commandline to the application are:

Add a new image:

```
curl -X POST -H "Content-Type: application/json" --data '{ "type": "inboundMedia", 
"payload": "http://cdn.playbuzz.com/cdn/0079c830-3406-4c05-a5c1-bc43e8f01479/7dd84d70-768b-492b-88f7-a6c70f2db2e9.jpg", 
"fromNumber": "+15551234567", "toNumber": "+15551234567" }' http://localhost:8080/event
```


Vote on an existing image:

```
curl -X POST -H "Content-Type: application/json" -d '{ "type": "inboundText", 
"payload": "7dd84d70-768b-492b-88f7-a6c70f2db2e9.jpg", "fromNumber": "+15551234567", "toNumber": "+15551234567" }
 http://localhost:8080/event ```
```


View a report of votes to pictures:

```
curl -X GET -H "Content-Type: application/json" -d '"+15551234567"' http://localhost:8080/report
```


## Configuration override

Other interesting command line parameters you want to override, and their default values:

Config Key | Default Value
--------------|----------------
http.interface | localhost
http.port | 8080
dropbox.host | https://api.dropboxapi.com
dropbox.port | 443
dropbox.folder | pictures
h2-disk-config.url | jdbc:h2:~/data/picture_voter
h2-disk-config.driver | org.h2.Driver
h2-disk-config.connectionPool | disabled
h2-disk-config.keepAliveConnection | true


By default, a new H2 database will be initialized in the folder `~/data/picture_voter`,
 which translates to `/home/you/data` in Linux/OSX or `C:/Users/you/data` in Windows



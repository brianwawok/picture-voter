#Picture voting application:

## Overview
This is a simple picture voting application. The users can interact in the following ways.

* A user can send in a message via the /event api with a new picture. That message is saved to a shared dropbox folder that anyone can view.
* A user can send in the name of the picture they like via the /event api, and that picture gets a 'vote' (only 1 vote per person per picture!)
* A user can see a list of all votes via the /report api, to see how popular each picture was.


It was mostly an exercise to try learning how Akka HTTP works.

## Running in test mode
This is a normal SBT application. To fire up an embedded in-memory database and run all tests, you simply need to execute
`sbt test`
from the root directory. When the tests are over, the in memory database will go away.

## Running in prod mode
The project uses the sbt assembly plugin. The target jar can br created with:
`sbt clean assembly`

Then to run the in prod made, you can execute
`java -jar -Ddropbox.token="xxxxxxxxxxx" ./target/scala-2.11/PictureVoterService.jar"`

Where XXXXXXXXXX is your dropbox token.  

Other interesting command line parameters you want to override, and their default values:

Parameter Key | Default Value
--------------|----------------
http.interface | "localhost"
http.port | 8080
dropbox.host | "https://api.dropboxapi.com"
dropbox.port | 443
dropbox.folder | "pictures"



An example of requests you can send via commandline to the application are:

Add a new image:
`curl -X POST -H "Content-Type: application/json" --data '{ "type": "inboundMedia", "payload": "http://cdn.playbuzz.com/cdn/0079c830-3406-4c05-a5c1-bc43e8f01479/7dd84d70-768b-492b-88f7-a6c70f2db2e9.jpg", "fromNumber": "+15551234567", "toNumber": "+15551234567" }' http://localhost:8080/event `

Vote on an existing image
`curl -X POST -H "Content-Type: application/json" -d '{ "type": "inboundText", "payload": "7dd84d70-768b-492b-88f7-a6c70f2db2e9.jpg", "fromNumber": "+15551234567", "toNumber": "+15551234567" }' http://localhost:8080/event `

View a report of votes to pictures:
`curl -X GET -H "Content-Type: application/json" -d '"+15551234567"' http://localhost:8080/report `

# Meu Simtron
Forwards incoming SMSs to Slack

## How to use
Create `res/values/slack.xml` and include the following values:
 * token: your Slack bot token
 * channel: the Slack channel ID to write
 
### Requirements
 * Android 7 or above
 
## TO DO
 * Add sim view, store it preferences
 * Test slack rtm
 * lost sim data (poll)

## Bugs
 * Ramdon '?' messages
 * SMSs cropped
 * Slack websocket dies

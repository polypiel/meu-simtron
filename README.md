# Meu Simtron
Forwards incoming SMSs to Slack

## How to use
Create `res/values/slack.xml` and include the following values:
 * token: your Slack bot token
 * channel: the Slack channel ID to write
 
### Requirements
 * Android 7 or above
 * Set off any battery-saving preference related with the app
 
## Changelog
## 1.0.4
 * Slack format messages changes to match Simtron v2
 * Bugfixes

### 1.0.3
 * Bugfix: ICCs with check sum
 * Bugfix: do not give status of unknown SIMs

### 1.0.2
 * Added permanent notification to keep the app alive
 * Improvement: ping-pong protocol in Slack

## TO DO
 * Add sim view, store it in preferences
 * Test slack rtm

## Bugs
 * Random '?' messages
 * SMSs cropped
 * Slack websocket dies

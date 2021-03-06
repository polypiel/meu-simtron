# Meu Simtron
Forwards incoming SMSs to Slack

## How to use
Create `res/values/slack.xml` and include the following values:
 * token: your Slack bot token
 * channel: the Slack channel ID to send texts
 * debugChannel: a debug Slack channel ID
 
### Requirements
 * Android 7 or above
 * Set off any battery-saving preference related with the app
 
## Changelog
### 1.1.0
 * Visual improvements
 * Edit sim info
 * Better app notification
 * Better status keep-alive
 * Fix rounded icons (Android 8+)

### 1.0.5
 * Kotlin 1.3
 * Better Slack RTM handling
 * Better notification messages
 * Debug messages
 * Updated sims dictionary

### 1.0.4
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
 * Slack web-socket dies

## Known Issues
 * It crashes on Android 9

## FAQ
### How I send texts in the simulator
```
cat /home/acalvo/.emulator_console_auth_token; echo
telnet
o localhost 5554
auth XXXXXXXXXX
sms send 0123456 "Your Message"
```
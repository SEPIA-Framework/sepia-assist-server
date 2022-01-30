# SEPIA Client Views

SEPIA views aka "frames" are custom mini-applications inside the SEPIA client.  
Custom views have many features and interfaces to help you build your own, specialized voice widget that can be triggered via custom commands or SDK services.  
  
For more views check-out: https://github.com/SEPIA-Framework/sepia-extensions/tree/master/client-views  
  
Download/update example (Linux):
```
cd ~/SEPIA/sepia-assist-server/Xtensions/WebContent/views
wget -N https://raw.githubusercontent.com/SEPIA-Framework/sepia-extensions/master/client-views/clock.html
```
  
Open a custom view:
- Define a "control custom frame" (frame_control) command via Teach-UI
- Choose action `<on>` or `<show>`
- As URL use `<assist_server>/views/path/to/my-view.html` (e.g. `<assist_server>/views/clock.html`)
  
More info: https://github.com/SEPIA-Framework/sepia-docs/wiki/Creating-HTML-voice-widgets-for-the-SEPIA-client

## Backup

NOTE: Put your own views in the 'custom' folder to ensure they are properly included in a SEPIA backup.

# Directory Listing

The SEPIA server can index files hosted by it's own web-server via the '/web-content-index/*' endpoint. The URL path depends on your folder structure.
For example:
* Your file is located at `[SEPIA]/sepia-assist-server/Xtensions/WebContent/shared/pictures/1.jpg` 
* The direct link (using localhost in this example) will be `http://localhost:20721/shared/pictures/1.jpg`
* Using the index endpoint you can access all pictures via `http://localhost/web-content-index/shared/pictures`

Certain folders can be hidden from the index endpoint if you include the file 'no-index'. For example this folder will not be indexed by the '/web-content-index/*' endpoint.  
NOTE: this does not apply to sub-folders by default!  
   
The 'web-content-index' endpoint can be activated/deactivated via the server settings property 'allow_file_index'.

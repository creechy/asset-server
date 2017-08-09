## Asset Server ##

This Asset Server is a simple, self-contained HTTP server that provides the ability to serve a desired set of files for download. 

It provides an file listing page with the ability to click files to download.

Currently only plain HTTP is supported, but you can optionally configure simple authentication for a weak level of protection against assets.

### Usage ###

`java -jar asset-server.jar [--port <port-number>] [--creds <username>|<filename>] <file(s)>`

#### Options ####

`--port <port-number>`

Set the port number (default: 8080)

`--creds <username>`
 `--creds <filename>`

Set a username and password to authenticate with. If the argument given is an existing file, then the username & password will be read from that file, in the following format

```
username:<username>
password:<password>
```
 
If the argument is not a file, then it will be used as the username, and the password will be prompted for on start up. 
 


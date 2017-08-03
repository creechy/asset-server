## Asset Server ##

This Asset Server is a simple, self-contained HTTP server that provides the ability to serve a desired set of files for download. 

It provides an file listing page with the ability to click files to download.

Currently only plain HTTP is supported, but you can optionally configure simple authentication for a weak level of protection against assets.

### Usage ###

`java -jar asset-server.jar [--port <port-number>] [--creds <user> <password>] <file(s)>`

